package io.redhat.na.ssp.tasktally.service;

import io.redhat.na.ssp.tasktally.model.CredentialRef;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.jboss.logging.Logger;

/**
 * Simple in-memory credential store used for testing. In production this would be backed
 * by Postgres and Flyway migrations.
 */
@ApplicationScoped
public class CredentialStore {
  private static final Logger LOG = Logger.getLogger(CredentialStore.class);
  private final Map<String, CredentialRef> store = new ConcurrentHashMap<>();

  public void put(String userId, CredentialRef cred) {
    LOG.debugf("Storing credential %s for user %s", cred.getName(), userId);
    store.put(key(userId, cred.getName()), cred);
    LOG.infof("Stored credential %s for user %s", cred.getName(), userId);
  }

  public Optional<CredentialRef> find(String userId, String name) {
    LOG.debugf("Fetching credential %s for user %s", name, userId);
    CredentialRef cred = store.get(key(userId, name));
    if (cred == null) {
      LOG.warnf("Credential %s for user %s not found", name, userId);
    }
    return Optional.ofNullable(cred);
  }

  private String key(String userId, String name) {
    return userId + ":" + name;
  }
}
