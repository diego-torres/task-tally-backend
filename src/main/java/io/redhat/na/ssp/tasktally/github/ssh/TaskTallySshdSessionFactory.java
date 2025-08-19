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

  // GitHub's SSH host key (ed25519)
  private static final String GITHUB_HOST_KEY = "github.com ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIOMqqnkVzrm0SdG6UOoqKLsabgH5C9okWi0dh2l9GKJl\n";

  public static SshdSessionFactory create(byte[] privateKey, byte[] knownHosts, char[] passphrase, java.io.File sshDir)
      throws IOException {
    java.nio.file.Path tempDir = sshDir != null ? sshDir.toPath() : java.nio.file.Files.createTempDirectory("jgit-ssh");

    // Write private key to id_ed25519 (or id_rsa) in temp dir
    java.nio.file.Path keyFile = tempDir.resolve("id_ed25519");
    java.nio.file.Files.write(keyFile, privateKey, java.nio.file.StandardOpenOption.CREATE);

    // Write known_hosts to temp dir
    java.nio.file.Path knownHostsFile = tempDir.resolve("known_hosts");

    // If knownHosts is null or empty, use GitHub's host key as default
    byte[] hostsToWrite;
    if (knownHosts == null || knownHosts.length == 0) {
      hostsToWrite = GITHUB_HOST_KEY.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    } else {
      // Check if GitHub's host key is already in the provided known_hosts
      String knownHostsStr = new String(knownHosts, java.nio.charset.StandardCharsets.UTF_8);
      if (!knownHostsStr.contains("github.com ssh-ed25519")) {
        // Append GitHub's host key if not present
        hostsToWrite = (knownHostsStr + GITHUB_HOST_KEY).getBytes(java.nio.charset.StandardCharsets.UTF_8);
      } else {
        hostsToWrite = knownHosts;
      }
    }

    java.nio.file.Files.write(knownHostsFile, hostsToWrite, java.nio.file.StandardOpenOption.CREATE);

    // Optionally write passphrase to a file if needed (JGit does not support this directly)

    SshdSessionFactoryBuilder builder = new SshdSessionFactoryBuilder();
    builder.setHomeDirectory(tempDir.toFile());
    builder.setSshDirectory(tempDir.toFile());
    return builder.build(null);
  }
}
