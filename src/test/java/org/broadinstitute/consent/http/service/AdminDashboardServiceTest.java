package org.broadinstitute.consent.http.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.broadinstitute.consent.http.db.AdminDashboardDAO;
import org.broadinstitute.consent.http.db.AdminDashboardDAO.DashboardDatabaseCounts;
import org.broadinstitute.consent.http.db.DarMetricsDAO;
import org.broadinstitute.consent.http.enumeration.AccessEndReason;
import org.broadinstitute.consent.http.enumeration.DarKind;
import org.broadinstitute.consent.http.enumeration.DecidedVia;
import org.broadinstitute.consent.http.enumeration.DecisionState;
import org.broadinstitute.consent.http.enumeration.SoApprovalStatus;
import org.broadinstitute.consent.http.models.AdminDashboardSummary;
import org.broadinstitute.consent.http.models.AdminDashboardSummary.DaaAssociations;
import org.broadinstitute.consent.http.models.AdminDashboardSummary.Dacs;
import org.broadinstitute.consent.http.models.AdminDashboardSummary.Institutions;
import org.broadinstitute.consent.http.models.AdminDashboardSummary.LibraryCards;
import org.broadinstitute.consent.http.models.AdminDashboardSummary.Users;
import org.broadinstitute.consent.http.models.DashboardMetrics;
import org.broadinstitute.consent.http.models.DashboardMetrics.Decisions;
import org.broadinstitute.consent.http.models.DashboardMetrics.Expiration;
import org.broadinstitute.consent.http.models.DashboardMetrics.SoApprovals;
import org.broadinstitute.consent.http.models.DashboardMetrics.Turnaround;
import org.broadinstitute.consent.http.models.DashboardMetrics.Volume;
import org.broadinstitute.consent.http.models.DashboardSummary.DarRequests;
import org.broadinstitute.consent.http.models.DecisionBucketCount;
import org.broadinstitute.consent.http.models.ExpirationBucket;
import org.broadinstitute.consent.http.models.RenewalBucket;
import org.broadinstitute.consent.http.models.SoApprovalBucket;
import org.broadinstitute.consent.http.models.TurnaroundBucket;
import org.broadinstitute.consent.http.models.VolumeBucketCount;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceTest {

  private static final List<Integer> ALL_DACS = null;
  private static final Instant NOW = Instant.parse("2026-10-01T15:00:00Z");
  private static final Instant START = Instant.parse("2026-07-04T00:00:00Z");
  private static final Instant END = Instant.parse("2026-10-02T00:00:00Z");
  private static final String WINDOW = "millennium";

  @Mock private Jdbi jdbi;
  @Mock private AdminDashboardDAO dashboardDAO;
  @Mock private DarMetricsDAO darMetricsDAO;

  private AdminDashboardService service;

  @BeforeEach
  void setUp() {
    when(jdbi.onDemand(AdminDashboardDAO.class)).thenReturn(dashboardDAO);
    when(jdbi.onDemand(DarMetricsDAO.class)).thenReturn(darMetricsDAO);
    when(dashboardDAO.getCounts())
        .thenReturn(new DashboardDatabaseCounts(10, 4, 2, 3, 50, 7, 2, 30, 5, 12));
    service = new AdminDashboardService(jdbi, Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void mapsCountsAndTheLastNinetyDaysOfMetrics() {
    when(darMetricsDAO.countDarDecisions(START, END, ALL_DACS, WINDOW))
        .thenReturn(
            List.of(
                new DecisionBucketCount(START, DecisionState.APPROVED, DecidedVia.MANUAL, 3L),
                new DecisionBucketCount(START, DecisionState.APPROVED, DecidedVia.RADAR, 1L),
                new DecisionBucketCount(START, DecisionState.DENIED, DecidedVia.MANUAL, 1L),
                new DecisionBucketCount(START, DecisionState.PENDING, null, 2L),
                new DecisionBucketCount(START, DecisionState.MIXED, DecidedVia.MANUAL, 1L),
                new DecisionBucketCount(START, DecisionState.CANCELED, null, 2L)));
    when(darMetricsDAO.countDarTurnaround(START, END, ALL_DACS, WINDOW))
        .thenReturn(List.of(new TurnaroundBucket(START, 5L, 2L, 14.2, 12.5, 9)));
    when(darMetricsDAO.countSoApprovals(START, END, WINDOW))
        .thenReturn(
            List.of(
                soBucket(DarKind.ORIGINAL, SoApprovalStatus.APPROVED, 2),
                soBucket(DarKind.CLOSEOUT, SoApprovalStatus.APPROVED, 1),
                soBucket(DarKind.ORIGINAL, SoApprovalStatus.SKIPPED, 4)));
    when(darMetricsDAO.countDarVolume(START, END, ALL_DACS, WINDOW))
        .thenReturn(List.of(new VolumeBucketCount(START, 7L, 6L, 3L, 9L)));
    when(darMetricsDAO.countExpirations(START, END, ALL_DACS, NOW, WINDOW))
        .thenReturn(List.of(new ExpirationBucket(START, AccessEndReason.CLOSED_OUT, 2L)));
    when(darMetricsDAO.countRenewals(START, END, ALL_DACS, WINDOW))
        .thenReturn(List.of(new RenewalBucket(START, 5L, 2L)));

    AdminDashboardSummary summary = service.getSummary();

    assertEquals(
        new AdminDashboardSummary(
            new DarRequests(10, 4, 2, 4),
            new Dacs(3),
            new Users(50),
            new Institutions(7, 2),
            new LibraryCards(30),
            new DaaAssociations(5, 12),
            new DashboardMetrics(
                "2026-07-04",
                "2026-10-01",
                new Decisions(10, 2, 4, 1, 1, 2),
                new Turnaround(7, 2, 12.5, 9),
                new SoApprovals(3, 0, 4),
                new Volume(7, 6, 3),
                new Expiration(0, 2, 2))),
        summary);
  }

  @Test
  void leavesTurnaroundStatisticsEmptyWhenNothingWasDecided() {
    when(darMetricsDAO.countDarDecisions(START, END, ALL_DACS, WINDOW)).thenReturn(List.of());
    when(darMetricsDAO.countDarTurnaround(START, END, ALL_DACS, WINDOW)).thenReturn(List.of());
    when(darMetricsDAO.countSoApprovals(START, END, WINDOW)).thenReturn(List.of());
    when(darMetricsDAO.countDarVolume(START, END, ALL_DACS, WINDOW)).thenReturn(List.of());
    when(darMetricsDAO.countExpirations(START, END, ALL_DACS, NOW, WINDOW)).thenReturn(List.of());
    when(darMetricsDAO.countRenewals(START, END, ALL_DACS, WINDOW)).thenReturn(List.of());

    assertEquals(new Turnaround(0, 0, null, null), service.getSummary().metrics().turnaround());
  }

  private static SoApprovalBucket soBucket(DarKind kind, SoApprovalStatus status, long count) {
    return new SoApprovalBucket(START, kind, status, count, 0L, null, null, null);
  }
}
