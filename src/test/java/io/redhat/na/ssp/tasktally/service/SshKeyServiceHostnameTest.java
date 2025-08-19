package io.redhat.na.ssp.tasktally.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.redhat.na.ssp.tasktally.api.SshKeyCreateRequest;
import io.redhat.na.ssp.tasktally.api.SshKeyGenerateRequest;
import io.redhat.na.ssp.tasktally.model.CredentialRef;
import io.redhat.na.ssp.tasktally.repo.CredentialRefRepository;
import io.redhat.na.ssp.tasktally.repo.UserPreferencesRepository;
import io.redhat.na.ssp.tasktally.secret.SecretResolver;
import io.redhat.na.ssp.tasktally.secrets.SecretWriter;

@ExtendWith(MockitoExtension.class)
class SshKeyServiceHostnameTest {

  @Mock
  private SecretWriter secretWriter;
  @Mock
  private CredentialRefRepository credentialRefRepository;
  @Mock
  private UserPreferencesRepository userPreferencesRepository;
  @Mock
  private SecretResolver secretResolver;
  @Mock
  private SshHostKeyService sshHostKeyService;

  @InjectMocks
  private SshKeyService sshKeyService;

  private static final String TEST_USER_ID = "test-user";
  private static final String TEST_HOSTNAME = "github.com";
  private static final String TEST_KNOWN_HOSTS = "github.com ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIOMqqnkVzrm0SdG6UOoqKLsabgH5C9okWi0dh2l9GKJl\n";

  @BeforeEach
  void setUp() {
    // Mock user preferences
    when(userPreferencesRepository.findByUserId(TEST_USER_ID))
        .thenReturn(java.util.Optional.of(new io.redhat.na.ssp.tasktally.model.UserPreferences()));

    // Mock credential repository to return empty (no existing credentials)
    when(credentialRefRepository.findByUserAndName(any(), anyString())).thenReturn(java.util.Optional.empty());
  }

  @Test
  void testCreateWithHostname() throws IOException {
    // Mock host key service to return known hosts
    when(sshHostKeyService.fetchKnownHosts(TEST_HOSTNAME)).thenReturn(TEST_KNOWN_HOSTS);

    // Mock secret writer
    when(secretWriter.writeSshKey(anyString(), anyString(), any(), any(), any(), any()))
        .thenReturn(new io.redhat.na.ssp.tasktally.secrets.SshSecretRefs("ref1", "ref2", null));

    SshKeyCreateRequest request = new SshKeyCreateRequest();
    request.name = "test-key";
    request.provider = "github";
    request.privateKeyPem = "-----BEGIN OPENSSH TEST KEY-----\ntest-key\n-----END OPENSSH TEST KEY-----";
    request.hostname = TEST_HOSTNAME;

    CredentialRef result = sshKeyService.create(TEST_USER_ID, request);

    assertNotNull(result);
    assertEquals("test-key", result.name);
    assertEquals("github", result.provider);

    // Verify host key service was called
    verify(sshHostKeyService).fetchKnownHosts(TEST_HOSTNAME);

    // Verify secret writer was called with the fetched known hosts
    verify(secretWriter).writeSshKey(eq(TEST_USER_ID), eq("test-key"), any(), eq(null), eq(null),
        eq(TEST_KNOWN_HOSTS.getBytes(StandardCharsets.UTF_8)));
  }

  @Test
  void testCreateWithHostnameAndKnownHosts() throws IOException {
    // Should use provided known_hosts instead of fetching from hostname
    String providedKnownHosts = "gitlab.com ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIOMqqnkVzrm0SdG6UOoqKLsabgH5C9okWi0dh2l9GKJl\n";

    // Mock secret writer
    when(secretWriter.writeSshKey(anyString(), anyString(), any(), any(), any(), any()))
        .thenReturn(new io.redhat.na.ssp.tasktally.secrets.SshSecretRefs("ref1", "ref2", null));

    SshKeyCreateRequest request = new SshKeyCreateRequest();
    request.name = "test-key";
    request.provider = "github";
    request.privateKeyPem = "-----BEGIN OPENSSH TEST KEY-----\ntest-key\n-----END OPENSSH TEST KEY-----";
    request.knownHosts = providedKnownHosts;
    request.hostname = TEST_HOSTNAME; // Should be ignored since knownHosts is provided

    CredentialRef result = sshKeyService.create(TEST_USER_ID, request);

    assertNotNull(result);
    assertEquals("test-key", result.name);

    // Verify host key service was NOT called since knownHosts was provided
    verify(sshHostKeyService, never()).fetchKnownHosts(anyString());

    // Verify secret writer was called with the provided known hosts
    verify(secretWriter).writeSshKey(eq(TEST_USER_ID), eq("test-key"), any(), eq(null), eq(null),
        eq(providedKnownHosts.getBytes(StandardCharsets.UTF_8)));
  }

