package io.redhat.na.ssp.tasktally.github;

import java.util.Map;

public interface GitHubClient {
  Map<String, String> getYamlFiles(String owner, String repo, String path, String branch, String token);
}
