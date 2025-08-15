package io.redhat.na.ssp.tasktally.github;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

public class ProjectTemplate {
  @NotBlank
  public String name;
  public String description;
  @NotNull
  @Valid
  public List<Activity> activities = new ArrayList<>();
}
