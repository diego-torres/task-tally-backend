package io.redhat.na.ssp.tasktally.secrets;

import java.nio.charset.StandardCharsets;

public final class SshKeyValidator {
  private static final int MAX_PRIVATE_KEY = 10 * 1024; // 10KB
  private static final int MAX_KNOWN_HOSTS = 64 * 1024; // 64KB
  private static final int MAX_PASSPHRASE = 256; // 256B

  private SshKeyValidator() {
  }

  public static void validatePrivateKey(byte[] pem) {
    if (pem == null || pem.length == 0) {
      throw new IllegalArgumentException("privateKeyPem is required");
    }
    if (pem.length > MAX_PRIVATE_KEY) {
      throw new IllegalArgumentException("privateKeyPem exceeds size limit");
    }
    String s = new String(pem, StandardCharsets.UTF_8);
    if (!s.contains("BEGIN") || !s.contains("END")) {
      throw new IllegalArgumentException("Invalid PEM format");
    }
  }

  public static void validateKnownHosts(byte[] kh) {
    if (kh != null && kh.length > MAX_KNOWN_HOSTS) {
      throw new IllegalArgumentException("knownHosts exceeds size limit");
    }
  }

  public static void validatePassphrase(char[] pp) {
    if (pp != null && pp.length > MAX_PASSPHRASE) {
      throw new IllegalArgumentException("passphrase exceeds size limit");
    }
  }
}
