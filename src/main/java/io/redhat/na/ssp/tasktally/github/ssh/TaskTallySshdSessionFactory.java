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

  // Default GitHub's SSH host key (ed25519) - fallback when no known_hosts provided
  private static final String GITHUB_HOST_KEY = "github.com ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIOMqqnkVzrm0SdG6UOoqKLsabgH5C9okWi0dh2l9GKJl\n";

  public static SshdSessionFactory create(byte[] privateKey, byte[] knownHosts, char[] passphrase, java.io.File sshDir)
      throws IOException {
    java.nio.file.Path tempDir = sshDir != null ? sshDir.toPath() : java.nio.file.Files.createTempDirectory("jgit-ssh");

    // Write private key to id_ed25519 (or id_rsa) in temp dir
    java.nio.file.Path keyFile = tempDir.resolve("id_ed25519");
    java.nio.file.Files.write(keyFile, privateKey, java.nio.file.StandardOpenOption.CREATE);

    // Set proper permissions on the private key file (600)
    keyFile.toFile().setReadable(false, false);
    keyFile.toFile().setReadable(true, true);
    keyFile.toFile().setWritable(false, false);
    keyFile.toFile().setWritable(true, true);

    // Write known_hosts to temp dir
    java.nio.file.Path knownHostsFile = tempDir.resolve("known_hosts");

    // If knownHosts is null or empty, use GitHub's host key as default
    byte[] hostsToWrite;
    if (knownHosts == null || knownHosts.length == 0) {
      hostsToWrite = GITHUB_HOST_KEY.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    } else {
      // Use the provided known_hosts as-is, since they should already be complete
      hostsToWrite = knownHosts;
    }

    java.nio.file.Files.write(knownHostsFile, hostsToWrite, java.nio.file.StandardOpenOption.CREATE);

    // Set proper permissions on the known_hosts file (644)
    knownHostsFile.toFile().setReadable(true, true);
    knownHostsFile.toFile().setWritable(true, true);

    // Create SSH config file to ensure proper host key checking
    java.nio.file.Path configFile = tempDir.resolve("config");
    String configContent = "Host github.com\n" + "  HostName github.com\n" + "  User git\n"
        + "  IdentityFile ~/.ssh/id_ed25519\n" + "  StrictHostKeyChecking yes\n"
        + "  UserKnownHostsFile ~/.ssh/known_hosts\n";
    java.nio.file.Files.write(configFile, configContent.getBytes(java.nio.charset.StandardCharsets.UTF_8),
        java.nio.file.StandardOpenOption.CREATE);

    SshdSessionFactoryBuilder builder = new SshdSessionFactoryBuilder();
    builder.setHomeDirectory(tempDir.toFile());
    builder.setSshDirectory(tempDir.toFile());

    return builder.build(null);
  }
}
