package org.broadinstitute.consent.http.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.ToLongFunction;
import org.broadinstitute.consent.http.db.DarMetricsDAO;
import org.broadinstitute.consent.http.enumeration.AccessEndReason;
import org.broadinstitute.consent.http.enumeration.DecisionState;
import org.broadinstitute.consent.http.enumeration.SoApprovalStatus;
import org.broadinstitute.consent.http.models.DashboardMetrics;
import org.broadinstitute.consent.http.models.DashboardMetrics.Decisions;
import org.broadinstitute.consent.http.models.DashboardMetrics.Expiration;
import org.broadinstitute.consent.http.models.DashboardMetrics.SoApprovals;
import org.broadinstitute.consent.http.models.DashboardMetrics.Turnaround;
import org.broadinstitute.consent.http.models.DashboardMetrics.Volume;
import org.broadinstitute.consent.http.models.DecisionBucketCount;
import org.broadinstitute.consent.http.models.ExpirationBucket;
import org.broadinstitute.consent.http.models.RenewalBucket;
import org.broadinstitute.consent.http.models.SoApprovalBucket;
import org.broadinstitute.consent.http.models.TurnaroundBucket;
import org.broadinstitute.consent.http.models.VolumeBucketCount;

/** The dashboards' metric tiles, from the same DarMetricsDAO counts as the /api/metrics reports. */
class DashboardMetricsReader {
  static final int METRICS_WINDOW_DAYS = 90;

  // date_trunc to a millennium puts the whole window in one bucket, so the reporting queries
  // return window-wide medians and distinct counts rather than per-bucket ones.
  private static final String WHOLE_WINDOW = "millennium";

  private final DarMetricsDAO darMetricsDAO;
  private final Clock clock;

  DashboardMetricsReader(DarMetricsDAO darMetricsDAO, Clock clock) {
    this.darMetricsDAO = darMetricsDAO;
    this.clock = clock;
  }

  /**
   * The last {@value #METRICS_WINDOW_DAYS} days over the DACs given, or every DAC when null; SO
   * approvals are left null unless asked for.
   */
  DashboardMetrics read(List<Integer> dacIds, boolean withSoApprovals) {
    LocalDate to = LocalDate.now(clock);
    LocalDate from = to.minusDays(METRICS_WINDOW_DAYS - 1L);
    Instant start = from.atStartOfDay(clock.getZone()).toInstant();
    Instant end = to.plusDays(1).atStartOfDay(clock.getZone()).toInstant();

    List<DecisionBucketCount> decisions =
        darMetricsDAO.countDarDecisions(start, end, dacIds, WHOLE_WINDOW);
    List<TurnaroundBucket> turnaround =
        darMetricsDAO.countDarTurnaround(start, end, dacIds, WHOLE_WINDOW);
    List<SoApprovalBucket> soApprovals =
        withSoApprovals ? darMetricsDAO.countSoApprovals(start, end, WHOLE_WINDOW) : null;
    List<VolumeBucketCount> volume = darMetricsDAO.countDarVolume(start, end, dacIds, WHOLE_WINDOW);
    List<ExpirationBucket> expirations =
        darMetricsDAO.countExpirations(start, end, dacIds, clock.instant(), WHOLE_WINDOW);
    List<RenewalBucket> renewals = darMetricsDAO.countRenewals(start, end, dacIds, WHOLE_WINDOW);

    TurnaroundBucket window = turnaround.isEmpty() ? null : turnaround.getFirst();
    return new DashboardMetrics(
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
        soApprovals == null
            ? null
            : new SoApprovals(
                sum(
                    soApprovals,
                    b -> b.status() == SoApprovalStatus.APPROVED,
                    SoApprovalBucket::count),
                sum(
                    soApprovals,
                    b -> b.status() == SoApprovalStatus.PENDING,
                    SoApprovalBucket::count),
                sum(
                    soApprovals,
                    b -> b.status() == SoApprovalStatus.SKIPPED,
                    SoApprovalBucket::count)),
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
