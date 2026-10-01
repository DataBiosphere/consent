package org.broadinstitute.consent.http.service;

import com.google.inject.Inject;
import org.broadinstitute.consent.http.db.AdminDashboardDAO;
import org.broadinstitute.consent.http.db.AdminDashboardDAO.DashboardDatabaseCounts;
import org.broadinstitute.consent.http.models.AdminDashboardSummary;
import org.broadinstitute.consent.http.models.AdminDashboardSummary.Dacs;
import org.broadinstitute.consent.http.models.AdminDashboardSummary.LibraryCards;
import org.broadinstitute.consent.http.models.AdminDashboardSummary.Users;
import org.broadinstitute.consent.http.models.DashboardSummary.DarRequests;
import org.jdbi.v3.core.Jdbi;

public class AdminDashboardService {
  private final AdminDashboardDAO dashboardDAO;

  @Inject
  public AdminDashboardService(Jdbi jdbi) {
    this.dashboardDAO = jdbi.onDemand(AdminDashboardDAO.class);
  }

  public AdminDashboardSummary getSummary() {
    DashboardDatabaseCounts db = dashboardDAO.getCounts();
    long inProcess = db.darTotal() - db.darApproved() - db.darCanceled();
    return new AdminDashboardSummary(
        new DarRequests(db.darTotal(), db.darApproved(), db.darCanceled(), inProcess),
        new Dacs(db.dacs()),
        new Users(db.users()),
        new LibraryCards(db.libraryCards()));
  }
}
