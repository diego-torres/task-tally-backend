package io.redhat.na.ssp.tasktally.secret;

/**
 * Resolves opaque references to secret values.
 */
public interface SecretResolver {
  /** Resolve the reference to a UTF-8 string. */
  String resolve(String ref);

  /** Resolve the reference to raw bytes. */
  byte[] resolveBytes(String ref);
}
