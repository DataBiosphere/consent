package org.broadinstitute.consent.http.service;

import com.google.inject.Inject;
import java.time.Clock;
import java.util.List;
import org.broadinstitute.consent.http.db.AdminDashboardDAO;
import org.broadinstitute.consent.http.db.AdminDashboardDAO.DashboardDatabaseCounts;
import org.broadinstitute.consent.http.db.DarMetricsDAO;
import org.broadinstitute.consent.http.models.AdminDashboardSummary;
import org.broadinstitute.consent.http.models.AdminDashboardSummary.DaaAssociations;
import org.broadinstitute.consent.http.models.AdminDashboardSummary.Dacs;
import org.broadinstitute.consent.http.models.AdminDashboardSummary.Institutions;
import org.broadinstitute.consent.http.models.AdminDashboardSummary.LibraryCards;
import org.broadinstitute.consent.http.models.AdminDashboardSummary.Users;
import org.broadinstitute.consent.http.models.DashboardSummary.DarRequests;
import org.jdbi.v3.core.Jdbi;

public class AdminDashboardService {
  // null reads every DAC's datasets; see DarMetricsDAO.IN_DAC_SCOPE
  private static final List<Integer> ALL_DACS = null;

  private final AdminDashboardDAO dashboardDAO;
  private final DashboardMetricsReader dashboardMetrics;

  @Inject
  public AdminDashboardService(Jdbi jdbi, Clock clock) {
    this.dashboardDAO = jdbi.onDemand(AdminDashboardDAO.class);
    this.dashboardMetrics = new DashboardMetricsReader(jdbi.onDemand(DarMetricsDAO.class), clock);
  }

  public AdminDashboardSummary getSummary() {
    DashboardDatabaseCounts db = dashboardDAO.getCounts();
    long inProcess = db.darTotal() - db.darApproved() - db.darCanceled();
    return new AdminDashboardSummary(
        new DarRequests(db.darTotal(), db.darApproved(), db.darCanceled(), inProcess),
        new Dacs(db.dacs()),
        new Users(db.users()),
        new Institutions(db.institutions(), db.institutionsWithoutSigningOfficial()),
        new LibraryCards(db.libraryCards()),
        new DaaAssociations(db.agreements(), db.researchersApproved()),
        dashboardMetrics.read(ALL_DACS, true));
  }
}
