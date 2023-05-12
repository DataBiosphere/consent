package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import org.broadinstitute.consent.http.service.sam.SamService;

@Path("tos")
public class TosResource extends Resource {

  private final SamService samService;

  @Inject
  public TosResource(SamService samService) {
    this.samService = samService;
  }

  @Path("text")
  @GET
  @Produces("text/plain")
  public Response getToSText() {
    try {
      String text = samService.getToSText();
      return Response.ok().entity(text).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @Path("text/duos")
  @GET
  @Produces("text/plain")
  public Response getDUOSToSText() {
    try {
      String text = samService.getToSText();
      String duos = text.replaceAll("Terra", "Terra/DUOS");
      return Response.ok().entity(duos).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }
}
