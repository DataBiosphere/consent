package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;
import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.broadinstitute.consent.http.service.DataAccessRequestService;

@Path("api/dataRequest")
public class DataRequestReportsResource extends Resource {

  private final DataAccessRequestService darService;

  @Inject
  public DataRequestReportsResource(DataAccessRequestService darService) {
    this.darService = darService;
  }

  @GET
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  @PermitAll
  @Path("/approved")
  public Response downloadApprovedDARs() {
    try {
      return Response.ok(darService.createApprovedDARDocument())
          .header(HttpHeaders.CONTENT_DISPOSITION,
              "attachment; filename=" + "ApprovedDataAccessRequests.tsv")
          .build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @GET
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  @PermitAll
  @Path("/reviewed")
  public Response downloadReviewedDARs() {
    try {
      return Response.ok(darService.createReviewedDARDocument())
          .header(HttpHeaders.CONTENT_DISPOSITION,
              "attachment; filename=" + "ReviewedDataAccessRequests.tsv")
          .build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

}
