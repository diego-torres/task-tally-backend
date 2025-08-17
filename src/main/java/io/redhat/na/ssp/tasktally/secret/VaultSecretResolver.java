package io.redhat.na.ssp.tasktally.secret;

/** Stub resolver for Vault secrets. */
public class VaultSecretResolver implements SecretResolver {
  @Override
  public String resolve(String ref) {
    throw new UnsupportedOperationException("Vault resolver not implemented");
  }

  @Override
  public byte[] resolveBytes(String ref) {
    throw new UnsupportedOperationException("Vault resolver not implemented");
  }
}
