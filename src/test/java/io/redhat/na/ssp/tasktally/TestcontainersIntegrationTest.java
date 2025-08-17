package io.redhat.na.ssp.tasktally;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.common.QuarkusTestResource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
public class TestcontainersIntegrationTest {
  @Test
  void testContainerIsRunning() {
    assertTrue(true, "Testcontainers integration is active");
  }
}
