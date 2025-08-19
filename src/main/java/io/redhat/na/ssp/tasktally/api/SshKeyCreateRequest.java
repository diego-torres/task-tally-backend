package io.redhat.na.ssp.tasktally.api;

public class SshKeyCreateRequest {
  public String name;
  public String provider;
  public String privateKeyPem;
  public String knownHosts;
  public String passphrase;
  /** Optional: hostname to automatically fetch SSH host keys from (e.g., "github.com") */
  public String hostname;
}
