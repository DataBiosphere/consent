package org.broadinstitute.consent.http.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.models.SigningOfficialDashboardSummary;
import org.broadinstitute.consent.http.models.SigningOfficialDashboardSummary.DaaAssociations;
import org.broadinstitute.consent.http.models.SigningOfficialDashboardSummary.DarApprovals;
import org.broadinstitute.consent.http.models.SigningOfficialDashboardSummary.DarRequests;
import org.broadinstitute.consent.http.models.SigningOfficialDashboardSummary.DataSubmitters;
import org.broadinstitute.consent.http.models.SigningOfficialDashboardSummary.InstitutionLibrary;
import org.broadinstitute.consent.http.models.SigningOfficialDashboardSummary.ResearcherStatus;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.SigningOfficialDashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SigningOfficialDashboardResourceTest {
  @Mock private SigningOfficialDashboardService dashboardService;

  private SigningOfficialDashboardResource resource;
  private DuosUser duosUser;
  private User user;

  @BeforeEach
  void setUp() {
    resource = new SigningOfficialDashboardResource(dashboardService);
    user = new User();
    user.setEmail("so@example.org");
    duosUser = new DuosUser(new AuthUser("so@example.org"), user);
  }

  @Test
  void returnsSummary() {
    SigningOfficialDashboardSummary summary =
        new SigningOfficialDashboardSummary(
            new ResearcherStatus(1, 2), new DarRequests(3, 1, 1, 1),
            new DarApprovals(2, 1), new DataSubmitters(4),
            new InstitutionLibrary(5, 6), new DaaAssociations(7, 8));
    when(dashboardService.getSummary(user)).thenReturn(summary);

    Response response = resource.getDashboardSummary(duosUser);

    assertEquals(200, response.getStatus());
    assertEquals(summary, response.getEntity());
  }

  @Test
  void mapsMissingInstitutionToBadRequest() {
    when(dashboardService.getSummary(user)).thenThrow(new BadRequestException("missing"));
    Response response = resource.getDashboardSummary(duosUser);
    assertEquals(400, response.getStatus());
  }

  @Test
  void mapsAggregationFailureToServerError() {
    when(dashboardService.getSummary(user)).thenThrow(new IllegalStateException("failed"));
    Response response = resource.getDashboardSummary(duosUser);
    assertEquals(500, response.getStatus());
  }
}
