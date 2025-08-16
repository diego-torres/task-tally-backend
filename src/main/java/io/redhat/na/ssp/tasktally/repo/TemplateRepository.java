package io.redhat.na.ssp.tasktally.repo;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.redhat.na.ssp.tasktally.model.Template;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class TemplateRepository implements PanacheRepository<Template> {

  public List<Template> listByUser(Long userPreferencesId) {
    return list("userPreferences.id", userPreferencesId);
  }

  public Optional<Template> findByUserAndId(Long userPreferencesId, Long id) {
    return find("userPreferences.id = ?1 and id = ?2", userPreferencesId, id).firstResultOptional();
  }
}
