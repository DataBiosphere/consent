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
import org.broadinstitute.consent.http.service.SigningOfficialDashboardService;

@Path("api/signing-official/dashboard-summary")
public class SigningOfficialDashboardResource extends Resource {
  private final SigningOfficialDashboardService dashboardService;

  @Inject
  public SigningOfficialDashboardResource(SigningOfficialDashboardService dashboardService) {
    this.dashboardService = dashboardService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed(SIGNINGOFFICIAL)
  @Timed
  public Response getDashboardSummary(@Auth DuosUser duosUser) {
    try {
      return Response.ok(dashboardService.getSummary(duosUser.getUser())).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }
}
