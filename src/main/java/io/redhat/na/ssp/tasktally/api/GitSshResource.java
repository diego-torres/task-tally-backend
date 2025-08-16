package io.redhat.na.ssp.tasktally.api;

import io.redhat.na.ssp.tasktally.github.ssh.SshGitService;
import io.redhat.na.ssp.tasktally.model.CredentialRef;
import io.redhat.na.ssp.tasktally.service.CredentialStore;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.nio.file.Files;

@Path("/api/git/ssh")
@Consumes(MediaType.APPLICATION_JSON)
public class GitSshResource {
  @Inject SshGitService git;
  @Inject CredentialStore store;

  @POST
  @Path("/validate")
  public Response validate(ValidateRequest req, @Context HttpHeaders headers) {
    String userId = headers.getHeaderString("X-User-Id");
    if (userId == null || userId.isBlank()) {
      return Response.status(Response.Status.UNAUTHORIZED).build();
    }
    CredentialRef cred = store.find(userId, req.credentialName).orElse(null);
    if (cred == null) {
      return Response.status(Response.Status.BAD_REQUEST).entity(new ValidateResult(false, "Credential not found")).build();
    }
    String uri = "git@" + req.provider + ".com:" + req.owner + "/" + req.repo + ".git";
    try {
      java.nio.file.Path dir = Files.createTempDirectory("git-validate");
      git.cloneShallow(uri, req.branch, dir, cred);
      return Response.ok(new ValidateResult(true, "Authentication succeeded and repo reachable")).build();
    } catch (Exception e) {
      return Response.status(Response.Status.BAD_REQUEST).entity(new ValidateResult(false, e.getMessage())).build();
    }
  }

  public static class ValidateRequest {
    public String provider;
    public String owner;
    public String repo;
    public String branch;
    public String credentialName;
  }

  public static class ValidateResult {
    public boolean ok;
    public String message;
    public ValidateResult(boolean ok, String message) {
      this.ok = ok;
      this.message = message;
    }
  }
}
