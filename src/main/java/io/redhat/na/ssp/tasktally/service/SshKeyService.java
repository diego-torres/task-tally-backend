package io.redhat.na.ssp.tasktally.service;

import io.redhat.na.ssp.tasktally.api.SshKeyCreateRequest;
import io.redhat.na.ssp.tasktally.api.SshKeyGenerateRequest;
import io.redhat.na.ssp.tasktally.model.CredentialRef;
import io.redhat.na.ssp.tasktally.secret.SecretResolver;
import io.redhat.na.ssp.tasktally.secrets.SecretWriter;
import io.redhat.na.ssp.tasktally.secrets.SshKeyValidator;
import io.redhat.na.ssp.tasktally.secrets.SshSecretRefs;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.GeneralSecurityException;
import java.io.IOException;
import org.apache.sshd.common.config.keys.PublicKeyEntry;
import org.apache.sshd.common.config.keys.loader.openssh.OpenSSHKeyPairResourceParser;
import org.apache.sshd.common.config.keys.writer.openssh.OpenSSHKeyEncryptionContext;
import org.apache.sshd.common.config.keys.writer.openssh.OpenSSHKeyPairResourceWriter;
import org.apache.sshd.common.util.io.resource.IoResource;
import org.jboss.logging.Logger;

@ApplicationScoped
public class SshKeyService {
  private static final Logger LOG = Logger.getLogger(SshKeyService.class);
  private static final Set<String> ALLOWED_PROVIDERS = Set.of("github", "gitlab");

  @Inject
  SecretWriter secretWriter;
  @Inject
  CredentialStore store;
  @Inject
  SecretResolver secretResolver;

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
    byte[] kh = req.knownHosts != null ? req.knownHosts.getBytes(StandardCharsets.UTF_8) : null;
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
    byte[] kh = req.knownHosts != null ? req.knownHosts.getBytes(StandardCharsets.UTF_8) : null;
    SshKeyValidator.validateKnownHosts(kh);
    char[] pp = req.passphrase != null ? req.passphrase.toCharArray() : null;
    SshKeyValidator.validatePassphrase(pp);

    try {
      KeyPairGenerator kpg = KeyPairGenerator.getInstance("Ed25519");
      KeyPair kp = kpg.generateKeyPair();

      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      OpenSSHKeyEncryptionContext enc = new OpenSSHKeyEncryptionContext();
      if (pp != null && pp.length > 0) {
        enc.setPassword(new String(pp));
      }
      new OpenSSHKeyPairResourceWriter().writePrivateKey(kp, null, enc, bos);
      byte[] privateOpenSsh = bos.toByteArray();

      String pub = PublicKeyEntry.toString(kp.getPublic());
      String comment = (req.comment == null || req.comment.isBlank()) ? "task-tally@" + userId : req.comment.trim();
      String publicOpenSsh = pub + " " + comment;

      SshSecretRefs refs = secretWriter.writeSshKey(userId, name, privateOpenSsh,
          publicOpenSsh.getBytes(StandardCharsets.UTF_8), pp, kh);

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
    } catch (GeneralSecurityException | IOException e) {
      throw new IllegalStateException("failed to generate key", e);
    }
  }

  public String getPublicKey(String userId, String name) {
    CredentialRef cred = get(userId, name);
    return getPublicKey(cred);
  }

  public String getPublicKey(CredentialRef cred) {
    String privRef = cred.secretRef;
    if (privRef == null) {
      throw new IllegalStateException("missing secret ref");
    }
    if (privRef.startsWith("k8s:secret/")) {
      String pubRef = privRef.replace("#id_ed25519", "#id_ed25519.pub");
      try {
        byte[] pub = secretResolver.resolveBytes(pubRef);
        if (pub != null && pub.length > 0) {
          return new String(pub, StandardCharsets.UTF_8);
        }
      } catch (Exception e) {
        // ignore and fallback
      }
      try {
        byte[] priv = secretResolver.resolveBytes(privRef);
        Iterable<KeyPair> keys = OpenSSHKeyPairResourceParser.INSTANCE.loadKeyPairs(null, // SessionContext
            (IoResource<?>) new ByteArrayInputStream(priv), // InputStream
            null // FilePasswordProvider
        );
        KeyPair kp = keys.iterator().next();
        return PublicKeyEntry.toString(kp.getPublic());
      } catch (IOException | GeneralSecurityException e) {
        throw new IllegalStateException("failed to derive public key", e);
      }
    }
    throw new IllegalStateException("unsupported secret backend");
  }
}
