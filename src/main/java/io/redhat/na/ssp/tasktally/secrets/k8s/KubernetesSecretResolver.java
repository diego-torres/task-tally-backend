package io.redhat.na.ssp.tasktally.secrets.k8s;

import io.redhat.na.ssp.tasktally.secrets.SecretResolver;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Resolves secrets from a Kubernetes-mounted secret base path.
 */
public class KubernetesSecretResolver implements SecretResolver {
  private final Path basePath;

  public KubernetesSecretResolver(Path basePath) {
    this.basePath = basePath;
  }

  @Override
  public byte[] resolveBinary(String secretRef) {
    try {
      return Files.readAllBytes(resolvePath(secretRef));
    } catch (IOException e) {
      throw new RuntimeException("Failed to resolve secret " + secretRef, e);
    }
  }

  @Override
  public char[] resolveChars(String secretRef) {
    return new String(resolveBinary(secretRef), StandardCharsets.UTF_8).toCharArray();
  }

  private Path resolvePath(String secretRef) {
    if (secretRef == null || !secretRef.startsWith("k8s:secret/")) {
      throw new IllegalArgumentException("Unsupported secret ref: " + secretRef);
    }
    String body = secretRef.substring("k8s:secret/".length());
    String[] parts = body.split("#", 2);
    if (parts.length != 2) {
      throw new IllegalArgumentException("Invalid secret ref: " + secretRef);
    }
    String secretName = parts[0];
    String key = parts[1];
    Path dir = basePath.resolve(secretName);
    return dir.resolve(key);
  }
}
