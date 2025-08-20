package io.redhat.na.ssp.tasktally.api;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.InjectMock;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import io.redhat.na.ssp.tasktally.model.UserPreferences;
import io.redhat.na.ssp.tasktally.model.Template;
import io.redhat.na.ssp.tasktally.model.Outcome;
import io.redhat.na.ssp.tasktally.repo.UserPreferencesRepository;
import io.redhat.na.ssp.tasktally.repo.TemplateRepository;
import io.redhat.na.ssp.tasktally.service.GitYamlService;
import io.redhat.na.ssp.tasktally.PostgresTestResource;

import java.util.ArrayList;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
class OutcomeResourceTest {

  @Inject
  UserPreferencesRepository userRepo;

  @Inject
  TemplateRepository templateRepo;

  @InjectMock
  GitYamlService gitYamlService;

  private static final String TEST_USER_ID = "test-user";
  private Long templateId;
  private Template template;

  @BeforeEach
  @Transactional
  public void setup() {
    // Create user preferences with fixed user ID
    UserPreferences up = new UserPreferences();
    up.userId = TEST_USER_ID;
    userRepo.persist(up);

    // Create a template for the user
    template = new Template();
    template.name = "Test Template";
    template.description = "Test template for outcomes";
    template.repositoryUrl = "git@github.com:test/repo.git";
    template.provider = "github";
    template.defaultBranch = "main";
    template.yamlPath = "outcomes.yml";
    template.userPreferences = up;
    templateRepo.persist(template);

    templateId = template.id;

    // Mock GitYamlService to return empty outcomes list by default
    when(gitYamlService.readOutcomes(org.mockito.ArgumentMatchers.any(Template.class), isNull()))
        .thenReturn(new ArrayList<>());
    doNothing().when(gitYamlService).writeOutcomes(org.mockito.ArgumentMatchers.any(Template.class), anyList(),
        isNull());
  }

  @AfterEach
  @Transactional
  public void cleanup() {
    templateRepo.deleteAll();
    userRepo.deleteAll();
  }

  @Test
  @TestSecurity(user = TEST_USER_ID, roles = {"user"})
  void testListOutcomes() {
    // Mock to return some test outcomes
    List<Outcome> testOutcomes = new ArrayList<>();
    Outcome outcome = new Outcome("Discovery", "Infrastructure", "OpenShift", "Production", "INFRA",
        "Infrastructure assessment completed", "Focus on security and scalability");
    testOutcomes.add(outcome);
    when(gitYamlService.readOutcomes(org.mockito.ArgumentMatchers.any(Template.class), isNull()))
        .thenReturn(testOutcomes);

    given().when().get("/api/users/" + TEST_USER_ID + "/templates/" + templateId + "/outcomes").then().statusCode(200)
        .contentType(ContentType.JSON).body("size()", equalTo(1)).body("[0].phase.name", equalTo("Discovery"))
        .body("[0].phase.track", equalTo("Infrastructure")).body("[0].phase.product", equalTo("OpenShift"))
        .body("[0].phase.environment", equalTo("Production")).body("[0].prefix", equalTo("INFRA"))
        .body("[0].description", equalTo("Infrastructure assessment completed"))
        .body("[0].notes", equalTo("Focus on security and scalability"));
  }

  @Test
  @TestSecurity(user = TEST_USER_ID, roles = {"user"})
  void testCreateOutcome() {
    String outcomeJson = """
        {
          "phase": {
            "name": "Discovery",
            "track": "Infrastructure",
            "product": "OpenShift",
            "environment": "Production"
          },
          "prefix": "INFRA",
          "description": "Infrastructure assessment completed",
          "notes": "Focus on security and scalability"
        }
        """;

    given().contentType(ContentType.JSON).body(outcomeJson).when()
        .post("/api/users/" + TEST_USER_ID + "/templates/" + templateId + "/outcomes").then().statusCode(200)
        .contentType(ContentType.JSON).body("phase.name", equalTo("Discovery"))
        .body("phase.track", equalTo("Infrastructure")).body("phase.product", equalTo("OpenShift"))
        .body("phase.environment", equalTo("Production")).body("prefix", equalTo("INFRA"))
        .body("description", equalTo("Infrastructure assessment completed"))
        .body("notes", equalTo("Focus on security and scalability"));

    // Verify that writeOutcomes was called
    verify(gitYamlService).writeOutcomes(org.mockito.ArgumentMatchers.any(Template.class), anyList(), isNull());
  }

