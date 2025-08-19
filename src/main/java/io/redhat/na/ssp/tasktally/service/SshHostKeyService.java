package io.redhat.na.ssp.tasktally.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Service for fetching SSH host keys from remote servers. Implements functionality similar to ssh-keyscan.
 */
@ApplicationScoped
public class SshHostKeyService {
  private static final Logger LOG = Logger.getLogger(SshHostKeyService.class);
  private static final int SSH_PORT = 22;
  private static final int CONNECT_TIMEOUT_MS = 10000; // 10 seconds
  private static final int READ_TIMEOUT_MS = 5000; // 5 seconds

  // Pattern to match SSH host key lines: hostname key-type base64-key
  private static final Pattern HOST_KEY_PATTERN = Pattern.compile(
      "^([^\\s]+)\\s+(ssh-rsa|ssh-ed25519|ecdsa-sha2-nistp256|ecdsa-sha2-nistp384|ecdsa-sha2-nistp521|ssh-dss)\\s+([A-Za-z0-9+/=]+)\\s*$");

  /**
   * Fetches SSH host keys from the specified hostname.
   * 
   * @param hostname
   *          the hostname to fetch keys from
   * @return list of host key entries in known_hosts format
   * @throws IOException
   *           if connection fails or host keys cannot be retrieved
   */
  public List<String> fetchHostKeys(String hostname) throws IOException {
    if (hostname == null || hostname.trim().isEmpty()) {
      throw new IllegalArgumentException("hostname is required");
    }

    String cleanHostname = hostname.trim();
    LOG.debugf("Fetching SSH host keys from: %s", cleanHostname);

    try (Socket socket = new Socket()) {
      // Set connection timeout
      socket.connect(new java.net.InetSocketAddress(cleanHostname, SSH_PORT), CONNECT_TIMEOUT_MS);
      socket.setSoTimeout(READ_TIMEOUT_MS);

      // Send SSH protocol version
      String clientVersion = "SSH-2.0-OpenSSH_8.9p1\r\n";
      socket.getOutputStream().write(clientVersion.getBytes(StandardCharsets.UTF_8));
      socket.getOutputStream().flush();

      // Read server response
      BufferedReader reader = new BufferedReader(
          new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
      String serverVersion = reader.readLine();

      if (serverVersion == null || !serverVersion.startsWith("SSH-")) {
        throw new IOException("Invalid SSH server response: " + serverVersion);
      }

      LOG.debugf("Connected to SSH server: %s", serverVersion);

      // Read additional lines until we get the host key
      List<String> hostKeys = new ArrayList<>();
      String line;
      int lineCount = 0;
      int maxLines = 50; // Prevent infinite loops

      while ((line = reader.readLine()) != null && lineCount < maxLines) {
        lineCount++;

        // Skip empty lines and SSH protocol messages
        if (line.trim().isEmpty() || line.startsWith("SSH-")) {
          continue;
        }

        // Try to parse as host key
        Matcher matcher = HOST_KEY_PATTERN.matcher(line);
        if (matcher.matches()) {
          String keyType = matcher.group(2);
          String keyData = matcher.group(3);

          // Validate the key data is valid base64
          try {
            Base64.getDecoder().decode(keyData);
            String hostKeyEntry = cleanHostname + " " + keyType + " " + keyData;
            hostKeys.add(hostKeyEntry);
            LOG.debugf("Found host key: %s %s", keyType, getKeyFingerprint(keyData));
          } catch (IllegalArgumentException e) {
            LOG.debugf("Invalid base64 key data: %s", keyData);
          }
        }

        // If we've found some keys and haven't seen new data for a while, break
        if (!hostKeys.isEmpty() && lineCount > 10) {
          break;
        }
      }

      if (hostKeys.isEmpty()) {
        throw new IOException("No valid SSH host keys found from " + cleanHostname);
      }

      LOG.infof("Successfully fetched %d host keys from %s", hostKeys.size(), cleanHostname);
      return hostKeys;

    } catch (java.net.ConnectException e) {
      throw new IOException("Failed to connect to " + cleanHostname + ":" + SSH_PORT + " - " + e.getMessage(), e);
    } catch (java.net.SocketTimeoutException e) {
      throw new IOException("Connection timeout to " + cleanHostname + ":" + SSH_PORT, e);
    }
  }

  /**
   * Fetches SSH host keys and returns them in known_hosts format.
   * 
   * @param hostname
   *          the hostname to fetch keys from
   * @return known_hosts content as a string
   * @throws IOException
   *           if connection fails or host keys cannot be retrieved
   */
  public String fetchKnownHosts(String hostname) throws IOException {
    List<String> hostKeys = fetchHostKeys(hostname);
    return String.join("\n", hostKeys) + "\n";
  }

  /**
   * Generates a SHA-256 fingerprint for a base64-encoded key.
   * 
   * @param base64Key
   *          the base64-encoded key
   * @return SHA-256 fingerprint in base64 format
   */
  private String getKeyFingerprint(String base64Key) {
    try {
      byte[] keyBytes = Base64.getDecoder().decode(base64Key);
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] fingerprint = md.digest(keyBytes);
      return Base64.getEncoder().encodeToString(fingerprint);
    } catch (NoSuchAlgorithmException e) {
      return "unknown";
    }
  }

  /**
   * Validates if a hostname is reachable and has SSH service.
   * 
   * @param hostname
   *          the hostname to validate
   * @return true if SSH service is available
   */
  public boolean isSshServiceAvailable(String hostname) {
    if (hostname == null || hostname.trim().isEmpty()) {
      return false;
    }

    try (Socket socket = new Socket()) {
      socket.connect(new java.net.InetSocketAddress(hostname.trim(), SSH_PORT), 5000);
      return true;
    } catch (IOException e) {
      LOG.debugf("SSH service not available on %s: %s", hostname, e.getMessage());
      return false;
    }
  }
}
