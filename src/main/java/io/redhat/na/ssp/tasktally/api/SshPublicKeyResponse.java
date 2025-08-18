package io.redhat.na.ssp.tasktally.api;

public class SshPublicKeyResponse {
  /** Full OpenSSH public key line, e.g., "ssh-ed25519 AAAA... comment". */
  public String publicKey;

  /** SHA256 fingerprint (OpenSSH style, Base64). */
  public String fingerprintSha256;

  /** Credential name for convenience. */
  public String name;

  /** Provider ("github" or "gitlab"). */
  public String provider;
}
