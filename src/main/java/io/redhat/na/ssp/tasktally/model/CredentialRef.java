package io.redhat.na.ssp.tasktally.model;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

@Entity
@Table(name = "credential_refs")
public class CredentialRef extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_preferences_id", nullable = false)
    public UserPreferences userPreferences;

    @Column(nullable = false)
    @NotBlank
    public String name;

    @Column(nullable = false)
    @NotBlank
    public String provider;

    @Column(nullable = false)
    @NotBlank
    public String scope;

    @Column(name = "secret_ref", nullable = false)
    @NotBlank
    public String secretRef;

    @Column(name = "created_at")
    public Instant createdAt;
}
