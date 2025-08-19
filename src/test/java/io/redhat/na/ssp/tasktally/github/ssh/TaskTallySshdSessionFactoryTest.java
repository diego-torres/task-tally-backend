package io.redhat.na.ssp.tasktally.github.ssh;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;

import org.apache.sshd.common.config.keys.PublicKeyEntry;
import org.apache.sshd.common.config.keys.writer.openssh.OpenSSHKeyPairResourceWriter;
import org.junit.jupiter.api.Test;

class TaskTallySshdSessionFactoryTest {
  @Test
  void buildsFactoryFromInMemoryKey() throws Exception {
    KeyPairGenerator kpg = KeyPairGenerator.getInstance("Ed25519");
    KeyPair kp = kpg.generateKeyPair();

    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    new OpenSSHKeyPairResourceWriter().writePrivateKey(kp, null, null, bos);
    byte[] priv = bos.toByteArray();

    String known = "example.com " + PublicKeyEntry.toString(kp.getPublic());
    java.nio.file.Path tempSshDir = java.nio.file.Files.createTempDirectory("ssh-test");

    assertNotNull(TaskTallySshdSessionFactory.create(priv, known.getBytes(StandardCharsets.UTF_8), new char[0],
        tempSshDir.toFile()));
  }
}
