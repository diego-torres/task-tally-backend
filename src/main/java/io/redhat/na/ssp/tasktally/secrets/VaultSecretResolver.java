package io.redhat.na.ssp.tasktally.secrets;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class VaultSecretResolver implements SecretResolver {
  @Override
  public String resolve(String ref) {
    throw new UnsupportedOperationException("TODO");
  }
}
