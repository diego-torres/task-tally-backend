package io.redhat.na.ssp.tasktally.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.redhat.na.ssp.tasktally.api.SshKeyCreateRequest;
import io.redhat.na.ssp.tasktally.api.SshKeyGenerateRequest;
import io.redhat.na.ssp.tasktally.model.CredentialRef;
import io.redhat.na.ssp.tasktally.model.UserPreferences;
import io.redhat.na.ssp.tasktally.repo.CredentialRefRepository;
import io.redhat.na.ssp.tasktally.repo.UserPreferencesRepository;
import io.redhat.na.ssp.tasktally.secret.SecretResolver;
import io.redhat.na.ssp.tasktally.secrets.SecretWriter;
import io.redhat.na.ssp.tasktally.secrets.SshSecretRefs;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@QuarkusTest
public class SshKeyServiceTest {
  private static final String TEST_USER_ID = "u1";

  @Inject
  SshKeyService service;
  @Inject
  CredentialRefRepository credentialRefRepository;
  @Inject
  UserPreferencesRepository userPreferencesRepository;
  @InjectMock
  SecretWriter writer;
  @InjectMock
  SecretResolver resolver;

  @BeforeEach
  @Transactional
  public void setUp() {
    // Clean up any existing test data
    credentialRefRepository.find("userPreferences.userId", TEST_USER_ID).list()
        .forEach(credentialRefRepository::delete);
    userPreferencesRepository.findByUserId(TEST_USER_ID).ifPresent(userPreferencesRepository::delete);

    // Create test user preferences
    userPreferencesRepository.findByUserId(TEST_USER_ID).orElseGet(() -> {
      UserPreferences userPrefs = new UserPreferences();
      userPrefs.userId = TEST_USER_ID;
      userPrefs.ui = new java.util.HashMap<>();
      userPreferencesRepository.persist(userPrefs);
      return userPrefs;
    });
  }

  @org.junit.jupiter.api.AfterEach
  @Transactional
  public void tearDown() {
    // Clean up test data
    credentialRefRepository.find("userPreferences.userId", TEST_USER_ID).list()
        .forEach(credentialRefRepository::delete);
    userPreferencesRepository.findByUserId(TEST_USER_ID).ifPresent(userPreferencesRepository::delete);
  }

  @Test
  @Transactional
  public void createStoresCredential() {
    when(writer.writeSshKey(any(), any(), any(), any(), any(), any()))
        .thenReturn(new SshSecretRefs("ref1", "kh", null));
    SshKeyCreateRequest req = new SshKeyCreateRequest();
    req.name = "k1";
    req.provider = "github";
    req.privateKeyPem = "-----BEGIN OPENSSH PRIVATE KEY-----\nAAA\n-----END OPENSSH PRIVATE KEY-----\n";
    req.knownHosts = "github.com ssh-ed25519 AAAA\n";
    CredentialRef cred = service.create(TEST_USER_ID, req);
    assertEquals("ref1", cred.secretRef);
    assertEquals(1, service.list(TEST_USER_ID).size());
    verify(writer).writeSshKey(any(), any(), any(), any(), any(), any());
  }

  @Test
  @Transactional
  public void generateWithoutPassphrase() {
    AtomicReference<byte[]> priv = new AtomicReference<>();
    AtomicReference<byte[]> pub = new AtomicReference<>();
    when(writer.writeSshKey(any(), any(), any(), any(), any(), any())).thenAnswer(inv -> {
      priv.set(inv.getArgument(2));
      pub.set(inv.getArgument(3));
      return new SshSecretRefs("k8s:secret/tasktally-ssh-u1-unique_no_passphrase#id_ed25519", null, null);
    });
    when(resolver.resolveBytes(any())).thenAnswer(inv -> pub.get());

    SshKeyGenerateRequest req = new SshKeyGenerateRequest();
    req.name = "unique_no_passphrase";
    req.provider = "github";
    CredentialRef cred = service.generate(TEST_USER_ID, req);
    assertNotNull(cred);
    String pk = service.getPublicKey(TEST_USER_ID, "unique_no_passphrase");
    assertTrue(pk.startsWith("ssh-ed25519 "));
    String privStr = new String(priv.get(), StandardCharsets.UTF_8);
    assertTrue(privStr.startsWith("-----BEGIN PRIVATE KEY-----"));
  }

