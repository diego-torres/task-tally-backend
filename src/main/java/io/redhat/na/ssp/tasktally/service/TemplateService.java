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
import org.jboss.logging.Logger;

@ApplicationScoped
public class TemplateService {
  private static final Logger LOG = Logger.getLogger(TemplateService.class);

  @Inject
  TemplateRepository templateRepo;
  @Inject
  UserPreferencesRepository userRepo;
  @Inject
  SshGitService gitService;

  private final Yaml yaml = new Yaml();

  @Transactional
  public List<Template> list(String userId) {
    LOG.debugf("Listing templates for user %s", userId);
    UserPreferences up = userRepo.findByUserId(userId).orElseThrow(() -> {
      LOG.errorf("User %s not found", userId);
      return new NotFoundException();
    });
    List<Template> list = templateRepo.listByUser(up.id);
    LOG.infof("Found %d templates for user %s", list.size(), userId);
    return list;
  }

  @Transactional
  public Template create(String userId, Template tmpl) {
    LOG.debugf("Creating template %s for user %s", tmpl.name, userId);
    UserPreferences up = userRepo.findByUserId(userId).orElseThrow(() -> {
      LOG.errorf("User %s not found", userId);
      return new NotFoundException();
    });
    tmpl.id = null;
    tmpl.userPreferences = up;
    templateRepo.persist(tmpl);
    syncRepo(tmpl);
    LOG.infof("Created template %s for user %s", tmpl.name, userId);
    return tmpl;
  }

  @Transactional
  public Template update(String userId, Long templateId, Template incoming) {
    LOG.debugf("Updating template %d for user %s", templateId, userId);
    UserPreferences up = userRepo.findByUserId(userId).orElseThrow(() -> {
      LOG.errorf("User %s not found", userId);
      return new NotFoundException();
    });
    Template existing = templateRepo.findByUserAndId(up.id, templateId).orElseThrow(() -> {
      LOG.errorf("Template %d for user %s not found", templateId, userId);
      return new NotFoundException();
    });
    existing.name = incoming.name;
    existing.description = incoming.description;
    existing.repositoryUrl = incoming.repositoryUrl;
    templateRepo.persist(existing);
    syncRepo(existing);
    LOG.infof("Updated template %d for user %s", templateId, userId);
    return existing;
  }

  @Transactional
  public void delete(String userId, Long templateId) {
    LOG.debugf("Deleting template %d for user %s", templateId, userId);
    UserPreferences up = userRepo.findByUserId(userId).orElseThrow(() -> {
      LOG.errorf("User %s not found", userId);
      return new NotFoundException();
    });
    templateRepo.findByUserAndId(up.id, templateId).orElseThrow(() -> {
      LOG.errorf("Template %d for user %s not found", templateId, userId);
      return new NotFoundException();
    });
    templateRepo.delete("userPreferences = ?1 and id = ?2", up, templateId);
    LOG.infof("Deleted template %d for user %s", templateId, userId);
  }

  private void syncRepo(Template tmpl) {
    try {
      LOG.debugf("Syncing repository %s", tmpl.repositoryUrl);
      Path work = Files.createTempDirectory("tmpl-repo");
      Path repo = gitService.cloneShallow(tmpl.repositoryUrl, "main", work, null);
      Map<String, Object> data = new HashMap<>();
      data.put("name", tmpl.name);
      data.put("description", tmpl.description);
      Path file = repo.resolve("template.yml");
      Files.writeString(file, yaml.dump(data));
      gitService.commitAndPush(repo, "TaskTally", "noreply@tasktally.local", "Update template", null);
      LOG.info("Repository synced successfully");
    } catch (IOException | GitAPIException e) {
      LOG.error("Failed to sync template repository", e);
      throw new IllegalStateException("Failed to sync template repository", e);
    }
  }
}
