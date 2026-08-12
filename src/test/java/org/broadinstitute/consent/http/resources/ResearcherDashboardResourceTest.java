package org.broadinstitute.consent.http.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.core.Response;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.DashboardSummary.DarRequests;
import org.broadinstitute.consent.http.models.DashboardSummary.DataLibrary;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.models.ResearcherDashboardSummary;
import org.broadinstitute.consent.http.models.ResearcherDashboardSummary.DataSubmissions;
import org.broadinstitute.consent.http.models.ResearcherDashboardSummary.DatasetApprovals;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.ResearcherDashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResearcherDashboardResourceTest {

  @Mock private ResearcherDashboardService dashboardService;

  private ResearcherDashboardResource resource;
  private DuosUser duosUser;
  private User user;

  @BeforeEach
  void setUp() {
    resource = new ResearcherDashboardResource(dashboardService);
    user = new User();
    user.setEmail("researcher@example.org");
    duosUser = new DuosUser(new AuthUser("researcher@example.org"), user);
  }

  @Test
  void returnsSummary() {
    ResearcherDashboardSummary summary =
        new ResearcherDashboardSummary(
            new DataLibrary(1, 2, 3, 4),
            new DarRequests(5, 2, 1, 2),
            new DatasetApprovals(6, 2, 3),
            new DataSubmissions(7));
    when(dashboardService.getSummary(user)).thenReturn(summary);

    Response response = resource.getDashboardSummary(duosUser);

    assertEquals(200, response.getStatus());
    assertEquals(summary, response.getEntity());
  }

  @Test
  void mapsAggregationFailureToServerError() {
    when(dashboardService.getSummary(user)).thenThrow(new IllegalStateException("failed"));

    Response response = resource.getDashboardSummary(duosUser);

    assertEquals(500, response.getStatus());
  }
}
