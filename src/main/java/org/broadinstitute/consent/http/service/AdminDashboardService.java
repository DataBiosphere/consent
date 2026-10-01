package org.broadinstitute.consent.http.service;

import com.google.inject.Inject;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.ToLongFunction;
import org.broadinstitute.consent.http.db.AdminDashboardDAO;
import org.broadinstitute.consent.http.db.AdminDashboardDAO.DashboardDatabaseCounts;
import org.broadinstitute.consent.http.db.DarMetricsDAO;
import org.broadinstitute.consent.http.enumeration.AccessEndReason;
import org.broadinstitute.consent.http.enumeration.DecisionState;
import org.broadinstitute.consent.http.enumeration.SoApprovalStatus;
import org.broadinstitute.consent.http.models.AdminDashboardSummary;
import org.broadinstitute.consent.http.models.AdminDashboardSummary.DaaAssociations;
import org.broadinstitute.consent.http.models.AdminDashboardSummary.Dacs;
import org.broadinstitute.consent.http.models.AdminDashboardSummary.Decisions;
import org.broadinstitute.consent.http.models.AdminDashboardSummary.Expiration;
import org.broadinstitute.consent.http.models.AdminDashboardSummary.Institutions;
import org.broadinstitute.consent.http.models.AdminDashboardSummary.LibraryCards;
import org.broadinstitute.consent.http.models.AdminDashboardSummary.Metrics;
import org.broadinstitute.consent.http.models.AdminDashboardSummary.SoApprovals;
import org.broadinstitute.consent.http.models.AdminDashboardSummary.Turnaround;
import org.broadinstitute.consent.http.models.AdminDashboardSummary.Users;
import org.broadinstitute.consent.http.models.AdminDashboardSummary.Volume;
import org.broadinstitute.consent.http.models.DashboardSummary.DarRequests;
import org.broadinstitute.consent.http.models.DecisionBucketCount;
import org.broadinstitute.consent.http.models.ExpirationBucket;
import org.broadinstitute.consent.http.models.RenewalBucket;
import org.broadinstitute.consent.http.models.SoApprovalBucket;
import org.broadinstitute.consent.http.models.TurnaroundBucket;
import org.broadinstitute.consent.http.models.VolumeBucketCount;
import org.jdbi.v3.core.Jdbi;

public class AdminDashboardService {
  static final int METRICS_WINDOW_DAYS = 90;

  // date_trunc to a millennium puts the whole window in one bucket, so the reporting queries
  // return window-wide medians and distinct counts rather than per-bucket ones.
  private static final String WHOLE_WINDOW = "millennium";

  private final AdminDashboardDAO dashboardDAO;
  private final DarMetricsDAO darMetricsDAO;
  private final Clock clock;

  @Inject
  public AdminDashboardService(Jdbi jdbi, Clock clock) {
    this.dashboardDAO = jdbi.onDemand(AdminDashboardDAO.class);
    this.darMetricsDAO = jdbi.onDemand(DarMetricsDAO.class);
    this.clock = clock;
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
        getMetrics());
  }

  private Metrics getMetrics() {
    LocalDate to = LocalDate.now(clock);
    LocalDate from = to.minusDays(METRICS_WINDOW_DAYS - 1L);
    Instant start = from.atStartOfDay(clock.getZone()).toInstant();
    Instant end = to.plusDays(1).atStartOfDay(clock.getZone()).toInstant();

    List<DecisionBucketCount> decisions = darMetricsDAO.countDarDecisions(start, end, WHOLE_WINDOW);
    List<TurnaroundBucket> turnaround = darMetricsDAO.countDarTurnaround(start, end, WHOLE_WINDOW);
    List<SoApprovalBucket> soApprovals = darMetricsDAO.countSoApprovals(start, end, WHOLE_WINDOW);
    List<VolumeBucketCount> volume = darMetricsDAO.countDarVolume(start, end, WHOLE_WINDOW);
    List<ExpirationBucket> expirations =
        darMetricsDAO.countExpirations(start, end, clock.instant(), WHOLE_WINDOW);
    List<RenewalBucket> renewals = darMetricsDAO.countRenewals(start, end, WHOLE_WINDOW);

    TurnaroundBucket window = turnaround.isEmpty() ? null : turnaround.getFirst();
    return new Metrics(
        from.toString(),
        to.toString(),
        new Decisions(
            sum(decisions, b -> true, DecisionBucketCount::count),
            sum(decisions, b -> b.state() == DecisionState.PENDING, DecisionBucketCount::count),
            sum(decisions, b -> b.state() == DecisionState.APPROVED, DecisionBucketCount::count),
            sum(decisions, b -> b.state() == DecisionState.DENIED, DecisionBucketCount::count),
            sum(decisions, b -> b.state() == DecisionState.MIXED, DecisionBucketCount::count),
            sum(decisions, b -> b.state() == DecisionState.CANCELED, DecisionBucketCount::count)),
        window == null
            ? new Turnaround(0, 0, null, null)
            : new Turnaround(
                window.count() + window.unmeasured(),
                window.unmeasured(),
                window.medianDays(),
                window.modeDays()),
        new SoApprovals(
            sum(soApprovals, b -> b.status() == SoApprovalStatus.APPROVED, SoApprovalBucket::count),
            sum(soApprovals, b -> b.status() == SoApprovalStatus.PENDING, SoApprovalBucket::count),
            sum(soApprovals, b -> b.status() == SoApprovalStatus.SKIPPED, SoApprovalBucket::count)),
        new Volume(
            sum(volume, b -> true, VolumeBucketCount::darCount),
            sum(volume, b -> true, VolumeBucketCount::researcherCount),
            sum(volume, b -> true, VolumeBucketCount::institutionCount)),
        new Expiration(
            sum(expirations, b -> b.reason() == AccessEndReason.EXPIRED, ExpirationBucket::count),
            sum(
                expirations,
                b -> b.reason() == AccessEndReason.CLOSED_OUT,
                ExpirationBucket::count),
            sum(renewals, b -> true, RenewalBucket::collectionCount)));
  }

  private static <T> long sum(List<T> buckets, Predicate<T> filter, ToLongFunction<T> count) {
    return buckets.stream().filter(filter).mapToLong(count).sum();
  }
}
