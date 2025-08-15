package io.redhat.na.ssp.tasktally.secrets;

public interface SecretResolver {
  String resolve(String ref);
}
