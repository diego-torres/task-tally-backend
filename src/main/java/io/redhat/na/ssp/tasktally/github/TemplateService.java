package io.redhat.na.ssp.tasktally.github;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import io.redhat.na.ssp.tasktally.github.ssh.SshGitService;
import io.redhat.na.ssp.tasktally.model.CredentialRef;
import io.redhat.na.ssp.tasktally.service.SshKeyService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class TemplateService {
  private static final Logger LOG = Logger.getLogger(TemplateService.class);

  @Inject
  SshKeyService sshKeyService;

  @Inject
  SshGitService gitService;

  private final Yaml yaml = new Yaml(new Constructor(ProjectTemplate.class, new org.yaml.snakeyaml.LoaderOptions()));

  public List<ProjectTemplate> pullTemplates(String userId, TemplatePullRequest req) {
    LOG.debugf("Pulling templates from %s for user %s", req.repoUri, userId);
    CredentialRef cred = null;
    if (req.credentialName != null && !req.credentialName.isBlank()) {
      cred = sshKeyService.get(userId, req.credentialName);
    }
    try {
      Path work = Files.createTempDirectory("tmpl-clone");
      Path repoDir = gitService.cloneShallow(req.repoUri, req.branch, work, cred);
      Path templatesDir = repoDir.resolve(req.path);
      List<ProjectTemplate> templates = new ArrayList<>();
      Path templateFile = templatesDir.resolve("template.yml");
      if (Files.exists(templateFile)) {
        try {
          String content = Files.readString(templateFile);
          templates.add(yaml.load(content));
        } catch (IOException e) {
          LOG.error("Failed reading template file", e);
          throw new RuntimeException(e);
        }
      } else {
        LOG.warnf("No template.yml found at %s", templateFile);
      }
      LOG.infof("Pulled %d templates for user %s", templates.size(), userId);
      return templates;
    } catch (IOException | GitAPIException e) {
      LOG.error("Failed to pull templates", e);
      throw new IllegalStateException("Failed to pull templates", e);
    }
  }
}
