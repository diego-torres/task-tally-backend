package io.redhat.na.ssp.tasktally.secret;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class KubernetesSecretResolverTest {
  @Test
  void readsFileValue() throws Exception {
    Path base = Files.createTempDirectory("sec");
    Path dir = base.resolve("mysecret");
    Files.createDirectories(dir);
    Files.writeString(dir.resolve("token"), "value");
    KubernetesSecretResolver resolver = new KubernetesSecretResolver(base);
    assertEquals("value", resolver.resolve("k8s:secret/mysecret#token"));
  }
}
