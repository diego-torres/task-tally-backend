package io.redhat.na.ssp.tasktally.service;

import io.redhat.na.ssp.tasktally.model.CredentialRef;
import io.redhat.na.ssp.tasktally.model.UserPreferences;
import io.redhat.na.ssp.tasktally.repo.CredentialRefRepository;
import io.redhat.na.ssp.tasktally.repo.UserPreferencesRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.OptimisticLockException;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

import java.util.Objects;
import org.jboss.logging.Logger;

@ApplicationScoped
public class PreferencesService {
  private static final Logger LOG = Logger.getLogger(PreferencesService.class);

  @Inject
  UserPreferencesRepository userRepo;

  @Inject
  CredentialRefRepository credentialRepo;

  @Transactional
  public UserPreferences getOrCreate(String userId) {
    LOG.debugf("Fetching preferences for user %s", userId);
    return userRepo.findByUserId(userId).orElseGet(() -> {
      LOG.infof("Creating new preferences for user %s", userId);
      UserPreferences up = new UserPreferences();
      up.userId = userId;
      userRepo.persist(up);
      return up;
    });
  }

  @Transactional
  public UserPreferences upsert(String userId, UserPreferences incoming) {
    LOG.debugf("Upserting preferences for user %s", userId);
    UserPreferences existing = userRepo.findByUserId(userId).orElse(null);
    if (existing == null) {
      LOG.infof("No existing preferences for user %s; creating", userId);
      incoming.id = null;
      incoming.userId = userId;
      userRepo.persist(incoming);
      return incoming;
    } else {
      if (!Objects.equals(existing.version, incoming.version)) {
        LOG.warnf("Version mismatch for user %s", userId);
        throw new OptimisticLockException("version mismatch");
      }
      existing.ui = incoming.ui;
      existing.defaultGitProvider = incoming.defaultGitProvider;
      userRepo.persist(existing);
      LOG.infof("Updated preferences for user %s", userId);
      return existing;
    }
  }

  @Transactional
  public CredentialRef addCredential(String userId, CredentialRef ref) {
    LOG.debugf("Adding credential %s for user %s", ref.getName(), userId);
    UserPreferences prefs = userRepo.findByUserId(userId).orElseThrow(() -> {
      LOG.errorf("User %s not found", userId);
      return new NotFoundException();
    });
    ref.id = null;
    ref.userPreferences = prefs;
    credentialRepo.persist(ref);
    LOG.infof("Added credential %s for user %s", ref.getName(), userId);
    return ref;
  }

  @Transactional
  public void deleteCredential(String userId, String name) {
    LOG.debugf("Deleting credential %s for user %s", name, userId);
    UserPreferences prefs = userRepo.findByUserId(userId).orElseThrow(() -> {
      LOG.errorf("User %s not found", userId);
      return new NotFoundException();
    });
    credentialRepo.delete("userPreferences = ?1 and name = ?2", prefs, name);
    LOG.infof("Deleted credential %s for user %s", name, userId);
  }

  public CredentialRef findCredential(String userId, String name) {
    LOG.debugf("Finding credential %s for user %s", name, userId);
    UserPreferences prefs = userRepo.findByUserId(userId).orElseThrow(() -> {
      LOG.errorf("User %s not found", userId);
      return new NotFoundException();
    });
    return credentialRepo.findByUserAndName(prefs.id, name).orElseThrow(() -> {
      LOG.errorf("Credential %s for user %s not found", name, userId);
      return new NotFoundException();
    });
  }
}
