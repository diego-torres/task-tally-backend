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

@ApplicationScoped
public class PreferencesService {

    @Inject
    UserPreferencesRepository userRepo;

    @Inject
    CredentialRefRepository credentialRepo;

    @Transactional
    public UserPreferences getOrCreate(String userId) {
        return userRepo.findByUserId(userId).orElseGet(() -> {
            UserPreferences up = new UserPreferences();
            up.userId = userId;
            userRepo.persist(up);
            return up;
        });
    }

    @Transactional
    public UserPreferences upsert(String userId, UserPreferences incoming) {
        UserPreferences existing = userRepo.findByUserId(userId).orElse(null);
        if (existing == null) {
            incoming.id = null;
            incoming.userId = userId;
            userRepo.persist(incoming);
            return incoming;
        } else {
            if (!Objects.equals(existing.version, incoming.version)) {
                throw new OptimisticLockException("version mismatch");
            }
            existing.ui = incoming.ui;
            existing.defaultGitProvider = incoming.defaultGitProvider;
            userRepo.persist(existing);
            return existing;
        }
    }

    @Transactional
    public CredentialRef addCredential(String userId, CredentialRef ref) {
        UserPreferences prefs = userRepo.findByUserId(userId).orElseThrow(NotFoundException::new);
        ref.id = null;
        ref.userPreferences = prefs;
        credentialRepo.persist(ref);
        return ref;
    }

    @Transactional
    public void deleteCredential(String userId, String name) {
        UserPreferences prefs = userRepo.findByUserId(userId).orElseThrow(NotFoundException::new);
        credentialRepo.delete("userPreferences = ?1 and name = ?2", prefs, name);
    }

    public CredentialRef findCredential(String userId, String name) {
        UserPreferences prefs = userRepo.findByUserId(userId).orElseThrow(NotFoundException::new);
        return credentialRepo.findByUserAndName(prefs.id, name).orElseThrow(NotFoundException::new);
    }
}
