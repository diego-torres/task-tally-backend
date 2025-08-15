package io.redhat.na.ssp.tasktally.github;

import jakarta.enterprise.context.ApplicationScoped;
import io.quarkus.arc.Unremovable;
import java.util.Map;

@ApplicationScoped
@Unremovable
public class DefaultGitHubClient implements GitHubClient {
  @Override
  public Map<String, String> getYamlFiles(String owner, String repo, String path, String branch, String token) {
    throw new UnsupportedOperationException("Not implemented");
  }
}
