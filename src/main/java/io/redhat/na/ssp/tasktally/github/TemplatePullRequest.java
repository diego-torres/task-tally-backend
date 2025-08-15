package io.redhat.na.ssp.tasktally.github;

import jakarta.validation.constraints.NotBlank;

public class TemplatePullRequest {
  @NotBlank
  public String owner;
  @NotBlank
  public String repo;
  @NotBlank
  public String path;
  public String branch = "main";
  @NotBlank
  public String credentialName;
}
