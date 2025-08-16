package io.redhat.na.ssp.tasktally.github;

import io.redhat.na.ssp.tasktally.model.CredentialRef;
import io.redhat.na.ssp.tasktally.service.PreferencesService;
import io.redhat.na.ssp.tasktally.github.ssh.SshGitService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

@ApplicationScoped
public class TemplateService {

  @Inject
  PreferencesService preferencesService;

  @Inject
  SshGitService gitService;

  private final Yaml yaml = new Yaml(new Constructor(ProjectTemplate.class));

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
      try (Stream<Path> stream = Files.list(templatesDir)) {
        stream
            .filter(p -> p.toString().endsWith(".yml") || p.toString().endsWith(".yaml"))
            .forEach(p -> {
              try {
                String content = Files.readString(p);
                templates.add(yaml.load(content));
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            });
      }
      return templates;
    } catch (IOException | GitAPIException e) {
      throw new IllegalStateException("Failed to pull templates", e);
    }
  }
}
