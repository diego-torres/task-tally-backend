package io.redhat.na.ssp.tasktally.repo;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.redhat.na.ssp.tasktally.model.UserPreferences;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

@ApplicationScoped
public class UserPreferencesRepository implements PanacheRepository<UserPreferences> {

    public Optional<UserPreferences> findByUserId(String userId) {
        return find("userId", userId).firstResultOptional();
    }
}
