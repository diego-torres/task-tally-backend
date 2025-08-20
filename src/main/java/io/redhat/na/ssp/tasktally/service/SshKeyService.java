package io.redhat.na.ssp.tasktally.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.sshd.common.NamedResource;
import org.apache.sshd.common.config.keys.FilePasswordProvider;
import org.apache.sshd.common.config.keys.loader.openssh.OpenSSHKeyPairResourceParser;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import io.redhat.na.ssp.tasktally.api.SshKeyCreateRequest;
import io.redhat.na.ssp.tasktally.api.SshKeyGenerateRequest;
import io.redhat.na.ssp.tasktally.github.ssh.TaskTallySshdSessionFactory;
import io.redhat.na.ssp.tasktally.model.CredentialRef;
import io.redhat.na.ssp.tasktally.model.UserPreferences;
import io.redhat.na.ssp.tasktally.repo.CredentialRefRepository;
import io.redhat.na.ssp.tasktally.repo.UserPreferencesRepository;
import io.redhat.na.ssp.tasktally.secret.SecretResolver;
import io.redhat.na.ssp.tasktally.secrets.SecretWriter;
import io.redhat.na.ssp.tasktally.secrets.SshKeyValidator;
import io.redhat.na.ssp.tasktally.secrets.SshSecretRefs;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class SshKeyService {
  private static final Logger LOG = Logger.getLogger(SshKeyService.class);
  private static final Set<String> ALLOWED_PROVIDERS = Set.of("github", "gitlab");

  // Default GitHub's SSH host keys - fallback when dynamic fetching fails
  // These are GitHub's current host keys as of 2024
  private static final String GITHUB_HOST_KEYS = """
      github.com ssh-rsa AAAAB3NzaC1yc2EAAAABIwAAAQEAq2A7hRGmdnm9tUDbO9IDSwBK6TbQa+PXYPCPy6rbTrTtw7PHkccKrpp0yVhp5HdEIcKr6pLlVDBfOLX9QUsyCOV0wzfjIJNlGEYsdlLJizHhbn2mUjvSAHQqZETYP81eFzLQNnPHt4EVVUh7VfDESU84KezmD5QlWpXLmvU31/yMf+Se8xhHTvKSCZIFImWwoG6mbUoWf9nzpIoaSjB+weqqUUmpaaasXVal72J+UX2B+2RPW3RcT0eOzQgqlJL3RKrTJvdsjE3JEAvGq3lGHSZXy28G3skua2SmVi/w4yCE6gbODqnTWlg7+wC604ydGXA8VJiS5ap43JXiUFFAaQ==
      github.com ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIOMqqnkVzrm0SdG6UOoqKLsabgH5C9okWi0dh2l9GKJl
      github.com ecdsa-sha2-nistp256 AAAAE2VjZHNhLXNoYTItbmlzdHAyNTYAAAAIbmlzdHAyNTYAAABBBEmKSENjQEezOmxkZMy7opKgwFB9nkt5YRrYMjNuG5N87uRgg6CLrbo5wAdT/y6v0mKV0U2w0WZ2YB/++Tpockg=
      """;

  @Inject
  SecretWriter secretWriter;
  @Inject
  CredentialRefRepository credentialRefRepository;
  @Inject
  UserPreferencesRepository userPreferencesRepository;
  @Inject
  SecretResolver secretResolver;
  @Inject
  SshHostKeyService sshHostKeyService;

  @ConfigProperty(name = "ssh.encryption.required", defaultValue = "false")
  boolean encryptionRequired;

  @Transactional
  public List<CredentialRef> list(String userId) {
    LOG.debugf("Listing SSH credentials for user: %s", userId);
    List<CredentialRef> credentials = credentialRefRepository.findByUserId(userId);
    LOG.debugf("Found %d SSH credentials for user: %s", credentials.size(), userId);
    return credentials;
  }

  @Transactional
  public CredentialRef create(String userId, SshKeyCreateRequest req) {
    if (req == null) {
      throw new IllegalArgumentException("request required");
    }
    String name = req.name != null ? req.name.trim() : null;
    if (name == null || name.isEmpty()) {
      throw new IllegalArgumentException("name is required");
    }

    UserPreferences userPrefs = getUserPreferences(userId);
    if (credentialRefRepository.findByUserAndName(userPrefs.id, name).isPresent()) {
      throw new IllegalStateException("credential already exists");
    }

    String provider = req.provider != null ? req.provider.trim().toLowerCase(Locale.ROOT) : null;
    if (provider == null || !ALLOWED_PROVIDERS.contains(provider)) {
      throw new IllegalArgumentException("provider must be github or gitlab");
    }
    byte[] priv = req.privateKeyPem != null ? req.privateKeyPem.getBytes(StandardCharsets.UTF_8) : null;
    SshKeyValidator.validatePrivateKey(priv);

    // Handle known hosts - either provided directly or fetched from hostname
    byte[] kh;
    if (req.knownHosts != null && !req.knownHosts.trim().isEmpty()) {
      kh = ensureTrailingNewline(req.knownHosts).getBytes(StandardCharsets.UTF_8);
    } else if (req.hostname != null && !req.hostname.trim().isEmpty()) {
      kh = fetchKnownHostsWithFallback(req.hostname.trim(), name);
    } else {
      kh = null;
    }
    SshKeyValidator.validateKnownHosts(kh);

    char[] pp = req.passphrase != null ? req.passphrase.toCharArray() : null;
    SshKeyValidator.validatePassphrase(pp);

    SshSecretRefs refs = secretWriter.writeSshKey(userId, name, priv, null, pp, kh);

    CredentialRef cred = new CredentialRef();
    cred.userPreferences = userPrefs;
    cred.name = name;
    cred.provider = provider;
    cred.scope = "write";
    cred.secretRef = refs.privateKeyRef();
    cred.knownHostsRef = refs.knownHostsRef();
    cred.passphraseRef = refs.passphraseRef();
    cred.createdAt = Instant.now();

    credentialRefRepository.persist(cred);
    LOG.infof("Created SSH credential %s for user %s", name, userId);
    return cred;
  }

  @Transactional
  public void delete(String userId, String name) {
    String trimmedName = name != null ? name.trim() : null;
    UserPreferences userPrefs = getUserPreferences(userId);
    CredentialRef cred = credentialRefRepository.findByUserAndName(userPrefs.id, trimmedName)
        .orElseThrow(() -> new IllegalArgumentException("not found"));

    try {
      secretWriter.deleteByRef(cred.secretRef);
      secretWriter.deleteByRef(cred.knownHostsRef);
      secretWriter.deleteByRef(cred.passphraseRef);
    } catch (Exception e) {
      LOG.warn("Failed to delete secret", e);
    }

    credentialRefRepository.delete(cred);
    LOG.infof("Deleted SSH credential %s for user %s", trimmedName, userId);
  }

  @Transactional
  public CredentialRef get(String userId, String name) {
    String trimmedName = name != null ? name.trim() : null;
    UserPreferences userPrefs = getUserPreferences(userId);
    return credentialRefRepository.findByUserAndName(userPrefs.id, trimmedName)
        .orElseThrow(() -> new IllegalArgumentException("not found"));
  }

  @Transactional
  public CredentialRef generate(String userId, SshKeyGenerateRequest req) {
    if (req == null) {
      throw new IllegalArgumentException("request required");
    }
    String name = req.name != null ? req.name.trim() : null;
    if (name == null || name.isEmpty()) {
      throw new IllegalArgumentException("name is required");
    }

    UserPreferences userPrefs = getUserPreferences(userId);
    if (credentialRefRepository.findByUserAndName(userPrefs.id, name).isPresent()) {
      throw new IllegalStateException("credential already exists");
    }

    String provider = req.provider != null ? req.provider.trim().toLowerCase(Locale.ROOT) : null;
    if (provider == null || !ALLOWED_PROVIDERS.contains(provider)) {
      throw new IllegalArgumentException("provider must be github or gitlab");
    }
    // Handle known hosts - either provided directly or fetched from hostname
    byte[] khRaw;
    if (req.knownHosts != null && !req.knownHosts.trim().isEmpty()) {
      khRaw = ensureTrailingNewline(req.knownHosts).getBytes(StandardCharsets.UTF_8);
    } else if (req.hostname != null && !req.hostname.trim().isEmpty()) {
      khRaw = fetchKnownHostsWithFallback(req.hostname.trim(), name);
    } else {
      khRaw = null;
    }
    SshKeyValidator.validateKnownHosts(khRaw);
    char[] pp = req.passphrase != null ? req.passphrase.toCharArray() : null;
    SshKeyValidator.validatePassphrase(pp);
    if (encryptionRequired && (pp == null || pp.length == 0)) {
      throw new IllegalArgumentException("passphrase required");
    }

    try {
      KeyPairGenerator kpg = KeyPairGenerator.getInstance("Ed25519");
      KeyPair kp = kpg.generateKeyPair();
      byte[] privatePem = writePkcs8Pem(kp.getPrivate());

      String publicLine = buildOpenSshPublic(kp.getPublic(), userId, req.comment);
      byte[] publicOpenSsh = (publicLine + "\n").getBytes(StandardCharsets.UTF_8);

      SshSecretRefs refs = secretWriter.writeSshKey(userId, name, privatePem, publicOpenSsh, pp, khRaw);

      CredentialRef cred = new CredentialRef();
      cred.userPreferences = userPrefs;
      cred.name = name;
      cred.provider = provider;
      cred.scope = "write";
      cred.secretRef = refs.privateKeyRef();
      cred.knownHostsRef = refs.knownHostsRef();
      cred.passphraseRef = refs.passphraseRef();
      cred.createdAt = Instant.now();

      credentialRefRepository.persist(cred);
      LOG.infof("Generated SSH credential %s for user %s", name, userId);
      return cred;
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("failed to generate key", e);
    }
  }

  public String getPublicKey(String userId, String name) {
    CredentialRef cred = get(userId, name);
    String privRef = cred.secretRef;
    if (privRef == null) {
      throw new IllegalStateException("missing secret ref");
    }
    if (privRef.startsWith("k8s:secret/")) {
      String pubRef = privRef.replaceFirst("#id_ed25519", "#id_ed25519.pub");
      try {
        byte[] pub = secretResolver.resolveBytes(pubRef);
        if (pub != null && pub.length > 0) {
          return new String(pub, StandardCharsets.UTF_8).trim();
        }
      } catch (Exception e) {
        // ignore and fallback
      }
      try {
        byte[] priv = secretResolver.resolveBytes(privRef);
        NamedResource named = NamedResource.ofName("id_ed25519");
        FilePasswordProvider fpp = cred.passphraseRef != null
            ? FilePasswordProvider.of(secretResolver.resolve(cred.passphraseRef))
            : FilePasswordProvider.EMPTY;
        KeyPair kp;
        try (ByteArrayInputStream in = new ByteArrayInputStream(priv)) {
          Iterable<KeyPair> keys = OpenSSHKeyPairResourceParser.INSTANCE.loadKeyPairs(null, named, fpp, in);
          kp = keys.iterator().next();
        }
        String publicLine = buildOpenSshPublic(kp.getPublic(), userId, null);
        byte[] publicBytes = (publicLine + "\n").getBytes(StandardCharsets.UTF_8);
        char[] passphrase = cred.passphraseRef != null
            ? secretResolver.resolve(cred.passphraseRef).toCharArray()
            : null;
        byte[] knownHosts = cred.knownHostsRef != null ? secretResolver.resolveBytes(cred.knownHostsRef) : null;
        secretWriter.writeSshKey(userId, cred.name, priv, publicBytes, passphrase, knownHosts);
        return publicLine;
      } catch (IOException | GeneralSecurityException e) {
        throw new IllegalStateException("failed to derive public key", e);
      }
    }
    throw new IllegalStateException("unsupported secret backend");
  }

  private UserPreferences getUserPreferences(String userId) {
    return userPreferencesRepository.findByUserId(userId).orElseGet(() -> {
      UserPreferences userPrefs = new UserPreferences();
      userPrefs.userId = userId;
      userPrefs.ui = new java.util.HashMap<>();
      userPreferencesRepository.persist(userPrefs);
      LOG.debugf("Created new UserPreferences for user: %s", userId);
      return userPrefs;
    });
  }

  private byte[] writePkcs8Pem(PrivateKey privateKey) {
    String base64 = java.util.Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(privateKey.getEncoded());
    String pem = "-----BEGIN PRIVATE KEY-----\n" + base64 + "\n-----END PRIVATE KEY-----\n";
    return pem.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
  }

  private String buildOpenSshPublic(java.security.PublicKey pub, String userId, String comment) {
    String c = (comment == null || comment.isBlank()) ? "task-tally@" + userId : comment.trim();
    String keyType = "ssh-ed25519";

    // Convert to OpenSSH wire format
    byte[] sshKeyBytes;
    if (pub instanceof java.security.interfaces.EdECPublicKey) {
      // For Ed25519, we need to extract the raw key bytes and format them for SSH
      java.security.interfaces.EdECPublicKey edKey = (java.security.interfaces.EdECPublicKey) pub;
      java.security.spec.EdECPoint point = edKey.getPoint();
      byte[] rawKey = point.getY().toByteArray();

      // SSH wire format: string "ssh-ed25519" + string key_data
      byte[] keyTypeBytes = keyType.getBytes(java.nio.charset.StandardCharsets.UTF_8);
      sshKeyBytes = new byte[4 + keyTypeBytes.length + 4 + rawKey.length];

      // Write key type length and key type
      sshKeyBytes[0] = (byte) ((keyTypeBytes.length >> 24) & 0xFF);
      sshKeyBytes[1] = (byte) ((keyTypeBytes.length >> 16) & 0xFF);
      sshKeyBytes[2] = (byte) ((keyTypeBytes.length >> 8) & 0xFF);
      sshKeyBytes[3] = (byte) (keyTypeBytes.length & 0xFF);
      System.arraycopy(keyTypeBytes, 0, sshKeyBytes, 4, keyTypeBytes.length);

      // Write key data length and key data
      int keyDataOffset = 4 + keyTypeBytes.length;
      sshKeyBytes[keyDataOffset] = (byte) ((rawKey.length >> 24) & 0xFF);
      sshKeyBytes[keyDataOffset + 1] = (byte) ((rawKey.length >> 16) & 0xFF);
      sshKeyBytes[keyDataOffset + 2] = (byte) ((rawKey.length >> 8) & 0xFF);
      sshKeyBytes[keyDataOffset + 3] = (byte) (rawKey.length & 0xFF);
      System.arraycopy(rawKey, 0, sshKeyBytes, keyDataOffset + 4, rawKey.length);
    } else {
      // Fallback for other key types
      sshKeyBytes = pub.getEncoded();
    }

    String encoded = java.util.Base64.getEncoder().encodeToString(sshKeyBytes);
    return keyType + " " + encoded + " " + c;
  }

  private String ensureTrailingNewline(String s) {
    return s.endsWith("\n") ? s : s + "\n";
  }

  /**
   * Fetches known hosts from the specified hostname with fallback to hardcoded keys.
   * 
   * @param hostname
   *          the hostname to fetch keys from
   * @param credentialName
   *          the name of the credential being created (for logging)
   * @return known hosts content as bytes
   */
  private byte[] fetchKnownHostsWithFallback(String hostname, String credentialName) {
    try {
      String fetchedKnownHosts = sshHostKeyService.fetchKnownHosts(hostname);
      LOG.infof("Automatically fetched host keys from %s for SSH credential %s", hostname, credentialName);
      return fetchedKnownHosts.getBytes(StandardCharsets.UTF_8);
    } catch (IOException e) {
      LOG.warnf("Failed to fetch host keys from %s: %s", hostname, e.getMessage());

      // Use hardcoded GitHub host keys as fallback for github.com
      if ("github.com".equals(hostname)) {
        LOG.infof("Using hardcoded GitHub host keys as fallback for SSH credential %s", credentialName);
        return GITHUB_HOST_KEYS.getBytes(StandardCharsets.UTF_8);
      }

      // For other hostnames, throw the original exception
      throw new IllegalArgumentException("Failed to fetch host keys from " + hostname + ": " + e.getMessage());
    }
  }
}
