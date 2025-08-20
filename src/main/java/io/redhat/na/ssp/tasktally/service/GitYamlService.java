package io.redhat.na.ssp.tasktally.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.redhat.na.ssp.tasktally.model.Outcome;
import io.redhat.na.ssp.tasktally.model.Template;
import io.redhat.na.ssp.tasktally.model.CredentialRef;
import io.redhat.na.ssp.tasktally.secrets.SecretResolver;
import io.redhat.na.ssp.tasktally.github.ssh.SshGitService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.ObjectLoader;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class GitYamlService {
  private static final Logger LOG = Logger.getLogger(GitYamlService.class);

  private final ObjectMapper yamlMapper;

  // SecretResolver is not used in the current implementation
  // @Inject
  // SecretResolver secretResolver;

  @Inject
  SshGitService sshGitService;

  public GitYamlService() {
    this.yamlMapper = new ObjectMapper(new YAMLFactory());
  }

  /**
   * Read outcomes from the template's Git repository
   */
  public List<Outcome> readOutcomes(Template template, CredentialRef credential) {
    LOG.debugf("Reading outcomes from Git repository: %s", template.repositoryUrl);

    try {
      // Clone repository to temporary directory
      Path tempDir = Files.createTempDirectory("tasktally-outcomes");
      Path repoDir = sshGitService.cloneShallow(template.repositoryUrl,
          template.defaultBranch != null ? template.defaultBranch : "main", tempDir, credential);

      // Read YAML file
      Path yamlFile = repoDir.resolve(template.yamlPath);
      if (!Files.exists(yamlFile)) {
        LOG.infof("No %s found in repository %s, returning empty list", template.yamlPath, template.repositoryUrl);
        return new ArrayList<>();
      }

      String yamlContent = Files.readString(yamlFile);
      if (yamlContent.trim().isEmpty()) {
        LOG.infof("Empty %s found in repository %s, returning empty list", template.yamlPath, template.repositoryUrl);
        return new ArrayList<>();
      }

      Map<String, Object> yamlData = yamlMapper.readValue(yamlContent, Map.class);
      List<Map<String, Object>> outcomesList = (List<Map<String, Object>>) yamlData.get("outcomes");

      if (outcomesList == null) {
        LOG.warnf("No 'outcomes' key found in %s, returning empty list", template.yamlPath);
        return new ArrayList<>();
      }

      List<Outcome> outcomes = new ArrayList<>();
      for (Map<String, Object> outcomeMap : outcomesList) {
        Map<String, Object> outcomeData = (Map<String, Object>) outcomeMap.get("outcome");
        if (outcomeData != null) {
          outcomes.add(mapToOutcome(outcomeData));
        }
      }

      LOG.infof("Read %d outcomes from Git repository %s", outcomes.size(), template.repositoryUrl);
      return outcomes;

    } catch (Exception e) {
      LOG.errorf(e, "Failed to read outcomes from Git repository %s", template.repositoryUrl);
      throw new RuntimeException("Failed to read outcomes from Git repository", e);
    }
  }

  /**
   * Write outcomes to the template's Git repository
   */
  public void writeOutcomes(Template template, List<Outcome> outcomes, CredentialRef credential) {
    LOG.debugf("Writing %d outcomes to Git repository: %s", outcomes.size(), template.repositoryUrl);

    try {
      // Clone repository to temporary directory
      Path tempDir = Files.createTempDirectory("tasktally-outcomes");
      Path repoDir = sshGitService.cloneShallow(template.repositoryUrl,
          template.defaultBranch != null ? template.defaultBranch : "main", tempDir, credential);

      // Prepare YAML structure
      List<Map<String, Object>> outcomesList = new ArrayList<>();
      for (Outcome outcome : outcomes) {
        Map<String, Object> outcomeMap = Map.of("outcome", outcomeToMap(outcome));
        outcomesList.add(outcomeMap);
      }

      Map<String, Object> yamlData = Map.of("outcomes", outcomesList);
      String yamlContent = yamlMapper.writeValueAsString(yamlData);

      // Write YAML file
      Path yamlFile = repoDir.resolve(template.yamlPath);
      Files.writeString(yamlFile, yamlContent);

      // Commit and push
      sshGitService.commitAndPush(repoDir, "Task-tally Bot", "bot@tasktally.com", "Update outcomes.yml", credential);

      LOG.infof("Successfully wrote %d outcomes to Git repository %s", outcomes.size(), template.repositoryUrl);

    } catch (Exception e) {
      LOG.errorf(e, "Failed to write outcomes to Git repository %s", template.repositoryUrl);
      throw new RuntimeException("Failed to write outcomes to Git repository", e);
    }
  }

  private Outcome mapToOutcome(Map<String, Object> data) {
    Outcome outcome = new Outcome();

    // Handle phase object
    Map<String, Object> phaseData = (Map<String, Object>) data.get("phase");
    if (phaseData != null) {
      outcome.phase = new Outcome.Phase();
      outcome.phase.name = (String) phaseData.get("name");
      outcome.phase.track = (String) phaseData.get("track");
      outcome.phase.product = (String) phaseData.get("product");
      outcome.phase.environment = (String) phaseData.get("environment");
    }

    outcome.prefix = (String) data.get("prefix");
    outcome.description = (String) data.get("description");
    outcome.notes = (String) data.get("notes");

    return outcome;
  }

  private Map<String, Object> outcomeToMap(Outcome outcome) {
    Map<String, Object> phaseMap = Map.of("name", outcome.phase.name, "track", outcome.phase.track, "product",
        outcome.phase.product, "environment", outcome.phase.environment);

    return Map.of("phase", phaseMap, "prefix", outcome.prefix, "description", outcome.description, "notes",
        outcome.notes);
  }
}
