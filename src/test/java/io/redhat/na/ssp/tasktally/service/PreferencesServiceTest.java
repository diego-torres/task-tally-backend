package io.redhat.na.ssp.tasktally.service;

import io.redhat.na.ssp.tasktally.model.UserPreferences;
import jakarta.inject.Inject;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import java.util.Map;

@io.quarkus.test.junit.QuarkusTest
public class PreferencesServiceTest {

    @Inject
    PreferencesService service;

    @Test
    public void optimisticLocking() {
        String userId = "u1";
        UserPreferences prefs = service.getOrCreate(userId);
        Integer ver = prefs.version;
        UserPreferences update1 = new UserPreferences();
        update1.ui = Map.of("theme", "dark");
        update1.version = ver;
        service.upsert(userId, update1);
        UserPreferences update2 = new UserPreferences();
        update2.ui = Map.of("theme", "light");
        update2.version = ver; // stale
        Assertions.assertThrows(OptimisticLockException.class, () -> service.upsert(userId, update2));
    }
}
