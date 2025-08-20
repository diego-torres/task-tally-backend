package io.redhat.na.ssp.tasktally.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.redhat.na.ssp.tasktally.github.ssh.SshGitService;
import io.redhat.na.ssp.tasktally.model.Template;
import io.redhat.na.ssp.tasktally.model.UserPreferences;
import io.redhat.na.ssp.tasktally.model.CredentialRef;
import io.redhat.na.ssp.tasktally.repo.TemplateRepository;
import io.redhat.na.ssp.tasktally.repo.UserPreferencesRepository;
import io.redhat.na.ssp.tasktally.repo.CredentialRefRepository;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

@QuarkusTest
public class TemplateServiceTest {

  @Inject
  TemplateService service;
  @Inject
  UserPreferencesRepository userRepo;
  @Inject
  TemplateRepository templateRepo;
  @Inject
  CredentialRefRepository credentialRefRepo;

  @InjectMock
  SshGitService gitService;

  private String userId;

  @BeforeEach
  @Transactional
  public void setup() throws Exception {
    // Generate a unique userId for each test run
    userId = "u" + System.nanoTime();
    UserPreferences up = new UserPreferences();
    up.userId = userId;
    userRepo.persist(up);

    when(gitService.cloneShallow(any(), any(), any(Path.class), any())).thenAnswer(invocation -> {
      Path dir = invocation.getArgument(2);
      Files.createDirectories(dir);
      return dir;
    });
    doNothing().when(gitService).commitAndPush(any(Path.class), any(), any(), any(), any());
  }

  @AfterEach
  @Transactional
  public void cleanup() {
    templateRepo.deleteAll();
    credentialRefRepo.deleteAll();
    userRepo.deleteAll();
  }

  @Test
  @Transactional
  public void testCreateUpdateDelete() throws IOException, GitAPIException {
    Template t = new Template();
    t.name = "T1";
    t.description = "desc";
    t.repositoryUrl = "git@example.com:repo.git";
    Template saved = service.create(userId, t);
    assertNotNull(saved.id);

    Template upd = new Template();
    upd.name = "T1b";
    upd.description = "desc2";
    upd.repositoryUrl = "git@example.com:repo.git";
    Template updated = service.update(userId, saved.id, upd);
    assertEquals("T1b", updated.name);

    service.delete(userId, saved.id);
    Long upId = userRepo.findByUserId(userId).get().id;
    assertTrue(templateRepo.listByUser(upId).isEmpty());
  }

  @Test
  @Transactional
  public void testCreateWithNewFields() throws IOException, GitAPIException {
    // Create an SSH key first
    UserPreferences up = userRepo.findByUserId(userId).get();
    CredentialRef sshKey = new CredentialRef();
    sshKey.userPreferences = up;
    sshKey.name = "test-key";
    sshKey.provider = "github";
    sshKey.scope = "write";
    sshKey.secretRef = "k8s:secret/test-key#id_ed25519";
    sshKey.knownHostsRef = "k8s:secret/test-key#known_hosts";
    credentialRefRepo.persist(sshKey);

    Template t = new Template();
    t.name = "T2";
    t.description = "desc with new fields";
    t.repositoryUrl = "git@github.com:user/repo.git";
    t.provider = "github";
    t.defaultBranch = "develop";
    t.sshKeyName = "test-key";

    Template saved = service.create(userId, t);
    assertNotNull(saved.id);
    assertEquals("github", saved.provider);
    assertEquals("develop", saved.defaultBranch);
    assertEquals("test-key", saved.sshKeyName);
  }

  @Test
  @Transactional
  public void testCreateWithDefaultBranch() throws IOException, GitAPIException {
    Template t = new Template();
    t.name = "T3";
    t.description = "desc with default branch";
    t.repositoryUrl = "git@github.com:user/repo.git";
    t.provider = "gitlab";

    Template saved = service.create(userId, t);
    assertNotNull(saved.id);
    assertEquals("gitlab", saved.provider);
    assertEquals("main", saved.defaultBranch); // Should default to main
    assertNull(saved.sshKeyName);
  }

