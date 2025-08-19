package io.redhat.na.ssp.tasktally.github.ssh;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.jgit.transport.sshd.SshdSessionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class TaskTallySshdSessionFactoryTest {

  @Test
  void testCreateWithGitHubHostKey(@TempDir Path tempDir) throws IOException {
    // Test data
    byte[] privateKey = "-----BEGIN OPENSSH TEST KEY-----\ntest-key\n-----END OPENSSH TEST KEY-----\n"
        .getBytes(StandardCharsets.UTF_8);
    byte[] knownHosts = "github.com ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIOMqqnkVzrm0SdG6UOoqKLsabgH5C9okWi0dh2l9GKJl\n"
        .getBytes(StandardCharsets.UTF_8);

    // Create session factory
    SshdSessionFactory factory = TaskTallySshdSessionFactory.create(privateKey, knownHosts, null, tempDir.toFile());

    // Verify factory is created
    assertNotNull(factory);

    // Verify private key file exists and has correct permissions
    Path keyFile = tempDir.resolve("id_ed25519");
    assertTrue(Files.exists(keyFile), "Private key file should exist");
    assertTrue(keyFile.toFile().canRead(), "Private key file should be readable");
    assertTrue(keyFile.toFile().canWrite(), "Private key file should be writable by owner");

    // Verify known_hosts file exists and contains GitHub host key
    Path knownHostsFile = tempDir.resolve("known_hosts");
    assertTrue(Files.exists(knownHostsFile), "Known hosts file should exist");
    String knownHostsContent = Files.readString(knownHostsFile);
    assertTrue(knownHostsContent.contains("github.com ssh-ed25519"), "Known hosts should contain GitHub host key");

    // Verify config file exists
    Path configFile = tempDir.resolve("config");
    assertTrue(Files.exists(configFile), "SSH config file should exist");
    String configContent = Files.readString(configFile);
    assertTrue(configContent.contains("Host github.com"), "Config should contain GitHub host configuration");
    assertTrue(configContent.contains("StrictHostKeyChecking yes"), "Config should enable strict host key checking");
  }

  @Test
  void testCreateWithEmptyKnownHosts(@TempDir Path tempDir) throws IOException {
    // Test data
    byte[] privateKey = "-----BEGIN OPENSSH TEST KEY-----\ntest-key\n-----END OPENSSH TEST KEY-----\n"
        .getBytes(StandardCharsets.UTF_8);

    // Create session factory with empty known_hosts (should use default GitHub host key)
    SshdSessionFactory factory = TaskTallySshdSessionFactory.create(privateKey, new byte[0], null, tempDir.toFile());

    // Verify factory is created
    assertNotNull(factory);

    // Verify known_hosts file contains the default GitHub host key
    Path knownHostsFile = tempDir.resolve("known_hosts");
    assertTrue(Files.exists(knownHostsFile), "Known hosts file should exist");
    String knownHostsContent = Files.readString(knownHostsFile);
    assertTrue(knownHostsContent.contains("github.com ssh-ed25519"),
        "Known hosts should contain default GitHub host key");
  }

  @Test
  void testCreateWithNullKnownHosts(@TempDir Path tempDir) throws IOException {
    // Test data
    byte[] privateKey = "-----BEGIN OPENSSH TEST KEY-----\ntest-key\n-----END OPENSSH TEST KEY-----\n"
        .getBytes(StandardCharsets.UTF_8);

    // Create session factory with null known_hosts (should use default GitHub host key)
    SshdSessionFactory factory = TaskTallySshdSessionFactory.create(privateKey, null, null, tempDir.toFile());

    // Verify factory is created
    assertNotNull(factory);

    // Verify known_hosts file contains the default GitHub host key
    Path knownHostsFile = tempDir.resolve("known_hosts");
    assertTrue(Files.exists(knownHostsFile), "Known hosts file should exist");
    String knownHostsContent = Files.readString(knownHostsFile);
    assertTrue(knownHostsContent.contains("github.com ssh-ed25519"),
        "Known hosts should contain default GitHub host key");
  }
}
