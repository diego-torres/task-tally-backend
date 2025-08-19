package io.redhat.na.ssp.tasktally.repo;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.redhat.na.ssp.tasktally.model.CredentialRef;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

@ApplicationScoped
public class CredentialRefRepository implements PanacheRepository<CredentialRef> {

  public Optional<CredentialRef> findByUserAndName(Long userPreferencesId, String name) {
    return find("userPreferences.id = ?1 and name = ?2", userPreferencesId, name).firstResultOptional();
  }

  public java.util.List<CredentialRef> findByUserId(String userId) {
    return find("userPreferences.userId", userId).list();
  }
}
