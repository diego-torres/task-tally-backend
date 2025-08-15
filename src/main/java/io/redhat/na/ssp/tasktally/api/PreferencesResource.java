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

@Path("/api/preferences")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PreferencesResource {

    @Inject
    PreferencesService service;

    private String userId(HttpHeaders headers) {
        String id = headers.getHeaderString("X-User-Id");
        if (id == null || id.isBlank()) {
            throw new NotAuthorizedException("X-User-Id required");
        }
        return id;
    }

    @GET
    @Path("/me")
    public PreferencesDto get(@Context HttpHeaders headers) {
        UserPreferences up = service.getOrCreate(userId(headers));
        PreferencesDto dto = new PreferencesDto();
        dto.ui = up.ui;
        dto.defaultGitProvider = up.defaultGitProvider;
        dto.version = up.version;
        return dto;
    }

    @PUT
    @Path("/me")
    public PreferencesDto update(@Valid PreferencesDto dto, @Context HttpHeaders headers) {
        UserPreferences up = new UserPreferences();
        up.ui = dto.ui;
        up.defaultGitProvider = dto.defaultGitProvider;
        up.version = dto.version;
        UserPreferences saved = service.upsert(userId(headers), up);
        PreferencesDto resp = new PreferencesDto();
        resp.ui = saved.ui;
        resp.defaultGitProvider = saved.defaultGitProvider;
        resp.version = saved.version;
        return resp;
    }

    @POST
    @Path("/me/credentials")
    public CredentialResponse addCredential(@Valid CredentialRequest req, @Context HttpHeaders headers) {
        CredentialRef ref = new CredentialRef();
        ref.name = req.name;
        ref.provider = req.provider;
        ref.scope = req.scope;
        ref.secretRef = req.secretRef;
        CredentialRef saved = service.addCredential(userId(headers), ref);
        CredentialResponse resp = new CredentialResponse();
        resp.name = saved.name;
        resp.provider = saved.provider;
        resp.scope = saved.scope;
        return resp;
    }

    @DELETE
    @Path("/me/credentials/{name}")
    public void delete(@PathParam("name") String name, @Context HttpHeaders headers) {
        service.deleteCredential(userId(headers), name);
    }
}
