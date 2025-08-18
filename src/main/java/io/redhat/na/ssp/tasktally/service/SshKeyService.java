package io.redhat.na.ssp.tasktally.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.sshd.common.config.keys.PublicKeyEntry;
import org.apache.sshd.common.config.keys.loader.openssh.OpenSSHKeyPairResourceParser;
import org.apache.sshd.common.config.keys.writer.openssh.OpenSSHKeyEncryptionContext;
import org.apache.sshd.common.config.keys.writer.openssh.OpenSSHKeyPairResourceWriter;
import org.apache.sshd.common.util.io.resource.IoResource;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import io.redhat.na.ssp.tasktally.api.SshKeyCreateRequest;
import io.redhat.na.ssp.tasktally.api.SshKeyGenerateRequest;
import io.redhat.na.ssp.tasktally.model.CredentialRef;
import io.redhat.na.ssp.tasktally.secret.SecretResolver;
import io.redhat.na.ssp.tasktally.secrets.SecretWriter;
import io.redhat.na.ssp.tasktally.secrets.SshKeyValidator;
import io.redhat.na.ssp.tasktally.secrets.SshSecretRefs;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class SshKeyService {
  static {
    // Register EdDSA provider once at class load
    if (java.security.Security.getProvider("EdDSA") == null) {
      java.security.Security.addProvider(new net.i2p.crypto.eddsa.EdDSASecurityProvider());
    }
  }
  private static final Logger LOG = Logger.getLogger(SshKeyService.class);
  private static final Set<String> ALLOWED_PROVIDERS = Set.of("github", "gitlab");

  @Inject
  SecretWriter secretWriter;
  @Inject
  CredentialStore store;
  @Inject
  SecretResolver secretResolver;

  @ConfigProperty(name = "ssh.kdf.rounds", defaultValue = "16")
  int kdfRounds;

  @ConfigProperty(name = "ssh.encryption.required", defaultValue = "false")
  boolean encryptionRequired;

  public List<CredentialRef> list(String userId) {
    return store.list(userId);
  }

  public CredentialRef create(String userId, SshKeyCreateRequest req) {
    if (req == null) {
      throw new IllegalArgumentException("request required");
    }
    String name = req.name != null ? req.name.trim() : null;
    if (name == null || name.isEmpty()) {
      throw new IllegalArgumentException("name is required");
    }
    if (store.find(userId, name).isPresent()) {
      throw new IllegalStateException("credential already exists");
    }
    String provider = req.provider != null ? req.provider.trim().toLowerCase(Locale.ROOT) : null;
    if (provider == null || !ALLOWED_PROVIDERS.contains(provider)) {
      throw new IllegalArgumentException("provider must be github or gitlab");
    }
    byte[] priv = req.privateKeyPem != null ? req.privateKeyPem.getBytes(StandardCharsets.UTF_8) : null;
    SshKeyValidator.validatePrivateKey(priv);
    byte[] kh = req.knownHosts != null ? ensureTrailingNewline(req.knownHosts).getBytes(StandardCharsets.UTF_8) : null;
    SshKeyValidator.validateKnownHosts(kh);
    char[] pp = req.passphrase != null ? req.passphrase.toCharArray() : null;
    SshKeyValidator.validatePassphrase(pp);

    SshSecretRefs refs = secretWriter.writeSshKey(userId, name, priv, null, pp, kh);

    CredentialRef cred = new CredentialRef();
    cred.name = name;
    cred.provider = provider;
    cred.scope = "write";
    cred.secretRef = refs.privateKeyRef();
    cred.knownHostsRef = refs.knownHostsRef();
    cred.passphraseRef = refs.passphraseRef();
    cred.createdAt = Instant.now();
    store.put(userId, cred);
    return cred;
  }

  public void delete(String userId, String name) {
    CredentialRef cred = store.find(userId, name).orElseThrow(() -> new IllegalArgumentException("not found"));
    try {
      secretWriter.deleteByRef(cred.secretRef);
      secretWriter.deleteByRef(cred.knownHostsRef);
      secretWriter.deleteByRef(cred.passphraseRef);
    } catch (Exception e) {
      LOG.warn("Failed to delete secret", e);
    }
    store.remove(userId, name);
  }

  public CredentialRef get(String userId, String name) {
    return store.find(userId, name).orElseThrow(() -> new IllegalArgumentException("not found"));
  }

  public CredentialRef generate(String userId, SshKeyGenerateRequest req) {
    if (req == null) {
      throw new IllegalArgumentException("request required");
    }
    String name = req.name != null ? req.name.trim() : null;
    if (name == null || name.isEmpty()) {
      throw new IllegalArgumentException("name is required");
    }
    if (store.find(userId, name).isPresent()) {
      throw new IllegalStateException("credential already exists");
    }
    String provider = req.provider != null ? req.provider.trim().toLowerCase(Locale.ROOT) : null;
    if (provider == null || !ALLOWED_PROVIDERS.contains(provider)) {
      throw new IllegalArgumentException("provider must be github or gitlab");
    }
    byte[] khRaw = req.knownHosts != null
        ? ensureTrailingNewline(req.knownHosts).getBytes(StandardCharsets.UTF_8)
        : null;
    SshKeyValidator.validateKnownHosts(khRaw);
    char[] pp = req.passphrase != null ? req.passphrase.toCharArray() : null;
    SshKeyValidator.validatePassphrase(pp);
    if (encryptionRequired && (pp == null || pp.length == 0)) {
      throw new IllegalArgumentException("passphrase required");
    }

    try {
      KeyPairGenerator kpg = KeyPairGenerator.getInstance("Ed25519", "EdDSA");
      KeyPair kp = kpg.generateKeyPair();
      byte[] privateOpenSsh = writeOpenSshPrivateKey(kp, req.passphrase);

      String publicLine = buildOpenSshPublic(kp.getPublic(), userId, req.comment);
      byte[] publicOpenSsh = (publicLine + "\n").getBytes(StandardCharsets.UTF_8);

      SshSecretRefs refs = secretWriter.writeSshKey(userId, name, privateOpenSsh, publicOpenSsh, pp, khRaw);

      CredentialRef cred = new CredentialRef();
      cred.name = name;
      cred.provider = provider;
      cred.scope = "write";
      cred.secretRef = refs.privateKeyRef();
      cred.knownHostsRef = refs.knownHostsRef();
      cred.passphraseRef = refs.passphraseRef();
      cred.createdAt = Instant.now();
      store.put(userId, cred);
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
      String pubRef = privRef.replace("#id_ed25519", "#id_ed25519.pub");
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
        Iterable<KeyPair> keys = OpenSSHKeyPairResourceParser.INSTANCE.loadKeyPairs(null,
            (IoResource<?>) new ByteArrayInputStream(priv), null);
        KeyPair kp = keys.iterator().next();
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

  private byte[] writeOpenSshPrivateKey(KeyPair kp, String passphrase) {
    try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
      OpenSSHKeyPairResourceWriter writer = new OpenSSHKeyPairResourceWriter();
      if (passphrase == null || passphrase.isBlank()) {
        writer.writePrivateKey(kp, null, null, bos);
      } else {
        OpenSSHKeyEncryptionContext enc = new OpenSSHKeyEncryptionContext();
        enc.setCipherName("aes256-ctr");
        enc.setKdfRounds(getConfiguredKdfRounds());
        enc.setPassword(passphrase);
        writer.writePrivateKey(kp, null, enc, bos);
      }
      return bos.toByteArray();
    } catch (Exception e) {
      throw new IllegalArgumentException("Failed to write OpenSSH private key: " + e.getMessage(), e);
    }
  }

  private int getConfiguredKdfRounds() {
    return kdfRounds;
  }

  private String buildOpenSshPublic(java.security.PublicKey pub, String userId, String comment) {
    String base = PublicKeyEntry.toString(pub);
    String c = (comment == null || comment.isBlank()) ? "task-tally@" + userId : comment.trim();
    return base + " " + c;
  }

  private String ensureTrailingNewline(String s) {
    return s.endsWith("\n") ? s : s + "\n";
  }
}
