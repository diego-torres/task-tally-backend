package io.redhat.na.ssp.tasktally.secrets;

public interface SecretWriter {
  /**
   * Persist SSH materials to the secret store and return SecretRefs.
   */
  SshSecretRefs writeSshKey(String userId, String name, byte[] privateKeyPem,
                            byte[] publicKeyOpenSsh, char[] passphrase, byte[] knownHosts);

  /**
   * Delete previously stored secret materials by a concrete SecretRef.
   */
  void deleteByRef(String secretRef);
}
