package org.broadinstitute.consent.http.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.broadinstitute.consent.http.db.MatchMigrationDAO;
import org.broadinstitute.consent.http.models.matchmigration.MatchMigrationPopulation;
import org.broadinstitute.consent.http.models.matchmigration.MatchMigrationRunResult;
import org.broadinstitute.consent.http.models.matchmigration.SnapshotReconciliation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MatchMigrationServiceTest {

  @Mock private MatchMigrationDAO matchMigrationDAO;
  @Mock private MatchService matchService;

  private MatchMigrationService service;

  private void initService() {
    service = new MatchMigrationService(matchMigrationDAO, matchService);
  }

  /** The snapshot is what makes the run reversible, so nothing may be rebuilt before it exists. */
  @Test
  void testRunSnapshotsBeforeReprocessingAnything() {
    stubPopulationAndReconciliation(clearPopulation(), reconciled());
    when(matchMigrationDAO.findResolvablePurposes()).thenReturn(List.of("DAR-1"));
    when(matchMigrationDAO.findUnresolvablePurposes()).thenReturn(List.of());
    doNothing().when(matchService).reprocessMatchesForPurpose("DAR-1");
    initService();

    service.run();

    InOrder inOrder = inOrder(matchMigrationDAO, matchService);
    inOrder.verify(matchMigrationDAO).snapshotAffectedMatches();
    inOrder.verify(matchMigrationDAO).snapshotAffectedRationales();
    inOrder.verify(matchService).reprocessMatchesForPurpose("DAR-1");
  }

  /**
   * An archived or missing DAR resolves to null, so a reprocess would delete its matches and insert
   * nothing. The run must leave those rows alone and say which purposes it left.
   */
  @Test
  void testRunNeverReprocessesAnUnresolvablePurpose() {
    stubPopulationAndReconciliation(clearPopulation(), reconciled());
    when(matchMigrationDAO.findResolvablePurposes()).thenReturn(List.of());
    when(matchMigrationDAO.findUnresolvablePurposes()).thenReturn(List.of("DAR-archived"));
    initService();

    MatchMigrationRunResult result = service.run();

    verify(matchService, never()).reprocessMatchesForPurpose("DAR-archived");
    assertEquals(1, result.run().skipped());
    assertEquals(List.of("DAR-archived"), result.run().skippedPurposeIds());
    assertEquals(0, result.run().reprocessed());
  }

  @Test
  void testRunRetriesAFailedPurposeOnceAndCountsItAsReprocessed() {
    stubPopulationAndReconciliation(clearPopulation(), reconciled());
    when(matchMigrationDAO.findResolvablePurposes()).thenReturn(List.of("DAR-flaky"));
    when(matchMigrationDAO.findUnresolvablePurposes()).thenReturn(List.of());
    doThrow(new IllegalStateException("transient"))
        .doNothing()
        .when(matchService)
        .reprocessMatchesForPurpose("DAR-flaky");
    initService();

    MatchMigrationRunResult result = service.run();

    verify(matchService, times(2)).reprocessMatchesForPurpose("DAR-flaky");
    assertEquals(1, result.run().retried());
    assertEquals(1, result.run().reprocessed());
    assertEquals(0, result.run().failed());
    assertTrue(result.run().failedPurposeIds().isEmpty());
  }

  @Test
  void testRunReportsAPurposeThatFailedTwiceSoARerunCanBeScopedToIt() {
    stubPopulationAndReconciliation(clearPopulation(), reconciled());
    when(matchMigrationDAO.findResolvablePurposes()).thenReturn(List.of("DAR-broken"));
    when(matchMigrationDAO.findUnresolvablePurposes()).thenReturn(List.of());
    doThrow(new IllegalStateException("persistent"))
        .when(matchService)
        .reprocessMatchesForPurpose("DAR-broken");
    initService();

    MatchMigrationRunResult result = service.run();

    verify(matchService, times(2)).reprocessMatchesForPurpose("DAR-broken");
    assertEquals(1, result.run().failed());
    assertEquals(List.of("DAR-broken"), result.run().failedPurposeIds());
    assertEquals(0, result.run().reprocessed());
  }

  /** One purpose failing must not stop the ones after it. */
  @Test
  void testRunContinuesPastAFailedPurpose() {
    stubPopulationAndReconciliation(clearPopulation(), reconciled());
    when(matchMigrationDAO.findResolvablePurposes()).thenReturn(List.of("DAR-broken", "DAR-fine"));
    when(matchMigrationDAO.findUnresolvablePurposes()).thenReturn(List.of());
    doThrow(new IllegalStateException("persistent"))
        .when(matchService)
        .reprocessMatchesForPurpose("DAR-broken");
    doNothing().when(matchService).reprocessMatchesForPurpose("DAR-fine");
    initService();

    MatchMigrationRunResult result = service.run();

    verify(matchService).reprocessMatchesForPurpose("DAR-fine");
    assertEquals(1, result.run().failed());
    assertEquals(1, result.run().reprocessed());
  }

  @Test
  void testRunIsNotReadyForConstraintsWhileRowsRemainUnaccountedFor() {
    SnapshotReconciliation unreconciled =
        new SnapshotReconciliation(409, 0, 409, 0, 405, 405, 0, 409);
    stubPopulationAndReconciliation(blockedPopulation(), unreconciled);
    when(matchMigrationDAO.findResolvablePurposes()).thenReturn(List.of());
    when(matchMigrationDAO.findUnresolvablePurposes()).thenReturn(List.of());
    initService();

    MatchMigrationRunResult result = service.run();

    assertFalse(result.reconciliation().reconciles());
    assertFalse(result.readyForConstraints());
  }

  /** A clean reconciliation is not enough on its own: an existing pair collision still blocks. */
  @Test
  void testRunIsNotReadyForConstraintsWhileAPairCollisionRemains() {
    MatchMigrationPopulation collision = new MatchMigrationPopulation(0, 0, 0, 0, 0, 0, 0, 0, 0, 1);
    stubPopulationAndReconciliation(collision, reconciled());
    when(matchMigrationDAO.findResolvablePurposes()).thenReturn(List.of());
    when(matchMigrationDAO.findUnresolvablePurposes()).thenReturn(List.of());
    initService();

    MatchMigrationRunResult result = service.run();

    assertTrue(result.reconciliation().reconciles());
    assertTrue(result.after().blocksConstraints());
    assertFalse(result.readyForConstraints());
  }

  @Test
  void testRunReportsTheSnapshotTotalsItCaptured() {
    stubPopulationAndReconciliation(clearPopulation(), reconciled());
    when(matchMigrationDAO.snapshotAffectedMatches()).thenReturn(409);
    when(matchMigrationDAO.snapshotAffectedRationales()).thenReturn(52);
    when(matchMigrationDAO.findResolvablePurposes()).thenReturn(List.of());
    when(matchMigrationDAO.findUnresolvablePurposes()).thenReturn(List.of());
    initService();

    MatchMigrationRunResult result = service.run();

    assertEquals(409, result.run().snapshottedMatches());
    assertEquals(52, result.run().snapshottedRationales());
  }

  @Test
  void testFindPopulationAndReconcileWriteNothing() {
    when(matchMigrationDAO.findPopulation()).thenReturn(clearPopulation());
    when(matchMigrationDAO.reconcile()).thenReturn(reconciled());
    initService();

    service.findPopulation();
    service.reconcile();

    verify(matchMigrationDAO, never()).snapshotAffectedMatches();
    verify(matchMigrationDAO, never()).snapshotAffectedRationales();
    verify(matchService, never()).reprocessMatchesForPurpose(any());
  }

  private void stubPopulationAndReconciliation(
      MatchMigrationPopulation population, SnapshotReconciliation reconciliation) {
    when(matchMigrationDAO.findPopulation()).thenReturn(population);
    when(matchMigrationDAO.reconcile()).thenReturn(reconciliation);
  }

  private static MatchMigrationPopulation clearPopulation() {
    return new MatchMigrationPopulation(0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
  }

  private static MatchMigrationPopulation blockedPopulation() {
    return new MatchMigrationPopulation(409, 405, 409, 0, 0, 409, 405, 0, 4, 0);
  }

  private static SnapshotReconciliation reconciled() {
    return new SnapshotReconciliation(409, 409, 0, 0, 405, 405, 0, 0);
  }
}
