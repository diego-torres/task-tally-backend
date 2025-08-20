package io.redhat.na.ssp.tasktally.api.dto;

import jakarta.validation.constraints.NotBlank;

public class OutcomeDto {
  public Long id;

  public PhaseDto phase;

  @NotBlank(message = "Prefix is required")
  public String prefix;

  @NotBlank(message = "Description is required")
  public String description;

  public String notes;

  public static class PhaseDto {
    @NotBlank(message = "Phase name is required")
    public String name;

    @NotBlank(message = "Track is required")
    public String track;

    @NotBlank(message = "Product is required")
    public String product;

    @NotBlank(message = "Environment is required")
    public String environment;
  }
}
