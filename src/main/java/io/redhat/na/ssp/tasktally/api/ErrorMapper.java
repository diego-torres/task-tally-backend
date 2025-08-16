package io.redhat.na.ssp.tasktally.api;

import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;
import org.jboss.logging.MDC;

import java.util.HashMap;
import java.util.Map;

@Provider
public class ErrorMapper implements ExceptionMapper<Throwable> {
  private static final Logger LOG = Logger.getLogger(ErrorMapper.class);

  @Override
  public Response toResponse(Throwable throwable) {
    LOG.debug("Mapping exception to response", throwable);
    int status = 500;
    String code = "INTERNAL_ERROR";
    String message = "";
    if (throwable instanceof ConstraintViolationException cve) {
      status = 400;
      code = "VALIDATION_ERROR";
      message = cve.getConstraintViolations().iterator().next().getMessage();
      LOG.warn("Validation error", cve);
    } else if (throwable instanceof WebApplicationException wae) {
      status = wae.getResponse().getStatus();
      code = "HTTP_" + status;
      message = wae.getMessage();
      LOG.warn("HTTP error", wae);
    } else {
      message = throwable.getMessage();
      LOG.error("Unhandled error", throwable);
    }
    Map<String, String> err = new HashMap<>();
    err.put("code", code);
    err.put("message", message == null ? "" : message);
    err.put("requestId", String.valueOf(MDC.get("requestId")));
    LOG.infof("Returning error response %d with code %s", status, code);
    return Response.status(status).type(MediaType.APPLICATION_JSON).entity(err).build();
  }
}
