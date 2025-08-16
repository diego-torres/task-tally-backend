package io.redhat.na.ssp.tasktally.model;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

/**
 * Reference to a Git credential. Secret material is stored externally and
 * referenced via SecretRefs.
 */
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

  @Column(name = "known_hosts_ref")
  public String knownHostsRef;

  @Column(name = "passphrase_ref")
  public String passphraseRef;

  @Column(name = "created_at")
  public Instant createdAt;

  @PrePersist
  public void prePersist() {
    if (createdAt == null) {
      createdAt = Instant.now();
    }
  }

  // getters for non-panache components that expect them
  public String getName() { return name; }
  public String getProvider() { return provider; }
  public String getScope() { return scope; }
  public String getSecretRef() { return secretRef; }
  public String getKnownHostsRef() { return knownHostsRef; }
  public String getPassphraseRef() { return passphraseRef; }
}

