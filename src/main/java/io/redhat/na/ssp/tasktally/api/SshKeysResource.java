package io.redhat.na.ssp.tasktally.api;

import io.redhat.na.ssp.tasktally.model.CredentialRef;
import io.redhat.na.ssp.tasktally.service.SshKeyService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
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

@Path("/api/users/{userId}/ssh-keys")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class SshKeysResource {
  private static final Logger LOG = Logger.getLogger(SshKeysResource.class);
  @Inject
  SshKeyService service;

  private void authorize(String pathUser, String headerUser) {
    if (headerUser == null || headerUser.isBlank() || !headerUser.equals(pathUser)) {
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
  public List<CredentialDto> list(@PathParam("userId") String userId, @HeaderParam("X-User-Id") String header) {
    authorize(userId, header);
    return service.list(userId).stream().map(this::toDto).collect(Collectors.toList());
  }

  @POST
  @Operation(summary = "Create SSH key for user")
  @APIResponse(responseCode = "201", description = "Created")
  public Response create(@PathParam("userId") String userId, @HeaderParam("X-User-Id") String header,
      SshKeyCreateRequest req) {
    authorize(userId, header);
    try {
      CredentialRef cred = service.create(userId, req);
      return Response.status(Response.Status.CREATED).entity(toDto(cred)).build();
    } catch (IllegalStateException e) {
      throw new WebApplicationException(e.getMessage(), Response.Status.CONFLICT);
    } catch (IllegalArgumentException e) {
      throw new WebApplicationException(e.getMessage(), Response.Status.BAD_REQUEST);
    }
  }

  @DELETE
  @Path("/{name}")
  @Operation(summary = "Delete SSH key")
  @APIResponse(responseCode = "204", description = "Deleted")
  public Response delete(@PathParam("userId") String userId, @HeaderParam("X-User-Id") String header,
      @PathParam("name") String name) {
    authorize(userId, header);
    try {
      service.delete(userId, name);
      return Response.noContent().build();
    } catch (IllegalArgumentException e) {
      throw new NotFoundException();
    }
  }
}
