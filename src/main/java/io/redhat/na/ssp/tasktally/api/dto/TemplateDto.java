package io.redhat.na.ssp.tasktally.api.dto;

import jakarta.validation.constraints.NotBlank;

public class TemplateDto {
  public Long id;
  @NotBlank
  public String name;
  public String description;
  @NotBlank
  public String repositoryUrl;
}
