package io.redhat.na.ssp.tasktally.github.ssh;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import org.apache.sshd.common.config.keys.writer.openssh.OpenSSHKeyPairResourceWriter;
import org.apache.sshd.common.config.keys.writer.openssh.OpenSSHKeyEncryptionContext;
import org.junit.jupiter.api.Test;

class TaskTallySshdSessionFactoryTest {
  @Test
  void buildsFactoryFromInMemoryKey() throws Exception {
    KeyPairGenerator gen = KeyPairGenerator.getInstance("Ed25519");
    KeyPair kp = gen.generateKeyPair();
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
  new OpenSSHKeyPairResourceWriter().writePrivateKey(kp, null, new OpenSSHKeyEncryptionContext(), bos);
    byte[] priv = bos.toByteArray();
    String known = "example.com ssh-ed25519 " + Base64.getEncoder().encodeToString(kp.getPublic().getEncoded());
    assertNotNull(TaskTallySshdSessionFactory.create(priv, known.getBytes(StandardCharsets.UTF_8), null));
  }
}
