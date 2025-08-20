package io.redhat.na.ssp.tasktally.model;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

@Entity
@Table(name = "templates")
public class Template extends PanacheEntityBase {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_preferences_id", nullable = false)
  public UserPreferences userPreferences;

  @Column(nullable = false)
  @NotBlank
  public String name;

  @Column
  public String description;

  @Column(name = "repository_url", nullable = false, unique = true)
  @NotBlank
  public String repositoryUrl;

  @Column
  public String provider;

  @Column(name = "default_branch")
  public String defaultBranch;

  @Column(name = "ssh_key_name")
  public String sshKeyName;

  @Column(name = "yaml_path")
  public String yamlPath = "outcomes.yml";

  @Column(name = "created_at")
  public Instant createdAt;

  @Column(name = "updated_at")
  public Instant updatedAt;

  @PrePersist
  public void prePersist() {
    if (createdAt == null) {
      createdAt = Instant.now();
    }
    if (updatedAt == null) {
      updatedAt = Instant.now();
    }
  }

  @PreUpdate
  public void preUpdate() {
    updatedAt = Instant.now();
  }
}
