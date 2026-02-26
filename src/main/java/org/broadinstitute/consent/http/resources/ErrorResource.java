package org.broadinstitute.consent.http.resources;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import org.broadinstitute.consent.http.models.Error;
import org.eclipse.jetty.ee10.servlet.ServletApiRequest;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler.ServletRequestInfo;

@Path("error")
public class ErrorResource {

  /**
   * Explanatory note about this 404 handler: In order to provide the original URI that resulted in
   * a 404, we need to access the underlying request information. The HttpServletRequest passed to
   * this method is actually a wrapper around the original request. By unwrapping it to get to the
   * ServletApiRequest, we can retrieve the original URI that was requested. This allows us to
   * construct a more informative error message for the client.
   *
   * @param httpServletRequest The HttpServletRequest
   * @return Response
   */
  @GET
  @Path("404")
  @Produces("application/json")
  public Response notFound(@Context HttpServletRequest httpServletRequest) {
    try {
      HttpServletRequestWrapper requestWrapper = (HttpServletRequestWrapper) httpServletRequest;
      ServletRequest servletRequest = requestWrapper.getRequest();
      ServletApiRequest servletApiRequest = (ServletApiRequest) servletRequest;
      ServletRequestInfo servletRequestInfo = servletApiRequest.getServletRequestInfo();
      String originalUri = servletRequestInfo.getDecodedPathInContext();
      String decodedUri = URLDecoder.decode(originalUri, Charset.defaultCharset());
      String msg = String.format("Unable to find requested path: %s", decodedUri);
      Error error = new Error(msg, Response.Status.NOT_FOUND.getStatusCode());
      return Response.status(Response.Status.NOT_FOUND).entity(error).build();
    } catch (Exception e) {
      return Response.status(Response.Status.NOT_FOUND).build();
    }
  }
}
