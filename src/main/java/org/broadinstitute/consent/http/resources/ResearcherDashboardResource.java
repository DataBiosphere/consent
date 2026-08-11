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
import org.broadinstitute.consent.http.service.ResearcherDashboardService;

@Path("api/researcher/dashboard-summary")
public class ResearcherDashboardResource extends Resource {

  private final ResearcherDashboardService dashboardService;

  @Inject
  public ResearcherDashboardResource(ResearcherDashboardService dashboardService) {
    this.dashboardService = dashboardService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed(RESEARCHER)
  @Timed
  public Response getDashboardSummary(@Auth DuosUser duosUser) {
    try {
      return Response.ok(dashboardService.getSummary(duosUser.getUser())).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }
}
