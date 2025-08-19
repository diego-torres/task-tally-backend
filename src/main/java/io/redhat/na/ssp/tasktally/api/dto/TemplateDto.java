package io.redhat.na.ssp.tasktally.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class TemplateDto {
  public Long id;
  @NotBlank
  public String name;
  public String description;
  @NotBlank
  public String repositoryUrl;
  @Pattern(regexp = "^(github|gitlab)$", message = "Provider must be either 'github' or 'gitlab'")
  public String provider;
  public String defaultBranch;
  public String sshKeyName;
}
