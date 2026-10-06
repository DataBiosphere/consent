package org.broadinstitute.consent.http.service;

import static org.broadinstitute.consent.http.service.DashboardServiceSupport.join;

import com.google.inject.Inject;
import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import org.broadinstitute.consent.http.db.DacDashboardDAO;
import org.broadinstitute.consent.http.db.DacDashboardDAO.DashboardDatabaseCounts;
import org.broadinstitute.consent.http.db.DarMetricsDAO;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.DacDashboardSummary;
import org.broadinstitute.consent.http.models.DacDashboardSummary.DacDatasets;
import org.broadinstitute.consent.http.models.DacDashboardSummary.Dacs;
import org.broadinstitute.consent.http.models.DacDashboardSummary.DarRequests;
import org.broadinstitute.consent.http.models.DashboardMetrics;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.service.DashboardSearchService.DacSearchCounts;
import org.jdbi.v3.core.Jdbi;

public class DacDashboardService {

  private final DacDashboardDAO dashboardDAO;
  private final DashboardSearchService dashboardSearchService;
  private final ExecutorService executorService;
  private final DashboardMetricsReader dashboardMetrics;

  @Inject
  public DacDashboardService(
      Jdbi jdbi,
      DashboardSearchService dashboardSearchService,
      ExecutorService executorService,
      Clock clock) {
    this.dashboardDAO = jdbi.onDemand(DacDashboardDAO.class);
    this.dashboardMetrics = new DashboardMetricsReader(jdbi.onDemand(DarMetricsDAO.class), clock);
    this.dashboardSearchService = dashboardSearchService;
    this.executorService = executorService;
  }

  public DacDashboardSummary getSummary(User user) {
    boolean isChair = user.hasUserRole(UserRoles.CHAIRPERSON);
    List<Integer> dacIds = getDacIds(user);

    CompletableFuture<DashboardDatabaseCounts> databaseCounts =
        CompletableFuture.supplyAsync(
            () ->
                dashboardDAO.getCounts(
                    user.getUserId(),
                    UserRoles.CHAIRPERSON.getRoleId(),
                    UserRoles.MEMBER.getRoleId()),
            executorService);
    CompletableFuture<DacSearchCounts> searchCounts =
        CompletableFuture.supplyAsync(
            () -> dashboardSearchService.getDacSearchCounts(isChair, dacIds), executorService);
    // The DAC Console's Metrics page has no SO approvals tab, so neither does its dashboard.
    CompletableFuture<DashboardMetrics> metrics =
        CompletableFuture.supplyAsync(() -> dashboardMetrics.read(dacIds, false), executorService);

    DashboardDatabaseCounts db = join(databaseCounts);
    DacSearchCounts search = join(searchCounts);
    long pending = db.darTotal() - db.darApproved();
    return new DacDashboardSummary(
        new DarRequests(db.darTotal(), db.darApproved(), pending, db.awaitingMyVote()),
        new Dacs(isChair ? db.dacs() : 0),
        new DacDatasets(search.dacDatasets()),
        search.dataLibrary(),
        join(metrics));
  }

  private List<Integer> getDacIds(User user) {
    if (user.getRoles() == null) {
      return List.of();
    }
    return user.getRoles().stream()
        .map(UserRole::getDacId)
        .filter(Objects::nonNull)
        .distinct()
        .toList();
  }
}
