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
import org.broadinstitute.consent.http.service.AdminDashboardService;

@Path("api/admin/dashboard-summary")
public class AdminDashboardResource extends Resource {
  private final AdminDashboardService dashboardService;

  @Inject
  public AdminDashboardResource(AdminDashboardService dashboardService) {
    this.dashboardService = dashboardService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed(ADMIN)
  @Timed
  public Response getDashboardSummary(@Auth DuosUser duosUser) {
    try {
      return Response.ok(dashboardService.getSummary()).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }
}
