package org.broadinstitute.consent.http.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.core.Response;
import org.broadinstitute.consent.http.models.AdminDashboardSummary;
import org.broadinstitute.consent.http.models.AdminDashboardSummary.Dacs;
import org.broadinstitute.consent.http.models.AdminDashboardSummary.LibraryCards;
import org.broadinstitute.consent.http.models.AdminDashboardSummary.Users;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.DashboardSummary.DarRequests;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.AdminDashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminDashboardResourceTest {
  @Mock private AdminDashboardService dashboardService;

  private AdminDashboardResource resource;
  private DuosUser duosUser;

  @BeforeEach
  void setUp() {
    resource = new AdminDashboardResource(dashboardService);
    duosUser = new DuosUser(new AuthUser("admin@example.org"), new User());
  }

  @Test
  void returnsSummary() {
    AdminDashboardSummary summary =
        new AdminDashboardSummary(
            new DarRequests(3, 1, 1, 1), new Dacs(2), new Users(9), new LibraryCards(5));
    when(dashboardService.getSummary()).thenReturn(summary);

    Response response = resource.getDashboardSummary(duosUser);

    assertEquals(200, response.getStatus());
    assertEquals(summary, response.getEntity());
  }

  @Test
  void mapsAggregationFailureToServerError() {
    when(dashboardService.getSummary()).thenThrow(new IllegalStateException("failed"));

    assertEquals(500, resource.getDashboardSummary(duosUser).getStatus());
  }
}
