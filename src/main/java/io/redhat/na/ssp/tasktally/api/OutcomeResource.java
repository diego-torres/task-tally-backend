package io.redhat.na.ssp.tasktally.api;

import io.redhat.na.ssp.tasktally.api.dto.OutcomeDto;
import io.redhat.na.ssp.tasktally.model.Outcome;
import io.redhat.na.ssp.tasktally.service.OutcomeService;
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

@Path("/api/users/{userId}/templates/{templateId}/outcomes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("user")
@SecurityRequirement(name = "keycloak")
public class OutcomeResource {
  private static final Logger LOG = Logger.getLogger(OutcomeResource.class);

  @Inject
  OutcomeService service;

  @Inject
  SecurityIdentity identity;

  private void authorize(String pathUser) {
    String tokenUser = Identities.userId(identity);
    if (!tokenUser.equals(pathUser)) {
      throw new ForbiddenException("not your resource");
    }
  }

  @GET
  public List<OutcomeDto> list(@PathParam("userId") String userId, @PathParam("templateId") Long templateId) {
    authorize(userId);
    LOG.debugf("Listing outcomes for template %d for user %s", templateId, userId);
    List<OutcomeDto> list = service.listByTemplate(userId, templateId).stream().map(this::toDto)
        .collect(Collectors.toList());
    LOG.infof("Retrieved %d outcomes for template %d for user %s", list.size(), templateId, userId);
    return list;
  }

  @POST
  public OutcomeDto create(@PathParam("userId") String userId, @PathParam("templateId") Long templateId,
      @Valid OutcomeDto dto) {
    authorize(userId);
    LOG.debugf("Creating outcome for template %d for user %s", templateId, userId);
    Outcome outcome = fromDto(dto);
    Outcome saved = service.create(userId, templateId, outcome);
    LOG.infof("Created outcome for template %d for user %s", templateId, userId);
    return toDto(saved);
  }

  @PUT
  @Path("/{id}")
  public OutcomeDto update(@PathParam("userId") String userId, @PathParam("templateId") Long templateId,
      @PathParam("id") Long id, @Valid OutcomeDto dto) {
    authorize(userId);
    LOG.debugf("Updating outcome %d for template %d for user %s", id, templateId, userId);
    Outcome outcome = fromDto(dto);
    Outcome updated = service.update(userId, templateId, id, outcome);
    LOG.infof("Updated outcome %d for template %d for user %s", id, templateId, userId);
    return toDto(updated);
  }

  @DELETE
  @Path("/{id}")
  public void delete(@PathParam("userId") String userId, @PathParam("templateId") Long templateId,
      @PathParam("id") Long id) {
    authorize(userId);
    LOG.debugf("Deleting outcome %d for template %d for user %s", id, templateId, userId);
    service.delete(userId, templateId, id);
    LOG.infof("Deleted outcome %d for template %d for user %s", id, templateId, userId);
  }

  @DELETE
  public void deleteAll(@PathParam("userId") String userId, @PathParam("templateId") Long templateId) {
    authorize(userId);
    LOG.debugf("Deleting all outcomes for template %d for user %s", templateId, userId);
    service.deleteAllByTemplate(userId, templateId);
    LOG.infof("Deleted all outcomes for template %d for user %s", templateId, userId);
  }

  private OutcomeDto toDto(Outcome o) {
    OutcomeDto dto = new OutcomeDto();
    dto.id = null; // No database ID in Git-based storage
    dto.phase = new OutcomeDto.PhaseDto();
    dto.phase.name = o.phase.name;
    dto.phase.track = o.phase.track;
    dto.phase.product = o.phase.product;
    dto.phase.environment = o.phase.environment;
    dto.prefix = o.prefix;
    dto.description = o.description;
    dto.notes = o.notes;
    return dto;
  }

  private Outcome fromDto(OutcomeDto dto) {
    Outcome o = new Outcome();
    o.phase = new Outcome.Phase();
    o.phase.name = dto.phase.name;
    o.phase.track = dto.phase.track;
    o.phase.product = dto.phase.product;
    o.phase.environment = dto.phase.environment;
    o.prefix = dto.prefix;
    o.description = dto.description;
    o.notes = dto.notes;
    return o;
  }
}
