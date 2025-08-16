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

@Path("/api/users/{userId}/templates")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TemplateResource {

  @Inject TemplateService service;

  @GET
  public List<TemplateDto> list(@PathParam("userId") String userId) {
    return service.list(userId).stream().map(this::toDto).collect(Collectors.toList());
  }

  @POST
  public TemplateDto create(@PathParam("userId") String userId, @Valid TemplateDto dto) {
    Template tmpl = fromDto(dto);
    Template saved = service.create(userId, tmpl);
    return toDto(saved);
  }

  @PUT
  @Path("/{id}")
  public TemplateDto update(@PathParam("userId") String userId, @PathParam("id") Long id, @Valid TemplateDto dto) {
    Template tmpl = fromDto(dto);
    Template updated = service.update(userId, id, tmpl);
    return toDto(updated);
  }

  @DELETE
  @Path("/{id}")
  public void delete(@PathParam("userId") String userId, @PathParam("id") Long id) {
    service.delete(userId, id);
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
