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
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import org.jboss.logging.Logger;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.redhat.na.ssp.tasktally.security.Identities;

@Path("/api/git")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("user")
@SecurityRequirement(name = "keycloak")
public class GitResource {
  private static final Logger LOG = Logger.getLogger(GitResource.class);

  @Inject
  PreferencesService preferencesService;

  @Inject
  TemplateService templateService;

  @Inject
  SecurityIdentity identity;

  @POST
  @Path("/link")
  public CredentialResponse link(@Valid GitLinkRequest req) {
    String uid = Identities.userId(identity);
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
  public List<ProjectTemplate> pullTemplates(@Valid TemplatePullRequest req) {
    String uid = Identities.userId(identity);
    LOG.debugf("Pulling templates from %s for user %s", req.repoUri, uid);
    List<ProjectTemplate> templates = templateService.pullTemplates(uid, req);
    LOG.infof("Pulled %d templates for user %s", templates.size(), uid);
    return templates;
  }

  @POST
  @Path("/templates/push")
  public void pushTemplates(@Valid TemplatePushRequest req) {
    LOG.error("Template push not implemented");
    throw new UnsupportedOperationException("TODO");
  }
}
