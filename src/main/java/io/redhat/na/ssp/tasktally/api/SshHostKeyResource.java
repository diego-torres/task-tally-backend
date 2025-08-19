package io.redhat.na.ssp.tasktally.api;

import java.io.IOException;
import java.util.List;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import io.redhat.na.ssp.tasktally.service.SshHostKeyService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/ssh/host-keys")
@Tag(name = "SSH Host Keys", description = "SSH host key management operations")
public class SshHostKeyResource {
  private static final Logger LOG = Logger.getLogger(SshHostKeyResource.class);

  @Inject
  SshHostKeyService sshHostKeyService;

  @GET
  @Path("/{hostname}")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "Fetch SSH host keys from a remote server", description = "Fetches SSH host keys from the specified hostname, similar to ssh-keyscan")
  @APIResponses(value = {
      @APIResponse(responseCode = "200", description = "Host keys fetched successfully", content = @Content(schema = @Schema(implementation = HostKeyResponse.class))),
      @APIResponse(responseCode = "400", description = "Invalid hostname"),
      @APIResponse(responseCode = "500", description = "Failed to fetch host keys")})
  public Response fetchHostKeys(@PathParam("hostname") String hostname) {
    if (hostname == null || hostname.trim().isEmpty()) {
      return Response.status(Response.Status.BAD_REQUEST).entity(new ErrorResponse("hostname is required")).build();
    }

    try {
      LOG.infof("Fetching SSH host keys from: %s", hostname);
      List<String> hostKeys = sshHostKeyService.fetchHostKeys(hostname.trim());

      HostKeyResponse response = new HostKeyResponse();
      response.hostname = hostname.trim();
      response.hostKeys = hostKeys;
      response.knownHosts = String.join("\n", hostKeys) + "\n";

      LOG.infof("Successfully fetched %d host keys from %s", hostKeys.size(), hostname);
      return Response.ok(response).build();

    } catch (IllegalArgumentException e) {
      LOG.errorf("Invalid hostname %s: %s", hostname, e.getMessage());
      return Response.status(Response.Status.BAD_REQUEST).entity(new ErrorResponse(e.getMessage())).build();
    } catch (IOException e) {
      LOG.errorf("Failed to fetch host keys from %s: %s", hostname, e.getMessage());
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(new ErrorResponse("Failed to fetch host keys: " + e.getMessage())).build();
    }
  }

  @GET
  @Path("/{hostname}/check")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "Check if SSH service is available on a host", description = "Checks if the specified hostname has SSH service available on port 22")
  @APIResponses(value = {
      @APIResponse(responseCode = "200", description = "SSH service availability checked", content = @Content(schema = @Schema(implementation = SshAvailabilityResponse.class)))})
  public Response checkSshAvailability(@PathParam("hostname") String hostname) {
    if (hostname == null || hostname.trim().isEmpty()) {
      return Response.status(Response.Status.BAD_REQUEST).entity(new ErrorResponse("hostname is required")).build();
    }

    boolean available = sshHostKeyService.isSshServiceAvailable(hostname.trim());

    SshAvailabilityResponse response = new SshAvailabilityResponse();
    response.hostname = hostname.trim();
    response.sshAvailable = available;

    LOG.debugf("SSH service availability check for %s: %s", hostname, available);
    return Response.ok(response).build();
  }

  public static class HostKeyResponse {
    @Schema(description = "The hostname that was queried")
    public String hostname;

    @Schema(description = "List of SSH host key entries")
    public List<String> hostKeys;

    @Schema(description = "Known hosts content in standard format")
    public String knownHosts;
  }

  public static class SshAvailabilityResponse {
    @Schema(description = "The hostname that was checked")
    public String hostname;

    @Schema(description = "Whether SSH service is available on port 22")
    public boolean sshAvailable;
  }

  public static class ErrorResponse {
    @Schema(description = "Error message")
    public String error;

    public ErrorResponse(String error) {
      this.error = error;
    }
  }
}
