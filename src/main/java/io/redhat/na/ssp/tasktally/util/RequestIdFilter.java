package io.redhat.na.ssp.tasktally.util;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.MDC;

import java.io.IOException;
import java.util.UUID;

@Provider
@PreMatching
public class RequestIdFilter implements ContainerRequestFilter, ContainerResponseFilter {
  private static final String HEADER = "X-Request-Id";

  @Override
  public void filter(ContainerRequestContext requestContext) throws IOException {
    String id = requestContext.getHeaderString(HEADER);
    if (id == null || id.isBlank()) {
      id = UUID.randomUUID().toString();
    }
    requestContext.setProperty(HEADER, id);
    MDC.put("requestId", id);
  }

  @Override
  public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
      throws IOException {
    Object id = requestContext.getProperty(HEADER);
    if (id != null) {
      responseContext.getHeaders().putSingle(HEADER, id.toString());
    }
    MDC.remove("requestId");
  }
}
