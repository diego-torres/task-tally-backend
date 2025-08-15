package io.redhat.na.ssp.tasktally.github;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public class Activity {
    @NotBlank
    public String role;
    @NotBlank
    public String task;
    @PositiveOrZero
    public int estimateHours;
}
