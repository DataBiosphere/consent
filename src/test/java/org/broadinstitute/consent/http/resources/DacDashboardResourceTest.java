package org.broadinstitute.consent.http.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.core.Response;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.DacDashboardSummary;
import org.broadinstitute.consent.http.models.DacDashboardSummary.DacDatasets;
import org.broadinstitute.consent.http.models.DacDashboardSummary.Dacs;
import org.broadinstitute.consent.http.models.DacDashboardSummary.DarRequests;
import org.broadinstitute.consent.http.models.DashboardSummary.DataLibrary;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.DacDashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DacDashboardResourceTest {

  @Mock private DacDashboardService dashboardService;

  private DacDashboardResource resource;
  private DuosUser duosUser;
  private User user;

  @BeforeEach
  void setUp() {
    resource = new DacDashboardResource(dashboardService);
    user = new User();
    user.setEmail("dac-user@example.org");
    duosUser = new DuosUser(new AuthUser("dac-user@example.org"), user);
  }

  @Test
  void returnsSummary() {
    DacDashboardSummary summary =
        new DacDashboardSummary(
            new DarRequests(8, 3, 5, 2),
            new Dacs(4),
            new DacDatasets(6),
            new DataLibrary(7, 8, 9, 10));
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
