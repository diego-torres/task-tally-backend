package io.redhat.na.ssp.tasktally.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.redhat.na.ssp.tasktally.secrets.SecretWriter;
import io.redhat.na.ssp.tasktally.secrets.SshSecretRefs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class SshKeysResourceTest {
  private void cleanupUserKeys(String userId) {
    // Get all keys for the user
    var response = given().header("X-User-Id", userId).get("/api/users/" + userId + "/ssh-keys").then().extract().jsonPath().getList("name");
    // Delete each key
    for (Object key : response) {
      given().header("X-User-Id", userId).delete("/api/users/" + userId + "/ssh-keys/" + key).then().statusCode(204);
    }
  }

  @InjectMock
  SecretWriter writer;

  @BeforeEach
  public void setup() {
    when(writer.writeSshKey(any(), any(), any(), any(), any(), any()))
        .thenReturn(new SshSecretRefs("kref", "href", null));
    cleanupUserKeys("u1");
  }

  @Test
  public void createListDelete() {
    String body = "{\"name\":\"k1\",\"provider\":\"github\",\"privateKeyPem\":\"-----BEGIN OPENSSH PRIVATE KEY-----\\nAAA\\n-----END OPENSSH PRIVATE KEY-----\\n\",\"knownHosts\":\"github.com ssh-ed25519 AAAA\\n\"}";
    given().header("X-User-Id", "u1").contentType("application/json").body(body).post("/api/users/u1/ssh-keys").then()
        .statusCode(201).body("secretRef", equalTo("kref"));
    given().header("X-User-Id", "u1").get("/api/users/u1/ssh-keys").then().statusCode(200).body("", hasSize(1));
    given().header("X-User-Id", "u1").delete("/api/users/u1/ssh-keys/k1").then().statusCode(204);
    given().header("X-User-Id", "u1").get("/api/users/u1/ssh-keys").then().statusCode(200).body("", hasSize(0));
  }

  @Test
  public void duplicateName409() {
    String body = "{\"name\":\"dup\",\"provider\":\"github\",\"privateKeyPem\":\"-----BEGIN OPENSSH PRIVATE KEY-----\\nAAA\\n-----END OPENSSH PRIVATE KEY-----\\n\"}";
    given().header("X-User-Id", "u1").contentType("application/json").body(body).post("/api/users/u1/ssh-keys").then()
        .statusCode(201);
    given().header("X-User-Id", "u1").contentType("application/json").body(body).post("/api/users/u1/ssh-keys").then()
        .statusCode(409);
  }

  @Test
  public void oversizedKey400() {
    String big = "A".repeat(11 * 1024);
    String body = "{\"name\":\"big\",\"provider\":\"github\",\"privateKeyPem\":\"-----BEGIN OPENSSH PRIVATE KEY-----"
        + big + "-----END OPENSSH PRIVATE KEY-----\"}";
    given().header("X-User-Id", "u1").contentType("application/json").body(body).post("/api/users/u1/ssh-keys").then()
        .statusCode(400);
  }

  @Test
  public void headerMismatch403() {
    String body = "{\"name\":\"k2\",\"provider\":\"github\",\"privateKeyPem\":\"-----BEGIN OPENSSH PRIVATE KEY-----\\nAAA\\n-----END OPENSSH PRIVATE KEY-----\\n\"}";
    given().header("X-User-Id", "u2").contentType("application/json").body(body).post("/api/users/u1/ssh-keys").then()
        .statusCode(403);
  }
}
