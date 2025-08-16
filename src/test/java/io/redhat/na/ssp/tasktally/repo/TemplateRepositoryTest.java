package io.redhat.na.ssp.tasktally.repo;

import io.quarkus.test.junit.QuarkusTest;
import io.redhat.na.ssp.tasktally.model.Template;
import io.redhat.na.ssp.tasktally.model.UserPreferences;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class TemplateRepositoryTest {

  @Inject TemplateRepository repo;
  @Inject UserPreferencesRepository userRepo;

  @Test
  @Transactional
  public void testListByUser() {
    UserPreferences up = new UserPreferences();
    up.userId = "u2";
    userRepo.persist(up);

    Template t = new Template();
    t.userPreferences = up;
    t.name = "T";
    t.repositoryUrl = "git@example.com:repo2.git";
    repo.persist(t);

    List<Template> list = repo.listByUser(up.id);
    assertEquals(1, list.size());
  }
}
