package io.redhat.na.ssp.tasktally.service;

import io.redhat.na.ssp.tasktally.model.CredentialRef;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory credential store used for testing. In production this would be backed
 * by Postgres and Flyway migrations.
 */
@ApplicationScoped
public class CredentialStore {
  private final Map<String, CredentialRef> store = new ConcurrentHashMap<>();

  public void put(String userId, CredentialRef cred) {
    store.put(key(userId, cred.getName()), cred);
  }

  public Optional<CredentialRef> find(String userId, String name) {
    return Optional.ofNullable(store.get(key(userId, name)));
  }

  private String key(String userId, String name) {
    return userId + ":" + name;
  }
}
