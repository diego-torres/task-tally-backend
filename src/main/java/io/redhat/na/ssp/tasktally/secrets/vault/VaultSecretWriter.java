package io.redhat.na.ssp.tasktally.secrets.vault;

import io.redhat.na.ssp.tasktally.secrets.SecretWriter;
import io.redhat.na.ssp.tasktally.secrets.SshSecretRefs;

public class VaultSecretWriter implements SecretWriter {
  @Override
  public SshSecretRefs writeSshKey(String userId, String name, byte[] privateKeyPem,
                                   byte[] publicKeyOpenSsh, char[] passphrase, byte[] knownHosts) {
    throw new UnsupportedOperationException("TODO");
  }

  @Override
  public void deleteByRef(String secretRef) {
    throw new UnsupportedOperationException("TODO");
  }
}
