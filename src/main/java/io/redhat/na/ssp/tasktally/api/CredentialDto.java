package io.redhat.na.ssp.tasktally.api;

import java.time.Instant;

public class CredentialDto {
  public String name;
  public String provider;
  public String scope;
  public String secretRef;
  public String knownHostsRef;
  public String passphraseRef;
  public Instant createdAt;
}
