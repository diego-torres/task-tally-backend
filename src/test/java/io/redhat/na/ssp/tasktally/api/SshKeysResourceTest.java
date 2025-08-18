package io.redhat.na.ssp.tasktally.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.emptyOrNullString;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.redhat.na.ssp.tasktally.secret.SecretResolver;
import io.redhat.na.ssp.tasktally.secrets.SecretWriter;
import io.redhat.na.ssp.tasktally.secrets.SshSecretRefs;

@QuarkusTest
public class SshKeysResourceTest {
  @org.junit.jupiter.api.AfterEach
  public void tearDown() {
    try {
      cleanupUserKeys("u1");
    } catch (Exception e) {
      // Log and ignore cleanup errors to prevent cascading test failures
      System.err.println("Cleanup failed: " + e.getMessage());
    }
  }
  private void cleanupUserKeys(String userId) {
    java.util.List<?> response = null;
    try {
      response = given().get("/api/users/" + userId + "/ssh-keys").then().extract().jsonPath().getList("name");
    } catch (Exception e) {
      // If response is null or invalid, skip cleanup
      System.err.println("CleanupUserKeys: failed to fetch keys: " + e.getMessage());
      return;
    }
    if (response == null)
      return;
    for (Object key : response) {
      try {
        given().delete("/api/users/" + userId + "/ssh-keys/" + key).then().statusCode(204);
      } catch (Exception e) {
        System.err.println("CleanupUserKeys: failed to delete key " + key + ": " + e.getMessage());
      }
    }
  }

  @InjectMock
  SecretWriter writer;

  @InjectMock
  SecretResolver resolver;

  @BeforeEach
  public void setup() {
    when(writer.writeSshKey(any(), any(), any(), any(), any(), any()))
        .thenReturn(new SshSecretRefs("kref", "href", null));
    when(resolver.resolveBytes(anyString())).thenReturn(new byte[0]);
  }

  @Test
  @TestSecurity(user = "u1", roles = {"user"})
  public void createListDelete() {
    cleanupUserKeys("u1");
    String body = "{\"name\":\"k1\",\"provider\":\"github\",\"privateKeyPem\":\"-----BEGIN OPENSSH PRIVATE KEY-----\\nAAA\\n----END OPENSSH PRIVATE KEY-----\\n\",\"knownHosts\":\"github.com ssh-ed25519 AAAA\\n\"}";
    given().contentType("application/json").body(body).post("/api/users/u1/ssh-keys").then().statusCode(201)
        .body("secretRef", equalTo("kref"));
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
    String body = "{\"name\":\"big\",\"provider\":\"github\",\"privateKeyPem\":\"-----BEGIN OPENSSH PRIVATE KEY-----"
        + big + "-----END OPENSSH PRIVATE KEY-----\"}";
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

  @Test
  @TestSecurity(user = "u1", roles = {"user"})
  public void generateAndFetchPublicKey() {
    cleanupUserKeys("u1");
    AtomicReference<byte[]> pub = new AtomicReference<>();
    String uniqueName = "agent-key-" + java.util.UUID.randomUUID();
    when(writer.writeSshKey(any(), any(), any(), any(), any(), any())).thenAnswer(inv -> {
      pub.set(inv.getArgument(3));
      return new SshSecretRefs("k8s:secret/" + uniqueName + "#id_ed25519", null, null);
    });
    when(resolver.resolveBytes(anyString())).thenAnswer(inv -> pub.get());

    String body = String.format("{\"name\":\"%s\",\"provider\":\"github\",\"comment\":\"task-tally@u1\"}", uniqueName);
    given().contentType("application/json").body(body).post("/api/users/u1/ssh-keys/generate").then().statusCode(201)
        .body("name", equalTo(uniqueName)).body("provider", equalTo("github"));

    given().get("/api/users/u1/ssh-keys/" + uniqueName + "/public").then().statusCode(200)
        .body("publicKey", startsWith("ssh-ed25519 ")).body("fingerprintSha256", not(emptyOrNullString()))
        .body("name", equalTo(uniqueName)).body("provider", equalTo("github"));
  }

  @Test
  @TestSecurity(user = "u2", roles = {"user"})
  public void generatePathMismatch403() {
    String body = "{\"name\":\"agent\",\"provider\":\"github\"}";
    given().contentType("application/json").body(body).post("/api/users/u1/ssh-keys/generate").then().statusCode(403);
  }

  @Test
  @TestSecurity(user = "u1", roles = {"user"})
  public void publicKeyNotFound404() {
    cleanupUserKeys("u1");
    given().get("/api/users/u1/ssh-keys/missing/public").then().statusCode(404);
  }
}