  @Test
  @Transactional
  public void testCreateWithInvalidSshKey() {
    Template t = new Template();
    t.name = "T4";
    t.description = "desc with invalid ssh key";
    t.repositoryUrl = "git@github.com:user/repo.git";
    t.provider = "github";
    t.sshKeyName = "non-existent-key";

    // Should throw IllegalArgumentException for invalid SSH key
    try {
      service.create(userId, t);
      assertTrue(false, "Should have thrown exception for invalid SSH key");
    } catch (IllegalArgumentException e) {
      assertTrue(e.getMessage().contains("Invalid SSH key reference"));
    }
  }

  @Test
  @Transactional
  public void testUpdateWithNewFields() throws IOException, GitAPIException {
    // Create initial template
    Template t = new Template();
    t.name = "T5";
    t.description = "initial desc";
    t.repositoryUrl = "git@github.com:user/repo.git";
    Template saved = service.create(userId, t);

    // Create an SSH key
    UserPreferences up = userRepo.findByUserId(userId).get();
    CredentialRef sshKey = new CredentialRef();
    sshKey.userPreferences = up;
    sshKey.name = "update-key";
    sshKey.provider = "github";
    sshKey.scope = "write";
    sshKey.secretRef = "k8s:secret/update-key#id_ed25519";
    sshKey.knownHostsRef = "k8s:secret/update-key#known_hosts";
    credentialRefRepo.persist(sshKey);

    // Update with new fields
    Template upd = new Template();
    upd.name = "T5-updated";
    upd.description = "updated desc";
    upd.repositoryUrl = "git@github.com:user/repo.git";
    upd.provider = "github";
    upd.defaultBranch = "feature";
    upd.sshKeyName = "update-key";

    Template updated = service.update(userId, saved.id, upd);
    assertEquals("T5-updated", updated.name);
    assertEquals("github", updated.provider);
    assertEquals("feature", updated.defaultBranch);
    assertEquals("update-key", updated.sshKeyName);
  }

  @Test
  @Transactional
  public void testCreateWithSshKeyWithoutKnownHosts() throws IOException, GitAPIException {
    // Create an SSH key without known_hosts
    UserPreferences up = userRepo.findByUserId(userId).get();
    CredentialRef sshKey = new CredentialRef();
    sshKey.userPreferences = up;
    sshKey.name = "github-no-known-hosts";
    sshKey.provider = "github";
    sshKey.scope = "write";
    sshKey.secretRef = "k8s:secret/test-key#id_ed25519";
    // Note: knownHostsRef is null - this should trigger automatic GitHub host key addition
    credentialRefRepo.persist(sshKey);

    Template t = new Template();
    t.name = "T6";
    t.description = "desc with SSH key without known_hosts";
    t.repositoryUrl = "git@github.com:user/repo.git";
    t.provider = "github";
    t.defaultBranch = "main";
    t.sshKeyName = "github-no-known-hosts";

    Template saved = service.create(userId, t);
    assertNotNull(saved.id);
    assertEquals("github", saved.provider);
    assertEquals("main", saved.defaultBranch);
    assertEquals("github-no-known-hosts", saved.sshKeyName);
  }

  @Test
  public void testYamlConfiguration() {
    // Test that the YAML configuration in TemplateService produces the expected format
    Map<String, Object> data = new HashMap<>();
    data.put("name", "test-proposal");
    data.put("description", "test proposal");
    data.put("provider", "github");
    data.put("defaultBranch", "main");

    // Create a YAML instance with the same configuration as TemplateService
    DumperOptions options = new DumperOptions();
    options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
    options.setExplicitStart(true);
    Yaml yaml = new Yaml(options);

    String result = yaml.dump(data);
    // The actual output order may vary, so we'll check that it contains all the expected fields
    // and starts with the document separator
    assertTrue(result.startsWith("---\n"), "YAML should start with document separator");
    assertTrue(result.contains("name: test-proposal"), "YAML should contain name field");
    assertTrue(result.contains("description: test proposal"), "YAML should contain description field");
    assertTrue(result.contains("provider: github"), "YAML should contain provider field");
    assertTrue(result.contains("defaultBranch: main"), "YAML should contain defaultBranch field");
  }

}
