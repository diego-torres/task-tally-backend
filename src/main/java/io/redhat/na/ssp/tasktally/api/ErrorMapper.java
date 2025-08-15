package io.redhat.na.ssp.tasktally.api;

import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.MDC;

import java.util.HashMap;
import java.util.Map;

@Provider
public class ErrorMapper implements ExceptionMapper<Throwable> {
  @Override
  public Response toResponse(Throwable throwable) {
    int status = 500;
    String code = "INTERNAL_ERROR";
    String message = "";
    if (throwable instanceof ConstraintViolationException cve) {
      status = 400;
      code = "VALIDATION_ERROR";
      message = cve.getConstraintViolations().iterator().next().getMessage();
    } else if (throwable instanceof WebApplicationException wae) {
      status = wae.getResponse().getStatus();
      code = "HTTP_" + status;
      message = wae.getMessage();
    } else {
      message = throwable.getMessage();
    }
    Map<String, String> err = new HashMap<>();
    err.put("code", code);
    err.put("message", message == null ? "" : message);
    err.put("requestId", String.valueOf(MDC.get("requestId")));
    return Response.status(status).type(MediaType.APPLICATION_JSON).entity(err).build();
  }
}
