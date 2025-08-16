package io.redhat.na.ssp.tasktally.github;

import jakarta.validation.constraints.NotBlank;

public class TemplatePullRequest {
  @NotBlank
  public String repoUri;
  @NotBlank
  public String path;
  public String branch = "main";
  public String credentialName;
}
