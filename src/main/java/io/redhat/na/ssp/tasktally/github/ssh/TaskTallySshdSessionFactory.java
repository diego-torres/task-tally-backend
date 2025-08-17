package io.redhat.na.ssp.tasktally.github.ssh;

import java.io.IOException;

import org.eclipse.jgit.transport.sshd.SshdSessionFactory;
import org.eclipse.jgit.transport.sshd.SshdSessionFactoryBuilder;

/**
 * Builds an {@link SshdSessionFactory} from in-memory key material.
 */
public final class TaskTallySshdSessionFactory {
  private TaskTallySshdSessionFactory() {
  }

  public static SshdSessionFactory create(byte[] privateKey, byte[] knownHosts, char[] passphrase, java.io.File sshDir)
      throws IOException {
    java.nio.file.Path tempDir = sshDir != null ? sshDir.toPath() : java.nio.file.Files.createTempDirectory("jgit-ssh");

    // Write private key to id_ed25519 (or id_rsa) in temp dir
    java.nio.file.Path keyFile = tempDir.resolve("id_ed25519");
    java.nio.file.Files.write(keyFile, privateKey, java.nio.file.StandardOpenOption.CREATE);

    // Write known_hosts to temp dir
    java.nio.file.Path knownHostsFile = tempDir.resolve("known_hosts");
    java.nio.file.Files.write(knownHostsFile, knownHosts, java.nio.file.StandardOpenOption.CREATE);

    // Optionally write passphrase to a file if needed (JGit does not support this directly)

    SshdSessionFactoryBuilder builder = new SshdSessionFactoryBuilder();
    builder.setHomeDirectory(tempDir.toFile());
    builder.setSshDirectory(tempDir.toFile());
    return builder.build(null);
  }
}
