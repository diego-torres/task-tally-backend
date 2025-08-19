package io.redhat.na.ssp.tasktally.api;

public class SshKeyGenerateRequest {
  /** Friendly name, must be unique for the user (e.g., "default"). */
  public String name;

  /** "github" or "gitlab" (required, validated like existing create). */
  public String provider;

  /** Optional: comment appended to the public key, default "task-tally@{userId}". */
  public String comment;

  /** Optional: plain-text known_hosts content to pin hosts (e.g., from ssh-keyscan). */
  public String knownHosts;

  /** Optional: passphrase for private key; if absent, generate unencrypted. */
  public String passphrase;

  /** Optional: hostname to automatically fetch SSH host keys from (e.g., "github.com") */
  public String hostname;
}
