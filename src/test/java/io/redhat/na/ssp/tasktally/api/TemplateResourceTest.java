package io.redhat.na.ssp.tasktally.api;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.redhat.na.ssp.tasktally.github.GitHubClient;
import io.redhat.na.ssp.tasktally.model.CredentialRef;
import io.redhat.na.ssp.tasktally.service.PreferencesService;
import io.redhat.na.ssp.tasktally.secrets.SecretResolver;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@QuarkusTest
public class TemplateResourceTest {

    @Inject
    PreferencesService preferencesService;

    @InjectMock
    GitHubClient gitHubClient;

    @InjectMock
    SecretResolver secretResolver;

    @BeforeEach
    void setup() {
        preferencesService.getOrCreate("u1");
        CredentialRef ref = new CredentialRef();
        ref.name = "gh";
        ref.provider = "github";
        ref.scope = "read";
        ref.secretRef = "k8s:secret/x#y";
        preferencesService.addCredential("u1", ref);
        when(secretResolver.resolve("k8s:secret/x#y")).thenReturn("{\"token\":\"abc\"}");
        when(gitHubClient.getYamlFiles(any(), any(), any(), any(), any())).thenReturn(Map.of("t.yaml", "name: demo"));
    }

    @Test
    void pullTemplates() {
        given()
            .header("X-User-Id", "u1")
            .contentType("application/json")
            .body("{\"owner\":\"o\",\"repo\":\"r\",\"path\":\"p\",\"branch\":\"main\",\"credentialName\":\"gh\"}")
        .when()
            .post("/api/github/templates/pull")
        .then()
            .statusCode(200)
            .body("[0].name", org.hamcrest.Matchers.equalTo("demo"));
    }
}
