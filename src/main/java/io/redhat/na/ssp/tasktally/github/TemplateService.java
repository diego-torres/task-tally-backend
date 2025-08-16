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
import io.redhat.na.ssp.tasktally.service.PreferencesService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class TemplateService {

  @Inject
  PreferencesService preferencesService;

  @Inject
  SshGitService gitService;

  private final Yaml yaml = new Yaml(new Constructor(ProjectTemplate.class, new org.yaml.snakeyaml.LoaderOptions()));

  public List<ProjectTemplate> pullTemplates(String userId, TemplatePullRequest req) {
    CredentialRef cred = null;
    if (req.credentialName != null && !req.credentialName.isBlank()) {
      cred = preferencesService.findCredential(userId, req.credentialName);
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
          throw new RuntimeException(e);
        }
      }
      return templates;
    } catch (IOException | GitAPIException e) {
      throw new IllegalStateException("Failed to pull templates", e);
    }
  }
}
