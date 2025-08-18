package io.redhat.na.ssp.tasktally.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.redhat.na.ssp.tasktally.api.SshKeyCreateRequest;
import io.redhat.na.ssp.tasktally.api.SshKeyGenerateRequest;
import io.redhat.na.ssp.tasktally.model.CredentialRef;
import io.redhat.na.ssp.tasktally.secret.SecretResolver;
import io.redhat.na.ssp.tasktally.secrets.SecretWriter;
import io.redhat.na.ssp.tasktally.secrets.SshSecretRefs;
import jakarta.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class SshKeyServiceTest {
  @org.junit.jupiter.api.AfterEach
  public void tearDown() {
    for (CredentialRef cred : store.list("u1")) {
      store.remove("u1", cred.name);
    }
  }

  @Inject
  SshKeyService service;
  @Inject
  CredentialStore store;
  @InjectMock
  SecretWriter writer;
  @InjectMock
  SecretResolver resolver;

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

  @Test
  public void generateWithoutPassphrase() {
    AtomicReference<byte[]> priv = new AtomicReference<>();
    AtomicReference<byte[]> pub = new AtomicReference<>();
    when(writer.writeSshKey(any(), any(), any(), any(), any(), any())).thenAnswer(inv -> {
      priv.set(inv.getArgument(2));
      pub.set(inv.getArgument(3));
      return new SshSecretRefs("k8s:secret/tasktally-ssh-u1-unique_no_passphrase#id_ed25519", null, null);
    });
    when(resolver.resolveBytes(any())).thenAnswer(inv -> pub.get());

    SshKeyGenerateRequest req = new SshKeyGenerateRequest();
    req.name = "unique_no_passphrase";
    req.provider = "github";
    CredentialRef cred = service.generate("u1", req);
    assertNotNull(cred);
    String pk = service.getPublicKey("u1", "unique_no_passphrase");
    assertTrue(pk.startsWith("ssh-ed25519 "));
    String privStr = new String(priv.get(), StandardCharsets.UTF_8);
    assertTrue(privStr.contains("openssh-key-v1"));
  }

  @Test
  public void generateWithPassphraseEncrypted() {
    AtomicReference<byte[]> priv = new AtomicReference<>();
    when(writer.writeSshKey(any(), any(), any(), any(), any(), any())).thenAnswer(inv -> {
      priv.set(inv.getArgument(2));
      return new SshSecretRefs("ref", null, null);
    });
    SshKeyGenerateRequest req = new SshKeyGenerateRequest();
    req.name = "enc";
    req.provider = "github";
    req.passphrase = "test";
    service.generate("u1", req);
    String privStr = new String(priv.get(), StandardCharsets.UTF_8);
    assertTrue(privStr.contains("aes256-ctr"));
  }

  @Test
  public void knownHostsEndsWithNewline() {
    AtomicReference<byte[]> kh = new AtomicReference<>();
    when(writer.writeSshKey(any(), any(), any(), any(), any(), any())).thenAnswer(inv -> {
      kh.set(inv.getArgument(5));
      return new SshSecretRefs("ref", "khref", null);
    });
    SshKeyGenerateRequest req = new SshKeyGenerateRequest();
    req.name = "kh";
    req.provider = "github";
    req.knownHosts = "github.com ssh-ed25519 AAAA";
    service.generate("u1", req);
    assertNotNull(kh.get());
    assertEquals('\n', kh.get()[kh.get().length - 1]);
  }
}
