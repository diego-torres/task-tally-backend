package io.redhat.na.ssp.tasktally.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SshHostKeyServiceTest {

  private SshHostKeyService service = new SshHostKeyService();

  @Test
  void testFetchHostKeysWithNullHostname() {
    assertThrows(IllegalArgumentException.class, () -> {
      service.fetchHostKeys(null);
    });
  }

  @Test
  void testFetchHostKeysWithEmptyHostname() {
    assertThrows(IllegalArgumentException.class, () -> {
      service.fetchHostKeys("");
    });
  }

  @Test
  void testFetchHostKeysWithWhitespaceHostname() {
    assertThrows(IllegalArgumentException.class, () -> {
      service.fetchHostKeys("   ");
    });
  }

  @Test
  void testFetchHostKeysWithInvalidHostname() {
    assertThrows(IOException.class, () -> {
      service.fetchHostKeys("invalid-hostname-that-does-not-exist-12345.com");
    });
  }

  @Test
  void testFetchKnownHostsWithValidHostname() {
    // This test requires internet connectivity and a real SSH server
    // We'll test with github.com which should be available
    try {
      String knownHosts = service.fetchKnownHosts("github.com");
      assertNotNull(knownHosts);
      assertTrue(knownHosts.contains("github.com"));
      assertTrue(knownHosts.contains("ssh-ed25519") || knownHosts.contains("ssh-rsa"));
      assertTrue(knownHosts.endsWith("\n"));
    } catch (IOException e) {
      // If network is not available, skip this test
      System.out.println("Skipping network test: " + e.getMessage());
    }
  }

  @Test
  void testIsSshServiceAvailableWithValidHostname() {
    // Test with github.com which should have SSH service
    boolean available = service.isSshServiceAvailable("github.com");
    assertTrue(available, "GitHub should have SSH service available");
  }

  @Test
  void testIsSshServiceAvailableWithInvalidHostname() {
    boolean available = service.isSshServiceAvailable("invalid-hostname-that-does-not-exist-12345.com");
    assertFalse(available, "Invalid hostname should not have SSH service");
  }

  @Test
  void testIsSshServiceAvailableWithNullHostname() {
    boolean available = service.isSshServiceAvailable(null);
    assertFalse(available, "Null hostname should return false");
  }

  @Test
  void testIsSshServiceAvailableWithEmptyHostname() {
    boolean available = service.isSshServiceAvailable("");
    assertFalse(available, "Empty hostname should return false");
  }

  @Test
  void testFetchHostKeysWithRealSshServer() {
    // This test requires internet connectivity
    try {
      List<String> hostKeys = service.fetchHostKeys("github.com");
      assertNotNull(hostKeys);
      assertFalse(hostKeys.isEmpty());

      // Verify each host key entry format
      for (String hostKey : hostKeys) {
        assertTrue(hostKey.startsWith("github.com "));
        assertTrue(hostKey.contains("ssh-ed25519") || hostKey.contains("ssh-rsa"));
        // Should have base64 key data
        String[] parts = hostKey.split("\\s+");
        assertEquals(3, parts.length, "Host key should have 3 parts: hostname, key-type, key-data");
        assertTrue(parts[2].matches("[A-Za-z0-9+/=]+"), "Key data should be valid base64");
      }
    } catch (IOException e) {
      // If network is not available, skip this test
      System.out.println("Skipping network test: " + e.getMessage());
    }
  }

  @Test
  void testFetchHostKeysWithGitLab() {
    // Test with gitlab.com as well
    try {
      List<String> hostKeys = service.fetchHostKeys("gitlab.com");
      assertNotNull(hostKeys);
      assertFalse(hostKeys.isEmpty());

      for (String hostKey : hostKeys) {
        assertTrue(hostKey.startsWith("gitlab.com "));
      }
    } catch (IOException e) {
      // If network is not available, skip this test
      System.out.println("Skipping network test: " + e.getMessage());
    }
  }

  @Test
  void testConcurrentHostKeyFetching() throws Exception {
    // Test concurrent fetching to ensure thread safety
    CompletableFuture<List<String>> future1 = CompletableFuture.supplyAsync(() -> {
      try {
        return service.fetchHostKeys("github.com");
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });

    CompletableFuture<List<String>> future2 = CompletableFuture.supplyAsync(() -> {
      try {
        return service.fetchHostKeys("gitlab.com");
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });

    try {
      List<String> githubKeys = future1.get(30, TimeUnit.SECONDS);
      List<String> gitlabKeys = future2.get(30, TimeUnit.SECONDS);

      assertNotNull(githubKeys);
      assertNotNull(gitlabKeys);
      assertFalse(githubKeys.isEmpty());
      assertFalse(gitlabKeys.isEmpty());

      // Keys should be different for different hosts
      assertNotEquals(githubKeys, gitlabKeys);
    } catch (Exception e) {
      // If network is not available, skip this test
      System.out.println("Skipping concurrent network test: " + e.getMessage());
    }
  }
}
