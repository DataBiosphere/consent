package org.broadinstitute.consent.http.resources;

import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import java.util.Objects;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.models.tdr.ApprovedUsers;
import org.broadinstitute.consent.http.service.DatasetService;
import org.broadinstitute.consent.http.service.TDRService;


@Path("api/tdr")
public class TDRResource extends Resource {

  private final TDRService tdrService;
  private final DatasetService datasetService;


  @Inject
  public TDRResource(TDRService tdrService, DatasetService datasetService) {
    this.datasetService = datasetService;
    this.tdrService = tdrService;
  }

  @GET
  @Produces("application/json")
  @RolesAllowed({ADMIN, SERVICE_ACCOUNT})
  @Path("/{identifier}/approved/users")
  @Timed
  public Response getApprovedUsers(@Auth DuosUser duosUser,
      @PathParam("identifier") String identifier) {
    try {
      Dataset dataset = datasetService.findMinimalDatasetByIdentifier(duosUser.getUser(), identifier);
      if (Objects.isNull(dataset)) {
        throw new NotFoundException("Could not find dataset " + identifier);
      }

      ApprovedUsers approvedUsers = tdrService.getApprovedUsersForDataset(duosUser, dataset);
      return Response.ok(approvedUsers).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @GET
  @Produces("application/json")
  @PermitAll
  @Path("/{identifier}")
  @Timed
  public Response getDatasetByIdentifier(@Auth DuosUser duosUser,
      @PathParam("identifier") String identifier) {
    try {
      Dataset dataset = datasetService.findMinimalDatasetByIdentifier(duosUser.getUser(), identifier);
      if (Objects.isNull(dataset)) {
        throw new NotFoundException("Could not find dataset " + identifier);
      }

      return Response.ok(unmarshal(dataset)).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

}
