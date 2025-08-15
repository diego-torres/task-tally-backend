package io.redhat.na.ssp.tasktally.api;

import io.redhat.na.ssp.tasktally.api.dto.CredentialResponse;
import io.redhat.na.ssp.tasktally.github.*;
import io.redhat.na.ssp.tasktally.model.CredentialRef;
import io.redhat.na.ssp.tasktally.service.PreferencesService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

@Path("/api/github")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GitHubResource {

    @Inject
    PreferencesService preferencesService;

    @Inject
    TemplateService templateService;

    private String userId(HttpHeaders headers) {
        String id = headers.getHeaderString("X-User-Id");
        if (id == null || id.isBlank()) {
            throw new NotAuthorizedException("X-User-Id required");
        }
        return id;
    }

    @POST
    @Path("/link")
    public CredentialResponse link(@Valid GitHubLinkRequest req, @Context HttpHeaders headers) {
        CredentialRef ref = new CredentialRef();
        ref.name = req.name;
        ref.provider = "github";
        ref.scope = req.scope;
        ref.secretRef = req.ref;
        CredentialRef saved = preferencesService.addCredential(userId(headers), ref);
        CredentialResponse resp = new CredentialResponse();
        resp.name = saved.name;
        resp.provider = saved.provider;
        resp.scope = saved.scope;
        return resp;
    }

    @POST
    @Path("/templates/pull")
    public List<ProjectTemplate> pullTemplates(@Valid TemplatePullRequest req, @Context HttpHeaders headers) {
        return templateService.pullTemplates(userId(headers), req);
    }

    @POST
    @Path("/templates/push")
    public void pushTemplates(@Valid TemplatePushRequest req, @Context HttpHeaders headers) {
        throw new UnsupportedOperationException("TODO");
    }
}
