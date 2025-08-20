package io.redhat.na.ssp.tasktally.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.greaterThan;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;

@QuarkusTest
class SshHostKeyResourceTest {

  @Test
  @TestSecurity(user = "alice", roles = {"user"})
  void testFetchHostKeysWithValidHostname() {
    var response = given().when().get("/api/ssh/host-keys/github.com");

    if (response.getStatusCode() == 200) {
      response.then().body("hostname", equalTo("github.com")).body("hostKeys", notNullValue())
          .body("knownHosts", notNullValue()).body("hostKeys.size()", greaterThan(0));
    } else if (response.getStatusCode() == 500) {
      response.then().body("error", containsString("Connection timeout"));
    } else {
      throw new AssertionError("Unexpected status code: " + response.getStatusCode());
    }
  }

  @Test
  @TestSecurity(user = "alice", roles = {"user"})
  void testFetchHostKeysWithGitLab() {
    var response = given().when().get("/api/ssh/host-keys/gitlab.com");

    if (response.getStatusCode() == 200) {
      response.then().body("hostname", equalTo("gitlab.com")).body("hostKeys", notNullValue())
          .body("knownHosts", notNullValue()).body("hostKeys.size()", greaterThan(0));
    } else if (response.getStatusCode() == 500) {
      response.then().body("error", containsString("Connection timeout"));
    } else {
      throw new AssertionError("Unexpected status code: " + response.getStatusCode());
    }
  }

  @Test
  @TestSecurity(user = "alice", roles = {"user"})
  void testFetchHostKeysWithInvalidHostname() {
    given().when().get("/api/ssh/host-keys/invalid-hostname-that-does-not-exist-12345.com").then().statusCode(500)
        .body("error", notNullValue());
  }

  @Test
  @TestSecurity(user = "alice", roles = {"user"})
  void testFetchHostKeysWithEmptyHostname() {
    given().when().get("/api/ssh/host-keys/").then().statusCode(404);
  }

  @Test
  @TestSecurity(user = "alice", roles = {"user"})
  void testCheckSshAvailabilityWithValidHostname() {
    given().when().get("/api/ssh/host-keys/github.com/check").then().statusCode(200)
        .body("hostname", equalTo("github.com")).body("sshAvailable", equalTo(true));
  }

  @Test
  @TestSecurity(user = "alice", roles = {"user"})
  void testCheckSshAvailabilityWithInvalidHostname() {
    given().when().get("/api/ssh/host-keys/invalid-hostname-that-does-not-exist-12345.com/check").then().statusCode(200)
        .body("hostname", equalTo("invalid-hostname-that-does-not-exist-12345.com"))
        .body("sshAvailable", equalTo(false));
  }

  @Test
  @TestSecurity(user = "alice", roles = {"user"})
  void testCheckSshAvailabilityWithEmptyHostname() {
    given().when().get("/api/ssh/host-keys//check").then().statusCode(404);
  }

  @Test
  void testFetchHostKeysWithoutAuthentication() {
    given().when().get("/api/ssh/host-keys/github.com").then().statusCode(401);
  }

  @Test
  void testCheckSshAvailabilityWithoutAuthentication() {
    given().when().get("/api/ssh/host-keys/github.com/check").then().statusCode(401);
  }
}
