package io.redhat.na.ssp.tasktally.api;

import io.redhat.na.ssp.tasktally.api.dto.CredentialRequest;
import io.redhat.na.ssp.tasktally.api.dto.CredentialResponse;
import io.redhat.na.ssp.tasktally.api.dto.PreferencesDto;
import io.redhat.na.ssp.tasktally.model.CredentialRef;
import io.redhat.na.ssp.tasktally.model.UserPreferences;
import io.redhat.na.ssp.tasktally.service.PreferencesService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import org.jboss.logging.Logger;

@Path("/api/preferences")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PreferencesResource {
  private static final Logger LOG = Logger.getLogger(PreferencesResource.class);

  @Inject
  PreferencesService service;

  private String userId(HttpHeaders headers) {
    String id = headers.getHeaderString("X-User-Id");
    if (id == null || id.isBlank()) {
      LOG.warn("Missing X-User-Id header");
      throw new NotAuthorizedException("X-User-Id required");
    }
    LOG.debugf("Resolved user id %s", id);
    return id;
  }

  @GET
  @Path("/me")
  public PreferencesDto get(@Context HttpHeaders headers) {
    String uid = userId(headers);
    LOG.debugf("Fetching preferences for user %s", uid);
    UserPreferences up = service.getOrCreate(uid);
    PreferencesDto dto = new PreferencesDto();
    dto.ui = up.ui;
    dto.defaultGitProvider = up.defaultGitProvider;
    dto.version = up.version;
    LOG.infof("Retrieved preferences for user %s", uid);
    return dto;
  }

  @PUT
  @Path("/me")
  public PreferencesDto update(@Valid PreferencesDto dto, @Context HttpHeaders headers) {
    String uid = userId(headers);
    LOG.debugf("Updating preferences for user %s", uid);
    UserPreferences up = new UserPreferences();
    up.ui = dto.ui;
    up.defaultGitProvider = dto.defaultGitProvider;
    up.version = dto.version;
    UserPreferences saved = service.upsert(uid, up);
    PreferencesDto resp = new PreferencesDto();
    resp.ui = saved.ui;
    resp.defaultGitProvider = saved.defaultGitProvider;
    resp.version = saved.version;
    LOG.infof("Updated preferences for user %s", uid);
    return resp;
  }

  @POST
  @Path("/me/credentials")
  public CredentialResponse addCredential(@Valid CredentialRequest req, @Context HttpHeaders headers) {
    String uid = userId(headers);
    LOG.debugf("Adding credential %s for user %s", req.name, uid);
    CredentialRef ref = new CredentialRef();
    ref.name = req.name;
    ref.provider = req.provider;
    ref.scope = req.scope;
    ref.secretRef = req.secretRef;
    ref.knownHostsRef = req.knownHostsRef;
    ref.passphraseRef = req.passphraseRef;
    CredentialRef saved = service.addCredential(uid, ref);
    CredentialResponse resp = new CredentialResponse();
    resp.name = saved.name;
    resp.provider = saved.provider;
    resp.scope = saved.scope;
    LOG.infof("Added credential %s for user %s", saved.name, uid);
    return resp;
  }

  @DELETE
  @Path("/me/credentials/{name}")
  public void delete(@PathParam("name") String name, @Context HttpHeaders headers) {
    String uid = userId(headers);
    LOG.debugf("Deleting credential %s for user %s", name, uid);
    service.deleteCredential(uid, name);
    LOG.infof("Deleted credential %s for user %s", name, uid);
  }
}
