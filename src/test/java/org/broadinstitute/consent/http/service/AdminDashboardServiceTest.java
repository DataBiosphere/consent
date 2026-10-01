package org.broadinstitute.consent.http.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import org.broadinstitute.consent.http.db.AdminDashboardDAO;
import org.broadinstitute.consent.http.db.AdminDashboardDAO.DashboardDatabaseCounts;
import org.broadinstitute.consent.http.models.AdminDashboardSummary;
import org.broadinstitute.consent.http.models.AdminDashboardSummary.Dacs;
import org.broadinstitute.consent.http.models.AdminDashboardSummary.LibraryCards;
import org.broadinstitute.consent.http.models.AdminDashboardSummary.Users;
import org.broadinstitute.consent.http.models.DashboardSummary.DarRequests;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceTest {
  @Mock private Jdbi jdbi;
  @Mock private AdminDashboardDAO dashboardDAO;

  @Test
  void mapsCountsAndDerivesInProcess() {
    when(jdbi.onDemand(AdminDashboardDAO.class)).thenReturn(dashboardDAO);
    when(dashboardDAO.getCounts()).thenReturn(new DashboardDatabaseCounts(10, 4, 2, 3, 50, 30));

    AdminDashboardSummary summary = new AdminDashboardService(jdbi).getSummary();

    assertEquals(
        new AdminDashboardSummary(
            new DarRequests(10, 4, 2, 4), new Dacs(3), new Users(50), new LibraryCards(30)),
        summary);
  }
}
