package io.redhat.na.ssp.tasktally.api;

import io.redhat.na.ssp.tasktally.api.dto.TemplateDto;
import io.redhat.na.ssp.tasktally.model.Template;
import io.redhat.na.ssp.tasktally.service.TemplateService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.stream.Collectors;
import org.jboss.logging.Logger;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.redhat.na.ssp.tasktally.security.Identities;
import jakarta.ws.rs.ForbiddenException;

@Path("/api/users/{userId}/templates")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("user")
@SecurityRequirement(name = "keycloak")
public class TemplateResource {
  private static final Logger LOG = Logger.getLogger(TemplateResource.class);

  @Inject
  TemplateService service;

  @Inject
  SecurityIdentity identity;

  private void authorize(String pathUser) {
    String tokenUser = Identities.userId(identity);
    if (!tokenUser.equals(pathUser)) {
      throw new ForbiddenException("not your resource");
    }
  }

  @GET
  public List<TemplateDto> list(@PathParam("userId") String userId) {
    authorize(userId);
    LOG.debugf("Listing templates for user %s", userId);
    List<TemplateDto> list = service.list(userId).stream().map(this::toDto).collect(Collectors.toList());
    LOG.infof("Retrieved %d templates for user %s", list.size(), userId);
    return list;
  }

  @POST
  public TemplateDto create(@PathParam("userId") String userId, @Valid TemplateDto dto) {
    authorize(userId);
    LOG.debugf("Creating template %s for user %s", dto.name, userId);
    Template tmpl = fromDto(dto);
    Template saved = service.create(userId, tmpl);
    LOG.infof("Created template %s for user %s", saved.name, userId);
    return toDto(saved);
  }

  @PUT
  @Path("/{id}")
  public TemplateDto update(@PathParam("userId") String userId, @PathParam("id") Long id, @Valid TemplateDto dto) {
    authorize(userId);
    LOG.debugf("Updating template %d for user %s", id, userId);
    Template tmpl = fromDto(dto);
    Template updated = service.update(userId, id, tmpl);
    LOG.infof("Updated template %d for user %s", id, userId);
    return toDto(updated);
  }

  @DELETE
  @Path("/{id}")
  public void delete(@PathParam("userId") String userId, @PathParam("id") Long id) {
    authorize(userId);
    LOG.debugf("Deleting template %d for user %s", id, userId);
    service.delete(userId, id);
    LOG.infof("Deleted template %d for user %s", id, userId);
  }

  private TemplateDto toDto(Template t) {
    TemplateDto dto = new TemplateDto();
    dto.id = t.id;
    dto.name = t.name;
    dto.description = t.description;
    dto.repositoryUrl = t.repositoryUrl;
    return dto;
  }

  private Template fromDto(TemplateDto dto) {
    Template t = new Template();
    t.name = dto.name;
    t.description = dto.description;
    t.repositoryUrl = dto.repositoryUrl;
    return t;
  }
}
