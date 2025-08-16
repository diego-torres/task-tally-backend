package io.redhat.na.ssp.tasktally.service;

import io.redhat.na.ssp.tasktally.model.Template;
import io.redhat.na.ssp.tasktally.model.UserPreferences;
import io.redhat.na.ssp.tasktally.repo.TemplateRepository;
import io.redhat.na.ssp.tasktally.repo.UserPreferencesRepository;
import io.redhat.na.ssp.tasktally.github.ssh.SshGitService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class TemplateService {

  @Inject TemplateRepository templateRepo;
  @Inject UserPreferencesRepository userRepo;
  @Inject SshGitService gitService;

  private final Yaml yaml = new Yaml();

  @Transactional
  public List<Template> list(String userId) {
    UserPreferences up = userRepo.findByUserId(userId).orElseThrow(NotFoundException::new);
    return templateRepo.listByUser(up.id);
  }

  @Transactional
  public Template create(String userId, Template tmpl) {
    UserPreferences up = userRepo.findByUserId(userId).orElseThrow(NotFoundException::new);
    tmpl.id = null;
    tmpl.userPreferences = up;
    templateRepo.persist(tmpl);
    syncRepo(tmpl);
    return tmpl;
  }

  @Transactional
  public Template update(String userId, Long templateId, Template incoming) {
    UserPreferences up = userRepo.findByUserId(userId).orElseThrow(NotFoundException::new);
    Template existing = templateRepo.findByUserAndId(up.id, templateId).orElseThrow(NotFoundException::new);
    existing.name = incoming.name;
    existing.description = incoming.description;
    existing.repositoryUrl = incoming.repositoryUrl;
    templateRepo.persist(existing);
    syncRepo(existing);
    return existing;
  }

  @Transactional
  public void delete(String userId, Long templateId) {
    UserPreferences up = userRepo.findByUserId(userId).orElseThrow(NotFoundException::new);
    templateRepo.findByUserAndId(up.id, templateId).orElseThrow(NotFoundException::new);
    templateRepo.delete("userPreferences = ?1 and id = ?2", up, templateId);
  }

  private void syncRepo(Template tmpl) {
    try {
      Path work = Files.createTempDirectory("tmpl-repo");
      Path repo = gitService.cloneShallow(tmpl.repositoryUrl, "main", work, null);
      Map<String, Object> data = new HashMap<>();
      data.put("name", tmpl.name);
      data.put("description", tmpl.description);
      Path file = repo.resolve("template.yml");
      Files.writeString(file, yaml.dump(data));
      gitService.commitAndPush(repo, "TaskTally", "noreply@tasktally.local", "Update template", null);
    } catch (IOException | GitAPIException e) {
      throw new IllegalStateException("Failed to sync template repository", e);
    }
  }
}
