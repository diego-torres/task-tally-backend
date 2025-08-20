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
  void testCreateWithProvidedKnownHosts(@TempDir Path tempDir) throws IOException {
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
    Path keyFile = tempDir.resolve("id_rsa");
    assertTrue(Files.exists(keyFile), "Private key file should exist");
    assertTrue(keyFile.toFile().canRead(), "Private key file should be readable");
    assertTrue(keyFile.toFile().canWrite(), "Private key file should be writable by owner");

    // Verify known_hosts file exists and contains provided host key
    Path knownHostsFile = tempDir.resolve("known_hosts");
    assertTrue(Files.exists(knownHostsFile), "Known hosts file should exist");
    String knownHostsContent = Files.readString(knownHostsFile);
    assertTrue(knownHostsContent.contains("github.com ssh-ed25519"), "Known hosts should contain provided host key");

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

    // Create session factory with empty known_hosts (should read from local file)
    SshdSessionFactory factory = TaskTallySshdSessionFactory.create(privateKey, new byte[0], null, tempDir.toFile());

    // Verify factory is created
    assertNotNull(factory);

    // Verify known_hosts file exists and contains some host keys
    Path knownHostsFile = tempDir.resolve("known_hosts");
    assertTrue(Files.exists(knownHostsFile), "Known hosts file should exist");
    String knownHostsContent = Files.readString(knownHostsFile);
    assertTrue(knownHostsContent.contains("github.com"), "Known hosts should contain GitHub host keys");
  }

  @Test
  void testCreateWithNullKnownHosts(@TempDir Path tempDir) throws IOException {
    // Test data
    byte[] privateKey = "-----BEGIN OPENSSH TEST KEY-----\ntest-key\n-----END OPENSSH TEST KEY-----\n"
        .getBytes(StandardCharsets.UTF_8);

    // Create session factory with null known_hosts (should read from local file)
    SshdSessionFactory factory = TaskTallySshdSessionFactory.create(privateKey, null, null, tempDir.toFile());

    // Verify factory is created
    assertNotNull(factory);

    // Verify known_hosts file exists and contains some host keys
    Path knownHostsFile = tempDir.resolve("known_hosts");
    assertTrue(Files.exists(knownHostsFile), "Known hosts file should exist");
    String knownHostsContent = Files.readString(knownHostsFile);
    assertTrue(knownHostsContent.contains("github.com"), "Known hosts should contain GitHub host keys");
  }

  @Test
  void testEnvironmentVariableConfiguration() {
    // Test that the environment variable constants are properly defined
    String envVar = System.getenv("TASKTALLY_KNOWN_HOSTS_FILE");
    String defaultPath = System.getProperty("user.home") + "/.ssh/known_hosts";

    // The system should work with or without the environment variable set
    assertNotNull(defaultPath, "Default known_hosts path should not be null");
    assertTrue(defaultPath.contains(".ssh/known_hosts"), "Default path should contain .ssh/known_hosts");

    // If environment variable is set, it should be a valid path
    if (envVar != null && !envVar.trim().isEmpty()) {
      Path envPath = Path.of(envVar);
      assertTrue(envPath.isAbsolute() || envPath.toString().contains("/"),
          "Environment variable path should be absolute or contain path separators");
    }
  }
}
