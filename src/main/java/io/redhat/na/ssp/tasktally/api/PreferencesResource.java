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
import jakarta.ws.rs.core.MediaType;
import org.jboss.logging.Logger;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.redhat.na.ssp.tasktally.security.Identities;

@Path("/api/preferences")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("user")
@SecurityRequirement(name = "keycloak")
public class PreferencesResource {
  private static final Logger LOG = Logger.getLogger(PreferencesResource.class);

  @Inject
  PreferencesService service;

  @Inject
  SecurityIdentity identity;

  @GET
  @Path("/me")
  public PreferencesDto get() {
    String uid = Identities.userId(identity);
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
  public PreferencesDto update(@Valid PreferencesDto dto) {
    String uid = Identities.userId(identity);
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
  public CredentialResponse addCredential(@Valid CredentialRequest req) {
    String uid = Identities.userId(identity);
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
  public void delete(@PathParam("name") String name) {
    String uid = Identities.userId(identity);
    LOG.debugf("Deleting credential %s for user %s", name, uid);
    service.deleteCredential(uid, name);
    LOG.infof("Deleted credential %s for user %s", name, uid);
  }
}
