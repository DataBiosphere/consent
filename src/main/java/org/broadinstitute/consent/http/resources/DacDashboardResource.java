package org.broadinstitute.consent.http.resources;

import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.service.DacDashboardService;

@Path("api/dac/dashboard-summary")
public class DacDashboardResource extends Resource {

  private final DacDashboardService dashboardService;

  @Inject
  public DacDashboardResource(DacDashboardService dashboardService) {
    this.dashboardService = dashboardService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed({CHAIRPERSON, MEMBER})
  @Timed
  public Response getDashboardSummary(@Auth DuosUser duosUser) {
    try {
      return Response.ok(dashboardService.getSummary(duosUser.getUser())).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }
}
