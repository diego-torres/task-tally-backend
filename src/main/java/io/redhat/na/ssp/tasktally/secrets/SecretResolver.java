package io.redhat.na.ssp.tasktally.secrets;

public interface SecretResolver {
  byte[] resolveBinary(String secretRef);
  char[] resolveChars(String secretRef);
}
