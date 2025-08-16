package io.redhat.na.ssp.tasktally.secret;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves secrets from Kubernetes mounted files or environment variables.
 */
public class KubernetesSecretResolver implements SecretResolver {
  private static final Pattern REF = Pattern.compile("k8s:secret/([^#]+)#(.+)");
  private final Path basePath;

  public KubernetesSecretResolver() {
    String env = System.getenv().getOrDefault("K8S_SECRET_BASE_PATH", "/var/run/secrets");
    this.basePath = Paths.get(env);
  }

  // Package-private constructor for tests
  KubernetesSecretResolver(Path basePath) {
    this.basePath = basePath;
  }

  @Override
  public String resolve(String ref) {
    return new String(resolveBytes(ref), StandardCharsets.UTF_8);
  }

  @Override
  public byte[] resolveBytes(String ref) {
    Matcher m = REF.matcher(ref);
    if (!m.matches()) {
      throw new IllegalArgumentException("Unsupported ref: " + ref);
    }
    String name = m.group(1);
    String key = m.group(2);

    Path file = basePath.resolve(name).resolve(key);
    if (Files.exists(file)) {
      try {
        return Files.readAllBytes(file);
      } catch (IOException e) {
        throw new IllegalStateException("Failed to read secret file", e);
      }
    }

    String envKey = (name + "_" + key).toUpperCase(Locale.ROOT).replace('-', '_').replace('.', '_');
    String envVal = System.getenv(envKey);
    if (envVal != null) {
      return envVal.getBytes(StandardCharsets.UTF_8);
    }
    throw new IllegalArgumentException("Secret not found: " + ref);
  }
}
