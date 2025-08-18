package io.redhat.na.ssp.tasktally.api;

import io.redhat.na.ssp.tasktally.model.CredentialRef;
import io.redhat.na.ssp.tasktally.service.SshKeyService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.logging.Logger;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.redhat.na.ssp.tasktally.security.Identities;

@Path("/api/users/{userId}/ssh-keys")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("user")
@SecurityRequirement(name = "keycloak")
public class SshKeysResource {
  private static final Logger LOG = Logger.getLogger(SshKeysResource.class);
  @Inject
  SshKeyService service;

  @Inject
  SecurityIdentity identity;

  private void authorize(String pathUser) {
    String tokenUser = Identities.userId(identity);
    LOG.debugf("Authorizing user: tokenUser=%s, pathUser=%s", tokenUser, pathUser);
    if (!tokenUser.equals(pathUser)) {
      LOG.warnf("Authorization failed for user: tokenUser=%s, pathUser=%s", tokenUser, pathUser);
      throw new WebApplicationException("forbidden", Response.Status.FORBIDDEN);
    }
  }

  private CredentialDto toDto(CredentialRef cred) {
    CredentialDto dto = new CredentialDto();
    dto.name = cred.name;
    dto.provider = cred.provider;
    dto.scope = cred.scope;
    dto.secretRef = cred.secretRef;
    dto.knownHostsRef = cred.knownHostsRef;
    dto.passphraseRef = cred.passphraseRef;
    dto.createdAt = cred.createdAt;
    return dto;
  }

  @GET
  @Operation(summary = "List SSH keys for user")
  @APIResponse(responseCode = "200", description = "List of SSH credential references")
  public List<CredentialDto> list(@PathParam("userId") String userId) {
    LOG.infof("Listing SSH keys for user: %s", userId);
    authorize(userId);
    List<CredentialDto> result = service.list(userId).stream().map(this::toDto).collect(Collectors.toList());
    LOG.debugf("Found %d SSH keys for user: %s", result.size(), userId);
    return result;
  }

  @POST
  @Operation(summary = "Create SSH key for user")
  @APIResponse(responseCode = "201", description = "Created")
  public Response create(@PathParam("userId") String userId, SshKeyCreateRequest req) {
    LOG.infof("Creating SSH key for user: %s", userId);
    authorize(userId);
    try {
      CredentialRef cred = service.create(userId, req);
      LOG.infof("SSH key created for user: %s, key name: %s", userId, cred.name);
      return Response.status(Response.Status.CREATED).entity(toDto(cred)).build();
    } catch (IllegalStateException e) {
      LOG.errorf("Failed to create SSH key for user: %s due to state error: %s", userId, e.getMessage());
      throw new WebApplicationException(e.getMessage(), Response.Status.CONFLICT);
    } catch (IllegalArgumentException e) {
      LOG.errorf("Failed to create SSH key for user: %s due to argument error: %s", userId, e.getMessage());
      throw new WebApplicationException(e.getMessage(), Response.Status.BAD_REQUEST);
    }
  }

  @DELETE
  @Path("/{name}")
  @Operation(summary = "Delete SSH key")
  @APIResponse(responseCode = "204", description = "Deleted")
  public Response delete(@PathParam("userId") String userId, @PathParam("name") String name) {
    LOG.infof("Deleting SSH key for user: %s, key name: %s", userId, name);
    authorize(userId);
    try {
      service.delete(userId, name);
      LOG.infof("SSH key deleted for user: %s, key name: %s", userId, name);
      return Response.noContent().build();
    } catch (IllegalArgumentException e) {
      LOG.errorf("Failed to delete SSH key for user: %s, key name: %s. Reason: %s", userId, name, e.getMessage());
      throw new NotFoundException();
    }
  }
}
