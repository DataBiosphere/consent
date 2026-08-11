package org.broadinstitute.consent.http.service;

import static org.broadinstitute.consent.http.service.DashboardServiceSupport.join;

import com.google.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import org.broadinstitute.consent.http.db.SigningOfficialDashboardDAO;
import org.broadinstitute.consent.http.db.SigningOfficialDashboardDAO.DashboardDatabaseCounts;
import org.broadinstitute.consent.http.models.DashboardSummary.DarRequests;
import org.broadinstitute.consent.http.models.SigningOfficialDashboardSummary;
import org.broadinstitute.consent.http.models.SigningOfficialDashboardSummary.DaaAssociations;
import org.broadinstitute.consent.http.models.SigningOfficialDashboardSummary.DarApprovals;
import org.broadinstitute.consent.http.models.SigningOfficialDashboardSummary.DataSubmitters;
import org.broadinstitute.consent.http.models.SigningOfficialDashboardSummary.ResearcherStatus;
import org.broadinstitute.consent.http.models.User;
import org.jdbi.v3.core.Jdbi;

public class SigningOfficialDashboardService {
  private final SigningOfficialDashboardDAO dashboardDAO;
  private final DashboardSearchService dashboardSearchService;
  private final ExecutorService executorService;

  @Inject
  public SigningOfficialDashboardService(
      Jdbi jdbi, DashboardSearchService dashboardSearchService, ExecutorService executorService) {
    this.dashboardDAO = jdbi.onDemand(SigningOfficialDashboardDAO.class);
    this.dashboardSearchService = dashboardSearchService;
    this.executorService = executorService;
  }

  public SigningOfficialDashboardSummary getSummary(User user) {
    if (user.getInstitutionId() == null) {
      throw new BadRequestException("Signing Official is not associated with an institution.");
    }
    CompletableFuture<DashboardDatabaseCounts> databaseCounts =
        CompletableFuture.supplyAsync(
            () ->
                dashboardDAO.getCounts(
                    user.getInstitutionId(), user.getUserId().toString(), user.getEmail()),
            executorService);
    var libraryCounts =
        CompletableFuture.supplyAsync(
            () -> dashboardSearchService.getInstitutionLibraryCounts(user.getInstitutionId()),
            executorService);

    DashboardDatabaseCounts db = join(databaseCounts);
    long inProcess = db.darTotal() - db.darApproved() - db.darCanceled();
    return new SigningOfficialDashboardSummary(
        new ResearcherStatus(db.activeResearchers(), db.inactiveResearchers()),
        new DarRequests(db.darTotal(), db.darApproved(), db.darCanceled(), inProcess),
        new DarApprovals(db.approvalTotal(), db.awaitingSoAction()),
        new DataSubmitters(db.approvedDataSubmitters()),
        join(libraryCounts),
        new DaaAssociations(db.agreements(), db.researchersApproved()));
  }
}
