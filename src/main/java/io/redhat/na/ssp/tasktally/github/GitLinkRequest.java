package io.redhat.na.ssp.tasktally.github;

import jakarta.validation.constraints.NotBlank;

public class GitLinkRequest {
  @NotBlank
  public String name;
  @NotBlank
  public String ref;
  public String scope = "write";
}
