package io.redhat.na.ssp.tasktally.model;

/**
 * Reference to a Git credential. Secret material is stored externally and referenced
 * via SecretRefs.
 */
public class CredentialRef {
  private String name;
  private String provider;
  private String scope;
  private String secretRef;
  private String knownHostsRef;
  private String passphraseRef;

  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public String getProvider() { return provider; }
  public void setProvider(String provider) { this.provider = provider; }
  public String getScope() { return scope; }
  public void setScope(String scope) { this.scope = scope; }
  public String getSecretRef() { return secretRef; }
  public void setSecretRef(String secretRef) { this.secretRef = secretRef; }
  public String getKnownHostsRef() { return knownHostsRef; }
  public void setKnownHostsRef(String knownHostsRef) { this.knownHostsRef = knownHostsRef; }
  public String getPassphraseRef() { return passphraseRef; }
  public void setPassphraseRef(String passphraseRef) { this.passphraseRef = passphraseRef; }
}
