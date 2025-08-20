package io.redhat.na.ssp.tasktally.secrets;

import static org.junit.jupiter.api.Assertions.*;

import io.redhat.na.ssp.tasktally.secrets.k8s.KubernetesSecretResolver;
import io.redhat.na.ssp.tasktally.secrets.k8s.KubernetesSecretWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

public class KubernetesSecretWriterTest {
  @Test
  void writesSecretAndReturnsRefs() throws Exception {
    Path base = Files.createTempDirectory("secrets");
    KubernetesSecretWriter writer = new KubernetesSecretWriter(base.toString());
    byte[] priv = "-----BEGIN TEST KEY-----\nkey\n-----END TEST KEY-----\n".getBytes(StandardCharsets.UTF_8);
    byte[] pub = "ssh-ed25519 AAAA".getBytes(StandardCharsets.UTF_8);
    byte[] kh = "github.com ssh-ed25519 AAAA".getBytes(StandardCharsets.UTF_8);
    SshSecretRefs refs = writer.writeSshKey("u1", "My Key", priv, pub, null, kh);

    assertNotNull(refs.privateKeyRef());
    Path privPath = base.resolve("tasktally-ssh-u1-my-key").resolve("id_rsa");
    assertTrue(Files.exists(privPath));

    KubernetesSecretResolver resolver = new KubernetesSecretResolver(base);
    byte[] resolved = resolver.resolveBinary(refs.privateKeyRef());
    assertArrayEquals(priv, resolved);
    assertEquals("k8s:secret/tasktally-ssh-u1-my-key#id_rsa", refs.privateKeyRef());
    assertEquals("k8s:secret/tasktally-ssh-u1-my-key#known_hosts", refs.knownHostsRef());
    String pubFile = Files.readString(base.resolve("tasktally-ssh-u1-my-key").resolve("id_rsa.pub"));
    assertTrue(pubFile.endsWith("\n"));
    String khFile = Files.readString(base.resolve("tasktally-ssh-u1-my-key").resolve("known_hosts"));
    assertTrue(khFile.endsWith("\n"));
  }
}
