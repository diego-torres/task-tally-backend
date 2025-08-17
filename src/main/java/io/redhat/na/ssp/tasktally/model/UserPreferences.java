package io.redhat.na.ssp.tasktally.model;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "user_preferences")
public class UserPreferences extends PanacheEntityBase {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "user_id", nullable = false, unique = true)
  @NotBlank
  public String userId;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "ui", nullable = false, columnDefinition = "jsonb")
  public Map<String, Object> ui = new HashMap<>();

  @Column(name = "default_git_provider")
  public String defaultGitProvider;

  @Version
  public Integer version;

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
}