  @Test
  @TestSecurity(user = TEST_USER_ID, roles = {"user"})
  void testCreateOutcome_MissingRequiredFields() {
    String outcomeJson = """
        {
          "phase": {
            "name": "Discovery"
          }
        }
        """;

    given().contentType(ContentType.JSON).body(outcomeJson).when()
        .post("/api/users/" + TEST_USER_ID + "/templates/" + templateId + "/outcomes").then().statusCode(400);
  }

  @Test
  @TestSecurity(user = TEST_USER_ID, roles = {"user"})
  void testUpdateOutcome() {
    // Mock to return an existing outcome
    List<Outcome> existingOutcomes = new ArrayList<>();
    Outcome existingOutcome = new Outcome("Discovery", "Infrastructure", "OpenShift", "Production", "INFRA",
        "Old description", "Old notes");
    existingOutcomes.add(existingOutcome);
    when(gitYamlService.readOutcomes(org.mockito.ArgumentMatchers.any(Template.class), isNull()))
        .thenReturn(existingOutcomes);

    String outcomeJson = """
        {
          "phase": {
            "name": "Implementation",
            "track": "Security",
            "product": "RHEL",
            "environment": "Development"
          },
          "prefix": "SEC",
          "description": "Security baseline established",
          "notes": "Follow CIS benchmarks"
        }
        """;

    given().contentType(ContentType.JSON).body(outcomeJson).when()
        .put("/api/users/" + TEST_USER_ID + "/templates/" + templateId + "/outcomes/0").then().statusCode(200)
        .contentType(ContentType.JSON).body("phase.name", equalTo("Implementation"))
        .body("phase.track", equalTo("Security")).body("phase.product", equalTo("RHEL"))
        .body("phase.environment", equalTo("Development")).body("prefix", equalTo("SEC"))
        .body("description", equalTo("Security baseline established")).body("notes", equalTo("Follow CIS benchmarks"));

    // Verify that writeOutcomes was called
    verify(gitYamlService).writeOutcomes(org.mockito.ArgumentMatchers.any(Template.class), anyList(), isNull());
  }

  @Test
  @TestSecurity(user = TEST_USER_ID, roles = {"user"})
  void testDeleteOutcome() {
    // Mock to return an existing outcome
    List<Outcome> existingOutcomes = new ArrayList<>();
    Outcome existingOutcome = new Outcome("Discovery", "Infrastructure", "OpenShift", "Production", "INFRA",
        "Description", "Notes");
    existingOutcomes.add(existingOutcome);
    when(gitYamlService.readOutcomes(org.mockito.ArgumentMatchers.any(Template.class), isNull()))
        .thenReturn(existingOutcomes);

    given().when().delete("/api/users/" + TEST_USER_ID + "/templates/" + templateId + "/outcomes/0").then()
        .statusCode(204);

    // Verify that writeOutcomes was called
    verify(gitYamlService).writeOutcomes(org.mockito.ArgumentMatchers.any(Template.class), anyList(), isNull());
  }

  @Test
  @TestSecurity(user = TEST_USER_ID, roles = {"user"})
  void testDeleteAllOutcomes() {
    given().when().delete("/api/users/" + TEST_USER_ID + "/templates/" + templateId + "/outcomes").then()
        .statusCode(204);

    // Verify that writeOutcomes was called with empty list
    verify(gitYamlService).writeOutcomes(org.mockito.ArgumentMatchers.any(Template.class), eq(new ArrayList<>()),
        isNull());
  }

  @Test
  @TestSecurity(user = "different-user", roles = {"user"})
  void testAccessDenied_DifferentUser() {
    given().when().get("/api/users/" + TEST_USER_ID + "/templates/" + templateId + "/outcomes").then().statusCode(403);
  }

  @Test
  void testUnauthorized() {
    given().when().get("/api/users/" + TEST_USER_ID + "/templates/" + templateId + "/outcomes").then().statusCode(401);
  }
}
