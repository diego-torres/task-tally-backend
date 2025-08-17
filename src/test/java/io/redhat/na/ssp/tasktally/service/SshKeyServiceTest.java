package io.redhat.na.ssp.tasktally.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.redhat.na.ssp.tasktally.api.SshKeyCreateRequest;
import io.redhat.na.ssp.tasktally.model.CredentialRef;
import io.redhat.na.ssp.tasktally.secrets.SecretWriter;
import io.redhat.na.ssp.tasktally.secrets.SshSecretRefs;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class SshKeyServiceTest {

  @Inject
  SshKeyService service;
  @Inject
  CredentialStore store;
  @InjectMock
  SecretWriter writer;

  @Test
  public void createStoresCredential() {
    when(writer.writeSshKey(any(), any(), any(), any(), any(), any()))
        .thenReturn(new SshSecretRefs("ref1", "kh", null));
    SshKeyCreateRequest req = new SshKeyCreateRequest();
    req.name = "k1";
    req.provider = "github";
    req.privateKeyPem = "-----BEGIN OPENSSH PRIVATE KEY-----\nAAA\n-----END OPENSSH PRIVATE KEY-----\n";
    req.knownHosts = "github.com ssh-ed25519 AAAA\n";
    CredentialRef cred = service.create("u1", req);
    assertEquals("ref1", cred.secretRef);
    assertEquals(1, store.list("u1").size());
    verify(writer).writeSshKey(any(), any(), any(), any(), any(), any());
  }
}
