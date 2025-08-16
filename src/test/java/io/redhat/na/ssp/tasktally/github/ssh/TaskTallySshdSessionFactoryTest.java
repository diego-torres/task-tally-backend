package io.redhat.na.ssp.tasktally.github.ssh;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.Base64;

import org.apache.sshd.common.config.keys.writer.openssh.OpenSSHKeyEncryptionContext;
import org.apache.sshd.common.config.keys.writer.openssh.OpenSSHKeyPairResourceWriter;
import org.junit.jupiter.api.Test;

class TaskTallySshdSessionFactoryTest {
  @Test
  void buildsFactoryFromInMemoryKey() throws Exception {
  // Ensure EdDSA provider is registered
  java.security.Security.addProvider(new net.i2p.crypto.eddsa.EdDSASecurityProvider());
  java.security.KeyPairGenerator gen = java.security.KeyPairGenerator.getInstance("EdDSA", "EdDSA");
  gen.initialize(new net.i2p.crypto.eddsa.spec.EdDSAGenParameterSpec("Ed25519"));
  KeyPair kp = gen.generateKeyPair();
  ByteArrayOutputStream bos = new ByteArrayOutputStream();
  new OpenSSHKeyPairResourceWriter().writePrivateKey(kp, null, new OpenSSHKeyEncryptionContext(), bos);
  byte[] priv = bos.toByteArray();
  String known = "example.com ssh-ed25519 " + Base64.getEncoder().encodeToString(kp.getPublic().getEncoded());
  java.nio.file.Path tempSshDir = java.nio.file.Files.createTempDirectory("ssh-test");
  assertNotNull(TaskTallySshdSessionFactory.create(priv, known.getBytes(StandardCharsets.UTF_8), new char[0], tempSshDir.toFile()));
  }
}
