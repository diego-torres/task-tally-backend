package io.redhat.na.ssp.tasktally.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.redhat.na.ssp.tasktally.secrets.SecretWriter;
import io.redhat.na.ssp.tasktally.secrets.SshSecretRefs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class SshKeysResourceTest {
  private void cleanupUserKeys(String userId) {
    var response = given().get("/api/users/" + userId + "/ssh-keys").then().extract().jsonPath().getList("name");
    for (Object key : response) {
      given().delete("/api/users/" + userId + "/ssh-keys/" + key).then().statusCode(204);
    }
  }

  @InjectMock
  SecretWriter writer;

  @BeforeEach
  public void setup() {
    when(writer.writeSshKey(any(), any(), any(), any(), any(), any()))
        .thenReturn(new SshSecretRefs("kref", "href", null));
  }

  @Test
  @TestSecurity(user = "u1", roles = {"user"})
  public void createListDelete() {
    cleanupUserKeys("u1");
    String body = "{\"name\":\"k1\",\"provider\":\"github\",\"privateKeyPem\":\"-----BEGIN OPENSSH PRIVATE KEY-----\\nAAA\\n----END OPENSSH PRIVATE KEY-----\\n\",\"knownHosts\":\"github.com ssh-ed25519 AAAA\\n\"}";
    given().contentType("application/json").body(body).post("/api/users/u1/ssh-keys").then()
        .statusCode(201).body("secretRef", equalTo("kref"));
    given().get("/api/users/u1/ssh-keys").then().statusCode(200).body("", hasSize(1));
    given().delete("/api/users/u1/ssh-keys/k1").then().statusCode(204);
    given().get("/api/users/u1/ssh-keys").then().statusCode(200).body("", hasSize(0));
  }

  @Test
  @TestSecurity(user = "u1", roles = {"user"})
  public void duplicateName409() {
    cleanupUserKeys("u1");
    String body = "{\"name\":\"dup\",\"provider\":\"github\",\"privateKeyPem\":\"-----BEGIN OPENSSH PRIVATE KEY-----\\nAAA\\n----END OPENSSH PRIVATE KEY-----\\n\"}";
    given().contentType("application/json").body(body).post("/api/users/u1/ssh-keys").then().statusCode(201);
    given().contentType("application/json").body(body).post("/api/users/u1/ssh-keys").then().statusCode(409);
  }

  @Test
  @TestSecurity(user = "u1", roles = {"user"})
  public void oversizedKey400() {
    String big = "A".repeat(11 * 1024);
    String body = "{\"name\":\"big\",\"provider\":\"github\",\"privateKeyPem\":\"-----BEGIN OPENSSH PRIVATE KEY-----" +
        big + "-----END OPENSSH PRIVATE KEY-----\"}";
    given().contentType("application/json").body(body).post("/api/users/u1/ssh-keys").then().statusCode(400);
  }

  @Test
  @TestSecurity(user = "u2", roles = {"user"})
  public void pathMismatch403() {
    String body = "{\"name\":\"k2\",\"provider\":\"github\",\"privateKeyPem\":\"-----BEGIN OPENSSH PRIVATE KEY-----\\nAAA\\n----END OPENSSH PRIVATE KEY-----\\n\"}";
    given().contentType("application/json").body(body).post("/api/users/u1/ssh-keys").then().statusCode(403);
  }

  @Test
  public void missingToken401() {
    given().get("/api/users/u1/ssh-keys").then().statusCode(401);
  }
}
