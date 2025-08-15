package io.redhat.na.ssp.tasktally;

import org.testcontainers.containers.PostgreSQLContainer;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import java.util.HashMap;
import java.util.Map;

public class PostgresTestResource implements QuarkusTestResourceLifecycleManager {
    private PostgreSQLContainer<?> postgres;

    @Override
    public Map<String, String> start() {
        postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("tasktally")
            .withUsername("test")
            .withPassword("test");
        postgres.start();

        Map<String, String> config = new HashMap<>();
        config.put("quarkus.datasource.jdbc.url", postgres.getJdbcUrl());
        config.put("quarkus.datasource.username", postgres.getUsername());
        config.put("quarkus.datasource.password", postgres.getPassword());
        return config;
    }

    @Override
    public void stop() {
        if (postgres != null) {
            postgres.stop();
        }
    }
}
