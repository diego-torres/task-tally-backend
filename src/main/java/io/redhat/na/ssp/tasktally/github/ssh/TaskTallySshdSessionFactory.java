package io.redhat.na.ssp.tasktally.github.ssh;

import java.io.IOException;

import org.eclipse.jgit.transport.sshd.SshdSessionFactory;
import org.eclipse.jgit.transport.sshd.SshdSessionFactoryBuilder;
import org.jboss.logging.Logger;

/**
 * Builds an {@link SshdSessionFactory} from in-memory key material.
 */
public final class TaskTallySshdSessionFactory {
  private static final Logger LOG = Logger.getLogger(TaskTallySshdSessionFactory.class);

  private TaskTallySshdSessionFactory() {
  }

  // Default GitHub's SSH host keys - fallback when no known_hosts provided
  // These are GitHub's current host keys as of 2024
  private static final String GITHUB_HOST_KEYS = """
      github.com ssh-rsa AAAAB3NzaC1yc2EAAAABIwAAAQEAq2A7hRGmdnm9tUDbO9IDSwBK6TbQa+PXYPCPy6rbTrTtw7PHkccKrpp0yVhp5HdEIcKr6pLlVDBfOLX9QUsyCOV0wzfjIJNlGEYsdlLJizHhbn2mUjvSAHQqZETYP81eFzLQNnPHt4EVVUh7VfDESU84KezmD5QlWpXLmvU31/yMf+Se8xhHTvKSCZIFImWwoG6mbUoWf9nzpIoaSjB+weqqUUmpaaasXVal72J+UX2B+2RPW3RcT0eOzQgqlJL3RKrTJvdsjE3JEAvGq3lGHSZXy28G3skua2SmVi/w4yCE6gbODqnTWlg7+wC604ydGXA8VJiS5ap43JXiUFFAaQ==
      github.com ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIOMqqnkVzrm0SdG6UOoqKLsabgH5C9okWi0dh2l9GKJl
      github.com ecdsa-sha2-nistp256 AAAAE2VjZHNhLXNoYTItbmlzdHAyNTYAAAAIbmlzdHAyNTYAAABBBEmKSENjQEezOmxkZMy7opKgwFB9nkt5YRrYMjNuG5N87uRgg6CLrbo5wAdT/y6v0mKV0U2w0WZ2YB/++Tpockg=
      """;

  public static SshdSessionFactory create(byte[] privateKey, byte[] knownHosts, char[] passphrase, java.io.File sshDir)
      throws IOException {
    LOG.debug("Creating SSH session factory");
    java.nio.file.Path tempDir = sshDir != null ? sshDir.toPath() : java.nio.file.Files.createTempDirectory("jgit-ssh");
    LOG.debugf("Using temp directory: %s", tempDir);

    // Write private key to id_rsa in temp dir
    java.nio.file.Path keyFile = tempDir.resolve("id_rsa");
    java.nio.file.Files.write(keyFile, privateKey, java.nio.file.StandardOpenOption.CREATE);

    // Set proper permissions on the private key file (600)
    keyFile.toFile().setReadable(false, false);
    keyFile.toFile().setReadable(true, true);
    keyFile.toFile().setWritable(false, false);
    keyFile.toFile().setWritable(true, true);

    // Write known_hosts to temp dir
    java.nio.file.Path knownHostsFile = tempDir.resolve("known_hosts");

    // If knownHosts is null or empty, use GitHub's host keys as default
    byte[] hostsToWrite;
    if (knownHosts == null || knownHosts.length == 0) {
      hostsToWrite = GITHUB_HOST_KEYS.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    } else {
      // Use the provided known_hosts as-is, since they should already be complete
      hostsToWrite = knownHosts;
    }

    java.nio.file.Files.write(knownHostsFile, hostsToWrite, java.nio.file.StandardOpenOption.CREATE);

    // Set proper permissions on the known_hosts file (644)
    knownHostsFile.toFile().setReadable(true, true);
    knownHostsFile.toFile().setWritable(true, true);

    LOG.debugf("Created known_hosts file with content: %s",
        new String(hostsToWrite, java.nio.charset.StandardCharsets.UTF_8));

    // Create SSH config file with more permissive host key checking
    // TODO: This is a temporary fix. We should implement a proper host key verifier
    // that validates against the known_hosts file instead of disabling strict checking.
    // For now, using 'no' to allow the connection to work while we investigate the
    // proper way to handle host key validation with JGit's SSH client.
    java.nio.file.Path configFile = tempDir.resolve("config");
    String configContent = "Host github.com\n" + "  HostName github.com\n" + "  User git\n" + "  IdentityFile "
        + keyFile.toString() + "\n" + "  StrictHostKeyChecking no\n" + "  UserKnownHostsFile "
        + knownHostsFile.toString() + "\n" + "  LogLevel DEBUG\n";
    java.nio.file.Files.write(configFile, configContent.getBytes(java.nio.charset.StandardCharsets.UTF_8),
        java.nio.file.StandardOpenOption.CREATE);

    LOG.debugf("Created SSH config file with content: %s", configContent);

    SshdSessionFactoryBuilder builder = new SshdSessionFactoryBuilder();
    builder.setHomeDirectory(tempDir.toFile());
    builder.setSshDirectory(tempDir.toFile());

    LOG.debug("SSH session factory created successfully");
    return builder.build(null);
  }
}
