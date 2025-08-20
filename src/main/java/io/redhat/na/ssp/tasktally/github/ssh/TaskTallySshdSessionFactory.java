package io.redhat.na.ssp.tasktally.github.ssh;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.jgit.transport.sshd.SshdSessionFactory;
import org.eclipse.jgit.transport.sshd.SshdSessionFactoryBuilder;
import org.jboss.logging.Logger;

/**
 * Builds an {@link SshdSessionFactory} from in-memory key material.
 */
public final class TaskTallySshdSessionFactory {
  private static final Logger LOG = Logger.getLogger(TaskTallySshdSessionFactory.class);

  private TaskTallySshdSessionFactory() {
  }

  // Environment variable for known_hosts file location
  private static final String KNOWN_HOSTS_PATH = "KNOWN_HOSTS_FILE";
  private static final String DEFAULT_KNOWN_HOSTS_PATH = System.getProperty("user.home") + "/.ssh/known_hosts";

  /**
   * Reads the known_hosts file from the configured location.
   * 
   * @return the content of the known_hosts file
   * @throws IOException
   *           if the file cannot be read
   */
  private static String readKnownHostsFile() throws IOException {
    String knownHostsPath = System.getenv(KNOWN_HOSTS_PATH);
    if (knownHostsPath == null || knownHostsPath.trim().isEmpty()) {
      knownHostsPath = DEFAULT_KNOWN_HOSTS_PATH;
    }

    Path path = Path.of(knownHostsPath);
    LOG.debugf("Reading known_hosts from: %s", path);

    if (!Files.exists(path)) {
      throw new IOException("Known hosts file not found at " + path);
    }

    String content = Files.readString(path, StandardCharsets.UTF_8);
    LOG.debugf("Successfully read known_hosts file with %d characters", content.length());
    return content;
  }

  public static SshdSessionFactory create(byte[] privateKey, byte[] knownHosts, char[] passphrase, java.io.File sshDir)
      throws IOException {
    LOG.debug("Creating SSH session factory");
    java.nio.file.Path tempDir = sshDir != null ? sshDir.toPath() : java.nio.file.Files.createTempDirectory("jgit-ssh");
    LOG.debugf("Using temp directory: %s", tempDir);

    // Write private key to id_rsa in temp dir
    java.nio.file.Path keyFile = tempDir.resolve("id_rsa");
    java.nio.file.Files.write(keyFile, privateKey, java.nio.file.StandardOpenOption.CREATE);

    // Set proper permissions on the private key file (600)
    keyFile.toFile().setReadable(false, false);
    keyFile.toFile().setReadable(true, true);
    keyFile.toFile().setWritable(false, false);
    keyFile.toFile().setWritable(true, true);

    // Write known_hosts to temp dir
    java.nio.file.Path knownHostsFile = tempDir.resolve("known_hosts");

    // If knownHosts is null or empty, read from local known_hosts file
    byte[] hostsToWrite;
    if (knownHosts == null || knownHosts.length == 0) {
      String localKnownHosts = readKnownHostsFile();
      hostsToWrite = localKnownHosts.getBytes(StandardCharsets.UTF_8);
    } else {
      // Use the provided known_hosts as-is, since they should already be complete
      hostsToWrite = knownHosts;
    }

    java.nio.file.Files.write(knownHostsFile, hostsToWrite, java.nio.file.StandardOpenOption.CREATE);

    // Set proper permissions on the known_hosts file (644)
    knownHostsFile.toFile().setReadable(true, true);
    knownHostsFile.toFile().setWritable(true, true);

    LOG.debugf("Created known_hosts file with content: %s", new String(hostsToWrite, StandardCharsets.UTF_8));

    // Create SSH config file with strict host key checking enabled
    // This enables proper host key verification against the known_hosts file
    java.nio.file.Path configFile = tempDir.resolve("config");
    String configContent = "Host github.com\n" + "  HostName github.com\n" + "  User git\n" + "  IdentityFile "
        + keyFile.toString() + "\n" + "  StrictHostKeyChecking yes\n" + "  UserKnownHostsFile "
        + knownHostsFile.toString() + "\n" + "  LogLevel DEBUG\n";
    java.nio.file.Files.write(configFile, configContent.getBytes(StandardCharsets.UTF_8),
        java.nio.file.StandardOpenOption.CREATE);

    LOG.debugf("Created SSH config file with content: %s", configContent);

    SshdSessionFactoryBuilder builder = new SshdSessionFactoryBuilder();
    builder.setHomeDirectory(tempDir.toFile());
    builder.setSshDirectory(tempDir.toFile());

    LOG.debug("SSH session factory created successfully with proper host key verification");
    return builder.build(null);
  }
}
