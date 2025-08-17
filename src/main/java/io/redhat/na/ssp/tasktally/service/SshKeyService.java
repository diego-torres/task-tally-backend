package io.redhat.na.ssp.tasktally.service;

import io.redhat.na.ssp.tasktally.api.SshKeyCreateRequest;
import io.redhat.na.ssp.tasktally.model.CredentialRef;
import io.redhat.na.ssp.tasktally.secrets.SecretWriter;
import io.redhat.na.ssp.tasktally.secrets.SshKeyValidator;
import io.redhat.na.ssp.tasktally.secrets.SshSecretRefs;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.jboss.logging.Logger;

@ApplicationScoped
public class SshKeyService {
  private static final Logger LOG = Logger.getLogger(SshKeyService.class);
  private static final Set<String> ALLOWED_PROVIDERS = Set.of("github", "gitlab");

  @Inject
  SecretWriter secretWriter;
  @Inject
  CredentialStore store;

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
}
