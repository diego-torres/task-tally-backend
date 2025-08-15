package io.redhat.na.ssp.tasktally.model;

import io.redhat.na.ssp.tasktally.util.JsonMapConverter;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "user_preferences")
public class UserPreferences extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    @NotBlank
    public String userId;

    @Convert(converter = JsonMapConverter.class)
    @Column(columnDefinition = "jsonb", nullable = false)
    public Map<String, Object> ui = new HashMap<>();

    @Column(name = "default_git_provider")
    public String defaultGitProvider;

    @Version
    public Integer version;

    @Column(name = "created_at")
    public Instant createdAt;

    @Column(name = "updated_at")
    public Instant updatedAt;
}
