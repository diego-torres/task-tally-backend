package io.redhat.na.ssp.tasktally.secrets;

import jakarta.enterprise.context.ApplicationScoped;
import io.quarkus.arc.Unremovable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ApplicationScoped
@Unremovable
public class KubernetesSecretResolver implements SecretResolver {
  private static final Pattern REF = Pattern.compile("k8s:secret/([^#]+)#(.+)");

  @Override
  public String resolve(String ref) {
    Matcher m = REF.matcher(ref);
    if (!m.matches()) {
      throw new IllegalArgumentException("Unsupported secret ref");
    }
    String name = m.group(1);
    String key = m.group(2);
    String basePath = System.getProperty("k8s.secret.base.path",
        System.getenv().getOrDefault("K8S_SECRET_BASE_PATH", "/var/run/secrets/tasktally"));
    Path p = Path.of(basePath, name, key);
    if (Files.exists(p)) {
      try {
        return Files.readString(p).trim();
      } catch (IOException e) {
        throw new IllegalStateException("Unable to read secret", e);
      }
    }
    String envVar = (name + "_" + key).toUpperCase(Locale.ROOT).replace('-', '_');
    String val = System.getenv(envVar);
    if (val == null) {
      val = System.getProperty("env." + envVar);
    }
    if (val != null) {
      return val;
    }
    throw new IllegalStateException("Secret not found");
  }
}
