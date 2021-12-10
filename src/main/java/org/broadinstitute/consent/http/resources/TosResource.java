package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;
import org.broadinstitute.consent.http.service.sam.SamService;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

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
