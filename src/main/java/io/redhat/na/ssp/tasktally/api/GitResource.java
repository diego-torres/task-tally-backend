package io.redhat.na.ssp.tasktally.api;

import io.redhat.na.ssp.tasktally.api.dto.CredentialResponse;
import io.redhat.na.ssp.tasktally.github.ProjectTemplate;
import io.redhat.na.ssp.tasktally.github.TemplatePullRequest;
import io.redhat.na.ssp.tasktally.github.TemplateService;
import io.redhat.na.ssp.tasktally.github.TemplatePushRequest;
import io.redhat.na.ssp.tasktally.github.GitLinkRequest;
import io.redhat.na.ssp.tasktally.model.CredentialRef;
import io.redhat.na.ssp.tasktally.service.PreferencesService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import org.jboss.logging.Logger;

@Path("/api/git")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GitResource {
  private static final Logger LOG = Logger.getLogger(GitResource.class);

  @Inject
  PreferencesService preferencesService;

  @Inject
  TemplateService templateService;

  private String userId(HttpHeaders headers) {
    String id = headers.getHeaderString("X-User-Id");
    if (id == null || id.isBlank()) {
      LOG.warn("Missing X-User-Id header");
      throw new NotAuthorizedException("X-User-Id required");
    }
    LOG.debugf("Resolved user id %s", id);
    return id;
  }

  @POST
  @Path("/link")
  public CredentialResponse link(@Valid GitLinkRequest req, @Context HttpHeaders headers) {
    String uid = userId(headers);
    LOG.debugf("Linking credential %s for user %s", req.name, uid);
    CredentialRef ref = new CredentialRef();
    ref.name = req.name;
    ref.provider = "github";
    ref.scope = req.scope;
    ref.secretRef = req.ref;
    CredentialRef saved = preferencesService.addCredential(uid, ref);
    CredentialResponse resp = new CredentialResponse();
    resp.name = saved.name;
    resp.provider = saved.provider;
    resp.scope = saved.scope;
    LOG.infof("Linked credential %s for user %s", saved.name, uid);
    return resp;
  }

  @POST
  @Path("/templates/pull")
  public List<ProjectTemplate> pullTemplates(@Valid TemplatePullRequest req, @Context HttpHeaders headers) {
    String uid = userId(headers);
    LOG.debugf("Pulling templates from %s for user %s", req.repoUri, uid);
    List<ProjectTemplate> templates = templateService.pullTemplates(uid, req);
    LOG.infof("Pulled %d templates for user %s", templates.size(), uid);
    return templates;
  }

  @POST
  @Path("/templates/push")
  public void pushTemplates(@Valid TemplatePushRequest req, @Context HttpHeaders headers) {
    LOG.error("Template push not implemented");
    throw new UnsupportedOperationException("TODO");
  }
}
