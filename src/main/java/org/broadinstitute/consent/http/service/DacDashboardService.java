package org.broadinstitute.consent.http.service;

import static org.broadinstitute.consent.http.service.DashboardServiceSupport.join;

import com.google.inject.Inject;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import org.broadinstitute.consent.http.db.DacDashboardDAO;
import org.broadinstitute.consent.http.db.DacDashboardDAO.DashboardDatabaseCounts;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.DacDashboardSummary;
import org.broadinstitute.consent.http.models.DacDashboardSummary.DacDatasets;
import org.broadinstitute.consent.http.models.DacDashboardSummary.Dacs;
import org.broadinstitute.consent.http.models.DacDashboardSummary.DarRequests;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.service.DashboardSearchService.DacSearchCounts;
import org.jdbi.v3.core.Jdbi;

public class DacDashboardService {

  private final DacDashboardDAO dashboardDAO;
  private final DashboardSearchService dashboardSearchService;
  private final ExecutorService executorService;

  @Inject
  public DacDashboardService(
      Jdbi jdbi, DashboardSearchService dashboardSearchService, ExecutorService executorService) {
    this.dashboardDAO = jdbi.onDemand(DacDashboardDAO.class);
    this.dashboardSearchService = dashboardSearchService;
    this.executorService = executorService;
  }

  public DacDashboardSummary getSummary(User user) {
    boolean isChair = user.hasUserRole(UserRoles.CHAIRPERSON);
    int roleId = isChair ? UserRoles.CHAIRPERSON.getRoleId() : UserRoles.MEMBER.getRoleId();
    List<Integer> dacIds = getDacIds(user);

    CompletableFuture<DashboardDatabaseCounts> databaseCounts =
        CompletableFuture.supplyAsync(
            () -> dashboardDAO.getCounts(user.getUserId(), roleId, isChair), executorService);
    CompletableFuture<DacSearchCounts> searchCounts =
        CompletableFuture.supplyAsync(
            () -> dashboardSearchService.getDacSearchCounts(isChair, dacIds), executorService);

    DashboardDatabaseCounts db = join(databaseCounts);
    DacSearchCounts search = join(searchCounts);
    long pending = db.darTotal() - db.darApproved();
    return new DacDashboardSummary(
        new DarRequests(db.darTotal(), db.darApproved(), pending, db.awaitingMyVote()),
        new Dacs(isChair ? db.dacs() : 0),
        new DacDatasets(search.dacDatasets()),
        search.dataLibrary());
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
