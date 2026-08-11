package org.broadinstitute.consent.http.service;

import static org.broadinstitute.consent.http.service.DashboardServiceSupport.join;

import com.google.inject.Inject;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import org.broadinstitute.consent.http.db.ResearcherDashboardDAO;
import org.broadinstitute.consent.http.db.ResearcherDashboardDAO.DashboardDatabaseCounts;
import org.broadinstitute.consent.http.models.DashboardSummary.DarRequests;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.ResearcherDashboardSummary;
import org.broadinstitute.consent.http.models.ResearcherDashboardSummary.DataSubmissions;
import org.broadinstitute.consent.http.models.ResearcherDashboardSummary.DatasetApprovals;
import org.broadinstitute.consent.http.models.User;
import org.jdbi.v3.core.Jdbi;

public class ResearcherDashboardService {

  private static final int EXPIRING_SOON_DAYS = 30;
  private static final int EXPIRATION_DAYS =
      (int) TimeUnit.MILLISECONDS.toDays(DataAccessRequest.EXPIRATION_DURATION_MILLIS);

  private final ResearcherDashboardDAO dashboardDAO;
  private final DashboardSearchService dashboardSearchService;
  private final ExecutorService executorService;

  @Inject
  public ResearcherDashboardService(
      Jdbi jdbi, DashboardSearchService dashboardSearchService, ExecutorService executorService) {
    this.dashboardDAO = jdbi.onDemand(ResearcherDashboardDAO.class);
    this.dashboardSearchService = dashboardSearchService;
    this.executorService = executorService;
  }

  public ResearcherDashboardSummary getSummary(User user) {
    CompletableFuture<DashboardDatabaseCounts> databaseCounts =
        CompletableFuture.supplyAsync(
            () -> dashboardDAO.getCounts(user.getUserId(), EXPIRATION_DAYS, EXPIRING_SOON_DAYS),
            executorService);
    var libraryCounts =
        CompletableFuture.supplyAsync(
            () -> dashboardSearchService.getDataLibraryCounts(user), executorService);
    CompletableFuture<Long> submissionCount =
        CompletableFuture.supplyAsync(
            () -> dashboardSearchService.getDataSubmissionCount(user), executorService);

    DashboardDatabaseCounts db = join(databaseCounts);
    long inProcess = db.darTotal() - db.darApproved() - db.darCanceled();
    return new ResearcherDashboardSummary(
        join(libraryCounts),
        new DarRequests(db.darTotal(), db.darApproved(), db.darCanceled(), inProcess),
        new DatasetApprovals(
            db.approvalsActive(), db.approvalsExpiringSoon(), db.approvalsExpired()),
        new DataSubmissions(join(submissionCount)));
  }
}
