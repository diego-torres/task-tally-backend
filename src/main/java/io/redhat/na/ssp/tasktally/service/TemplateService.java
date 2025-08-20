package io.redhat.na.ssp.tasktally.service;

import io.redhat.na.ssp.tasktally.model.Template;
import io.redhat.na.ssp.tasktally.model.UserPreferences;
import io.redhat.na.ssp.tasktally.model.CredentialRef;
import io.redhat.na.ssp.tasktally.repo.TemplateRepository;
import io.redhat.na.ssp.tasktally.repo.UserPreferencesRepository;
import io.redhat.na.ssp.tasktally.github.ssh.SshGitService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.DumperOptions;

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
  @Inject
  SshKeyService sshKeyService;

  private final Yaml yaml;

  public TemplateService() {
    DumperOptions options = new DumperOptions();
    options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
    options.setExplicitStart(true);
    this.yaml = new Yaml(options);
  }

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

    // Validate SSH key reference if provided
    if (tmpl.sshKeyName != null && !tmpl.sshKeyName.trim().isEmpty()) {
      try {
        sshKeyService.get(userId, tmpl.sshKeyName);
        LOG.debugf("SSH key %s validated for user %s", tmpl.sshKeyName, userId);
      } catch (IllegalArgumentException e) {
        LOG.errorf("Invalid SSH key reference %s for user %s: %s", tmpl.sshKeyName, userId, e.getMessage());
        throw new IllegalArgumentException("Invalid SSH key reference: " + tmpl.sshKeyName);
      }
    }

    // Set default branch if not provided
    if (tmpl.defaultBranch == null || tmpl.defaultBranch.trim().isEmpty()) {
      tmpl.defaultBranch = "main";
    }

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

    // Validate SSH key reference if provided
    if (incoming.sshKeyName != null && !incoming.sshKeyName.trim().isEmpty()) {
      try {
        sshKeyService.get(userId, incoming.sshKeyName);
        LOG.debugf("SSH key %s validated for user %s", incoming.sshKeyName, userId);
      } catch (IllegalArgumentException e) {
        LOG.errorf("Invalid SSH key reference %s for user %s: %s", incoming.sshKeyName, userId, e.getMessage());
        throw new IllegalArgumentException("Invalid SSH key reference: " + incoming.sshKeyName);
      }
    }

    existing.name = incoming.name;
    existing.description = incoming.description;
    existing.repositoryUrl = incoming.repositoryUrl;
    existing.provider = incoming.provider;
    existing.defaultBranch = incoming.defaultBranch != null ? incoming.defaultBranch : "main";
    existing.sshKeyName = incoming.sshKeyName;
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

  void syncRepo(Template tmpl) {
    try {
      LOG.debugf("Syncing repository %s", tmpl.repositoryUrl);
      Path work = Files.createTempDirectory("tmpl-repo");

      // Get SSH credential if specified
      CredentialRef sshCred = null;
      if (tmpl.sshKeyName != null && !tmpl.sshKeyName.trim().isEmpty()) {
        try {
          // We need to get the user ID from the template's user preferences
          String userId = tmpl.userPreferences.userId;
          sshCred = sshKeyService.get(userId, tmpl.sshKeyName);
          LOG.debugf("Using SSH key %s for repository sync", tmpl.sshKeyName);
        } catch (Exception e) {
          LOG.warnf("Failed to get SSH key %s, proceeding without SSH: %s", tmpl.sshKeyName, e.getMessage());
        }
      }

      String branch = tmpl.defaultBranch != null ? tmpl.defaultBranch : "main";
      Path repo = gitService.cloneShallow(tmpl.repositoryUrl, branch, work, sshCred);
      Map<String, Object> data = new HashMap<>();
      data.put("name", tmpl.name);
      data.put("description", tmpl.description);
      data.put("provider", tmpl.provider);
      data.put("defaultBranch", tmpl.defaultBranch);
      Path file = repo.resolve("template.yml");
      Files.writeString(file, yaml.dump(data));
      gitService.commitAndPush(repo, "TaskTally", "noreply@tasktally.local", "Update template", sshCred);
      LOG.info("Repository synced successfully");
    } catch (IOException | GitAPIException e) {
      LOG.error("Failed to sync template repository", e);
      throw new IllegalStateException("Failed to sync template repository", e);
    }
  }
}