  @Test
  @Transactional
  public void knownHostsEndsWithNewline() {
    AtomicReference<byte[]> kh = new AtomicReference<>();
    when(writer.writeSshKey(any(), any(), any(), any(), any(), any())).thenAnswer(inv -> {
      kh.set(inv.getArgument(5));
      return new SshSecretRefs("ref", "khref", null);
    });
    SshKeyGenerateRequest req = new SshKeyGenerateRequest();
    req.name = "kh";
    req.provider = "github";
    req.knownHosts = "github.com ssh-ed25519 AAAA";
    service.generate(TEST_USER_ID, req);
    assertNotNull(kh.get());
    assertEquals('\n', kh.get()[kh.get().length - 1]);
  }

  @Test
  @Transactional
  public void listReturnsEmptyForNewUser() {
    assertEquals(0, service.list(TEST_USER_ID).size());
  }

  @Test
  @Transactional
  public void deleteRemovesCredential() {
    when(writer.writeSshKey(any(), any(), any(), any(), any(), any()))
        .thenReturn(new SshSecretRefs("ref1", "kh", null));

    SshKeyCreateRequest req = new SshKeyCreateRequest();
    req.name = "k1";
    req.provider = "github";
    req.privateKeyPem = "-----BEGIN OPENSSH PRIVATE KEY-----\nAAA\n-----END OPENSSH PRIVATE KEY-----\n";

    service.create(TEST_USER_ID, req);
    assertEquals(1, service.list(TEST_USER_ID).size());

    service.delete(TEST_USER_ID, "k1");
    assertEquals(0, service.list(TEST_USER_ID).size());
  }

  @Test
  @Transactional
  public void publicKeyFormatIsCorrect() {
    AtomicReference<byte[]> priv = new AtomicReference<>();
    AtomicReference<byte[]> pub = new AtomicReference<>();
    when(writer.writeSshKey(any(), any(), any(), any(), any(), any())).thenAnswer(inv -> {
      priv.set(inv.getArgument(2));
      pub.set(inv.getArgument(3));
      return new SshSecretRefs("k8s:secret/tasktally-ssh-u1-format-test#id_ed25519", null, null);
    });
    when(resolver.resolveBytes(any())).thenAnswer(inv -> pub.get());

    SshKeyGenerateRequest req = new SshKeyGenerateRequest();
    req.name = "format-test";
    req.provider = "github";
    req.comment = "test@example.com";

    CredentialRef cred = service.generate(TEST_USER_ID, req);
    assertNotNull(cred);

    String pk = service.getPublicKey(TEST_USER_ID, "format-test");

    // Verify the format matches OpenSSH public key format
    assertTrue(pk.startsWith("ssh-ed25519 "), "Public key should start with 'ssh-ed25519 '");
    assertTrue(pk.contains(" "), "Public key should contain spaces separating parts");

    // Split the key into parts
    String[] parts = pk.split(" ");
    assertEquals(3, parts.length, "Public key should have exactly 3 parts: type, key, comment");
    assertEquals("ssh-ed25519", parts[0], "First part should be 'ssh-ed25519'");
    assertTrue(parts[1].length() > 0, "Key part should not be empty");
    assertEquals("test@example.com", parts[2], "Comment should match the provided comment");

    // Verify the key part is valid base64
    try {
      java.util.Base64.getDecoder().decode(parts[1]);
    } catch (IllegalArgumentException e) {
      fail("Key part should be valid base64: " + e.getMessage());
    }

    // Verify no extra whitespace or newlines
    assertEquals(pk.trim(), pk, "Public key should not have leading/trailing whitespace");
    assertFalse(pk.contains("\n"), "Public key should not contain newlines");
    assertFalse(pk.contains("\r"), "Public key should not contain carriage returns");
  }
}
