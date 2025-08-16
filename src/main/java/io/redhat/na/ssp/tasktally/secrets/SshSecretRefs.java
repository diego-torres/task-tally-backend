package io.redhat.na.ssp.tasktally.secrets;

public record SshSecretRefs(String privateKeyRef, String knownHostsRef, String passphraseRef) {}
