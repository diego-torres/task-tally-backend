package io.redhat.na.ssp.tasktally.secrets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import io.redhat.na.ssp.tasktally.secret.KubernetesSecretResolver;

public class KubernetesSecretResolverTest {

  @Test
  void resolvesFile() throws Exception {
    Path base = Files.createTempDirectory("k8ssec");
    Path secret = base.resolve("mysecret");
    Files.createDirectories(secret);
    Files.writeString(secret.resolve("token"), "abc");
    KubernetesSecretResolver r = new KubernetesSecretResolver(base.toString());
    assertEquals("abc", r.resolve("k8s:secret/mysecret#token"));
  }

  @Test
  void resolvesEnv() {
    System.setProperty("env.MYSECRET_TOKEN", "xyz");
    KubernetesSecretResolver r = new KubernetesSecretResolver("/nope");
    assertEquals("xyz", r.resolve("k8s:secret/mysecret#token"));
  }
}
