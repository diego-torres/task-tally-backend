package io.redhat.na.ssp.tasktally.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.redhat.na.ssp.tasktally.github.ssh.SshGitService;
import io.redhat.na.ssp.tasktally.model.Template;
import io.redhat.na.ssp.tasktally.model.UserPreferences;
import io.redhat.na.ssp.tasktally.repo.TemplateRepository;
import io.redhat.na.ssp.tasktally.repo.UserPreferencesRepository;
import jakarta.inject.Inject;

@QuarkusTest
public class TemplateServiceTest {

  @Inject TemplateService service;
  @Inject UserPreferencesRepository userRepo;
  @Inject TemplateRepository templateRepo;

  @InjectMock SshGitService gitService;

  @BeforeEach
  public void setup() throws Exception {
    UserPreferences up = new UserPreferences();
    up.userId = "u1";
    userRepo.persist(up);

    when(gitService.cloneShallow(any(), any(), any(Path.class), any()))
        .thenAnswer(invocation -> {
          Path dir = invocation.getArgument(2);
          Files.createDirectories(dir);
          return dir;
        });
    doNothing().when(gitService).commitAndPush(any(Path.class), any(), any(), any(), any());
  }

  @Test
  public void testCreateUpdateDelete() throws IOException, GitAPIException {
    Template t = new Template();
    t.name = "T1";
    t.description = "desc";
    t.repositoryUrl = "git@example.com:repo.git";
    Template saved = service.create("u1", t);
    assertNotNull(saved.id);

    Template upd = new Template();
    upd.name = "T1b";
    upd.description = "desc2";
    upd.repositoryUrl = "git@example.com:repo.git";
    Template updated = service.update("u1", saved.id, upd);
    assertEquals("T1b", updated.name);

    service.delete("u1", saved.id);
    assertTrue(templateRepo.listAll().isEmpty());
  }
}