  @Test
  void testCreateWithHostnameFetchFailure() throws IOException {
    // Mock host key service to throw exception
    when(sshHostKeyService.fetchKnownHosts(TEST_HOSTNAME)).thenThrow(new IOException("Connection failed"));

    SshKeyCreateRequest request = new SshKeyCreateRequest();
    request.name = "test-key";
    request.provider = "github";
    request.privateKeyPem = "-----BEGIN OPENSSH TEST KEY-----\ntest-key\n-----END OPENSSH TEST KEY-----";
    request.hostname = TEST_HOSTNAME;

    // Should throw exception
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      sshKeyService.create(TEST_USER_ID, request);
    });

    assertTrue(exception.getMessage().contains("Failed to fetch host keys from " + TEST_HOSTNAME));
  }

  @Test
  void testGenerateWithHostname() throws IOException {
    // Mock host key service to return known hosts
    when(sshHostKeyService.fetchKnownHosts(TEST_HOSTNAME)).thenReturn(TEST_KNOWN_HOSTS);

    // Mock secret writer
    when(secretWriter.writeSshKey(anyString(), anyString(), any(), any(), any(), any()))
        .thenReturn(new io.redhat.na.ssp.tasktally.secrets.SshSecretRefs("ref1", "ref2", null));

    SshKeyGenerateRequest request = new SshKeyGenerateRequest();
    request.name = "test-key";
    request.provider = "github";
    request.hostname = TEST_HOSTNAME;

    CredentialRef result = sshKeyService.generate(TEST_USER_ID, request);

    assertNotNull(result);
    assertEquals("test-key", result.name);
    assertEquals("github", result.provider);

    // Verify host key service was called
    verify(sshHostKeyService).fetchKnownHosts(TEST_HOSTNAME);

    // Verify secret writer was called with the fetched known hosts
    verify(secretWriter).writeSshKey(eq(TEST_USER_ID), eq("test-key"), any(), any(), eq(null),
        eq(TEST_KNOWN_HOSTS.getBytes(StandardCharsets.UTF_8)));
  }

  @Test
  void testGenerateWithHostnameAndKnownHosts() throws IOException {
    // Should use provided known_hosts instead of fetching from hostname
    String providedKnownHosts = "gitlab.com ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIOMqqnkVzrm0SdG6UOoqKLsabgH5C9okWi0dh2l9GKJl\n";

    // Mock secret writer
    when(secretWriter.writeSshKey(anyString(), anyString(), any(), any(), any(), any()))
        .thenReturn(new io.redhat.na.ssp.tasktally.secrets.SshSecretRefs("ref1", "ref2", null));

    SshKeyGenerateRequest request = new SshKeyGenerateRequest();
    request.name = "test-key";
    request.provider = "github";
    request.knownHosts = providedKnownHosts;
    request.hostname = TEST_HOSTNAME; // Should be ignored since knownHosts is provided

    CredentialRef result = sshKeyService.generate(TEST_USER_ID, request);

    assertNotNull(result);
    assertEquals("test-key", result.name);

    // Verify host key service was NOT called since knownHosts was provided
    verify(sshHostKeyService, never()).fetchKnownHosts(anyString());

    // Verify secret writer was called with the provided known hosts
    verify(secretWriter).writeSshKey(eq(TEST_USER_ID), eq("test-key"), any(), any(), eq(null),
        eq(providedKnownHosts.getBytes(StandardCharsets.UTF_8)));
  }

  @Test
  void testGenerateWithHostnameFetchFailure() throws IOException {
    // Mock host key service to throw exception
    when(sshHostKeyService.fetchKnownHosts(TEST_HOSTNAME)).thenThrow(new IOException("Connection failed"));

    SshKeyGenerateRequest request = new SshKeyGenerateRequest();
    request.name = "test-key";
    request.provider = "github";
    request.hostname = TEST_HOSTNAME;

    // Should throw exception
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      sshKeyService.generate(TEST_USER_ID, request);
    });

    assertTrue(exception.getMessage().contains("Failed to fetch host keys from " + TEST_HOSTNAME));
  }

  @Test
  void testCreateWithEmptyHostname() throws IOException {
    // Mock secret writer
    when(secretWriter.writeSshKey(anyString(), anyString(), any(), any(), any(), any()))
        .thenReturn(new io.redhat.na.ssp.tasktally.secrets.SshSecretRefs("ref1", "ref2", null));

    SshKeyCreateRequest request = new SshKeyCreateRequest();
    request.name = "test-key";
    request.provider = "github";
    request.privateKeyPem = "-----BEGIN OPENSSH TEST KEY-----\ntest-key\n-----END OPENSSH TEST KEY-----";
    request.hostname = ""; // Empty hostname

    CredentialRef result = sshKeyService.create(TEST_USER_ID, request);

    assertNotNull(result);
    assertEquals("test-key", result.name);

    // Verify host key service was NOT called
    verify(sshHostKeyService, never()).fetchKnownHosts(anyString());

    // Verify secret writer was called with null known hosts
    verify(secretWriter).writeSshKey(eq(TEST_USER_ID), eq("test-key"), any(), eq(null), eq(null), eq(null));
  }

  @Test
  void testCreateWithWhitespaceHostname() throws IOException {
    // Mock secret writer
    when(secretWriter.writeSshKey(anyString(), anyString(), any(), any(), any(), any()))
        .thenReturn(new io.redhat.na.ssp.tasktally.secrets.SshSecretRefs("ref1", "ref2", null));

    SshKeyCreateRequest request = new SshKeyCreateRequest();
    request.name = "test-key";
    request.provider = "github";
    request.privateKeyPem = "-----BEGIN OPENSSH TEST KEY-----\ntest-key\n-----END OPENSSH TEST KEY-----";
    request.hostname = "   "; // Whitespace hostname

    CredentialRef result = sshKeyService.create(TEST_USER_ID, request);

    assertNotNull(result);
    assertEquals("test-key", result.name);

    // Verify host key service was NOT called
    verify(sshHostKeyService, never()).fetchKnownHosts(anyString());

    // Verify secret writer was called with null known hosts
    verify(secretWriter).writeSshKey(eq(TEST_USER_ID), eq("test-key"), any(), eq(null), eq(null), eq(null));
  }
}
