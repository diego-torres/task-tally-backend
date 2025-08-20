package io.redhat.na.ssp.tasktally.model;

import java.time.Instant;

/**
 * Outcome model for YAML serialization. Outcomes are stored in Git repositories as YAML files, not in the database.
 */
public class Outcome {

  public Phase phase;
  public String prefix;
  public String description;
  public String notes;
  public Instant createdAt;
  public Instant updatedAt;

  public static class Phase {
    public String name;
    public String track;
    public String product;
    public String environment;
  }

  public Outcome() {
    this.createdAt = Instant.now();
    this.updatedAt = Instant.now();
  }

  public Outcome(String phaseName, String track, String product, String environment, String prefix, String description,
      String notes) {
    this();
    this.phase = new Phase();
    this.phase.name = phaseName;
    this.phase.track = track;
    this.phase.product = product;
    this.phase.environment = environment;
    this.prefix = prefix;
    this.description = description;
    this.notes = notes;
  }
}
