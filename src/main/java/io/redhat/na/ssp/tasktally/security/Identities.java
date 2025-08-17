package io.redhat.na.ssp.tasktally.security;

import io.quarkus.security.identity.SecurityIdentity;

public final class Identities {
  private Identities() {}

  public static String userId(SecurityIdentity identity) {
    return identity.getPrincipal().getName();
  }
}
