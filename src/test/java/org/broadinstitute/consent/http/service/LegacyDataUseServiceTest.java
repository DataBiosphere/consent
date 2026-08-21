package org.broadinstitute.consent.http.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.broadinstitute.consent.http.db.PersistedDataUseDAO;
import org.broadinstitute.consent.http.models.datause.NoncanonicalDataUseView;
import org.broadinstitute.consent.http.models.datause.PersistedDataUseRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LegacyDataUseServiceTest {

  private static final String HMB_AND_OTHER = "{\"hmbResearch\":true,\"other\":\"not for profit\"}";
  private static final String OTHER_ONLY = "{\"other\":\"bespoke restriction\"}";

  @Mock private PersistedDataUseDAO persistedDataUseDAO;
  @Mock private MatchService matchService;

  private LegacyDataUseService service;

  @BeforeEach
  void setUp() {
    service = new LegacyDataUseService(persistedDataUseDAO, matchService);
  }

  private static PersistedDataUseRow row(int datasetId, String dataUse, String accessManagement) {
    return new PersistedDataUseRow(datasetId, dataUse, accessManagement, 1);
  }

  @Test
  void noncanonicalViewsExcludeValidShapes() {
    when(persistedDataUseDAO.findAllPersistedDataUse())
        .thenReturn(
            List.of(
                row(1, "{\"generalUse\":true}", "controlled"),
                row(2, OTHER_ONLY, "controlled"),
                row(3, HMB_AND_OTHER, "controlled"),
                row(4, "{}", "controlled"),
                row(5, "{}", "open"),
                row(6, "{\"generalUse\":true}", "open")));

    List<Integer> noncanonical =
        service.findNoncanonicalViews().stream().map(NoncanonicalDataUseView::datasetId).toList();

    // 3 is MULTIPLE, 4 has no primary under controlled, 6 has a primary under open
    assertEquals(List.of(3, 4, 6), noncanonical);
  }

  @Test
  void identifiesNoncanonicalDatasetsWithoutExposingTheStoredValue() {
    when(persistedDataUseDAO.findAllPersistedDataUse())
        .thenReturn(
            List.of(
                new PersistedDataUseRow(1, "{\"generalUse\":true}", "controlled", 1),
                new PersistedDataUseRow(2, HMB_AND_OTHER, "controlled", 2),
                new PersistedDataUseRow(3, "{}", "CONTROLLED", null)));

    var views = service.findNoncanonicalViews();

    assertEquals(2, views.size());
    var multiple = views.getFirst();
    assertEquals(2, multiple.datasetId());
    assertEquals("MULTIPLE(HMB,OTHER)", multiple.classification());
    assertEquals("controlled", multiple.accessManagement());
    assertEquals(2, multiple.darCount());
    assertTrue(multiple.needsMatchRecompute());
    // A null DAR count reports as zero and is not reachable by the recompute
    var none = views.get(1);
    assertEquals("NONE", none.classification());
    assertEquals(0, none.darCount());
    assertFalse(none.needsMatchRecompute());
  }

  @Test
  void selectsOnlyAbstainingShapesThatADarCanReach() {
    when(persistedDataUseDAO.findAllPersistedDataUse())
        .thenReturn(
            List.of(
                // Canonical single primaries: V5 delegates to V4
                new PersistedDataUseRow(1, "{\"generalUse\":true}", "controlled", 1),
                new PersistedDataUseRow(2, "{\"diseaseRestrictions\":[\"DOID:1\"]}", "external", 3),
                // Abstaining, with a DAR to reach them through
                new PersistedDataUseRow(3, OTHER_ONLY, "controlled", 1),
                new PersistedDataUseRow(4, HMB_AND_OTHER, "controlled", 2),
                new PersistedDataUseRow(5, "{}", "open", 1),
                new PersistedDataUseRow(6, null, "controlled", 1),
                // Abstaining but unreachable
                new PersistedDataUseRow(7, OTHER_ONLY, "controlled", 0)));

    // 7 is deliberately not stubbed: an unreachable row must never be queried for its DARs
    List.of(3, 4, 5, 6)
        .forEach(
            id ->
                when(persistedDataUseDAO.findDarReferenceIdsByDatasetId(id))
                    .thenReturn(List.of("ref-%d".formatted(id))));

    var report = service.recomputeAbstainingMatches().run();

    List.of(3, 4, 5, 6)
        .forEach(id -> verify(matchService).reprocessMatchesForPurpose("ref-%d".formatted(id)));
    // 1 and 2 are canonical single primaries, 7 abstains but no DAR reaches it
    verifyNoMoreInteractions(matchService);
    assertEquals(4, report.processed());
  }

  @Test
  void recomputeRewritesEveryDarReachingTheDataset() {
    when(persistedDataUseDAO.findDarReferenceIdsByDatasetId(7))
        .thenReturn(List.of("ref-a", "ref-b"));

    var report = service.run(List.of(row(7, HMB_AND_OTHER, "controlled")));

    verify(matchService).reprocessMatchesForPurpose("ref-a");
    verify(matchService).reprocessMatchesForPurpose("ref-b");
    assertEquals(1, report.processed());
    assertEquals(2, report.matchesRecomputed());
    assertEquals(0, report.failed());
  }

  /** Reprocessing a DAR covers every dataset on it, so a shared DAR must be rebuilt once. */
  @Test
  void aDarSharedByCandidatesIsRebuiltOnce() {
    when(persistedDataUseDAO.findDarReferenceIdsByDatasetId(20)).thenReturn(List.of("ref-a"));
    when(persistedDataUseDAO.findDarReferenceIdsByDatasetId(21))
        .thenReturn(List.of("ref-a", "ref-b"));

    var report =
        service.run(List.of(row(20, OTHER_ONLY, "controlled"), row(21, OTHER_ONLY, "controlled")));

    verify(matchService).reprocessMatchesForPurpose("ref-a");
    verify(matchService).reprocessMatchesForPurpose("ref-b");
    verifyNoMoreInteractions(matchService);
    assertEquals(2, report.processed());
    // Distinct DARs, not one count per dataset
    assertEquals(2, report.matchesRecomputed());
  }

  @Test
  void datasetWithNoDarsChangesNoMatches() {
    when(persistedDataUseDAO.findDarReferenceIdsByDatasetId(19)).thenReturn(List.of());

    var report = service.run(List.of(row(19, HMB_AND_OTHER, "controlled")));

    verify(matchService, never()).reprocessMatchesForPurpose(any());
    assertEquals(1, report.processed());
    assertEquals(0, report.matchesRecomputed());
    // Reported apart from the datasets that rebuilt something, which processed alone cannot tell
    assertEquals(1, report.unchanged());
  }

  @Test
  void transientFailureIsRetriedOnceAndCanSucceed() {
    when(persistedDataUseDAO.findDarReferenceIdsByDatasetId(15))
        .thenThrow(new RuntimeException("connection reset"))
        .thenReturn(List.of("ref-a"));

    var report = service.run(List.of(row(15, HMB_AND_OTHER, "controlled")));

    verify(matchService).reprocessMatchesForPurpose("ref-a");
    assertEquals(1, report.processed());
    assertEquals(1, report.retried());
    assertEquals(0, report.failed());
  }

  @Test
  void transientFailureTwiceCountsAsFailedAndRetried() {
    when(persistedDataUseDAO.findDarReferenceIdsByDatasetId(16)).thenReturn(List.of("ref-a"));
    doThrow(new RuntimeException("still unavailable"))
        .when(matchService)
        .reprocessMatchesForPurpose("ref-a");

    var report = service.run(List.of(row(16, HMB_AND_OTHER, "controlled")));

    verify(matchService, times(2)).reprocessMatchesForPurpose("ref-a");
    assertEquals(1, report.failed());
    assertEquals(1, report.retried());
    assertEquals(List.of(16), report.failedDatasetIds());
  }

  /** One record failing must not stop the rest, and the report must scope the rerun. */
  @Test
  void oneFailureDoesNotStopTheRun() {
    when(persistedDataUseDAO.findDarReferenceIdsByDatasetId(17))
        .thenThrow(new RuntimeException("connection reset"));
    when(persistedDataUseDAO.findDarReferenceIdsByDatasetId(18)).thenReturn(List.of("ref-b"));

    var report =
        service.run(
            List.of(row(17, HMB_AND_OTHER, "controlled"), row(18, HMB_AND_OTHER, "controlled")));

    verify(matchService).reprocessMatchesForPurpose("ref-b");
    assertEquals(1, report.processed());
    assertEquals(1, report.failed());
    assertEquals(List.of(17), report.failedDatasetIds());
  }

  /** A retry skips what the first attempt rebuilt, so the count cannot come from one attempt. */
  @Test
  void aRetryStillCreditsWhatTheFirstAttemptRebuilt() {
    when(persistedDataUseDAO.findDarReferenceIdsByDatasetId(22))
        .thenReturn(List.of("ref-a", "ref-b"));
    // Stubbed explicitly: strict stubbing raises on an unstubbed arg, which the retry would eat
    doNothing().when(matchService).reprocessMatchesForPurpose("ref-a");
    doThrow(new RuntimeException("connection reset"))
        .doNothing()
        .when(matchService)
        .reprocessMatchesForPurpose("ref-b");

    var report = service.run(List.of(row(22, HMB_AND_OTHER, "controlled")));

    verify(matchService).reprocessMatchesForPurpose("ref-a");
    verify(matchService, times(2)).reprocessMatchesForPurpose("ref-b");
    assertEquals(1, report.processed());
    assertEquals(1, report.retried());
    assertEquals(2, report.matchesRecomputed());
    assertEquals(0, report.unchanged());
  }

  /** A dataset can fail having already rebuilt some of its DARs; the report must not deny it. */
  @Test
  void aFailedDatasetStillCreditsTheDarsItRebuilt() {
    when(persistedDataUseDAO.findDarReferenceIdsByDatasetId(23))
        .thenReturn(List.of("ref-a", "ref-b"));
    doNothing().when(matchService).reprocessMatchesForPurpose("ref-a");
    doThrow(new RuntimeException("still unavailable"))
        .when(matchService)
        .reprocessMatchesForPurpose("ref-b");

    var report = service.run(List.of(row(23, HMB_AND_OTHER, "controlled")));

    assertEquals(1, report.failed());
    assertEquals(List.of(23), report.failedDatasetIds());
    assertEquals(1, report.matchesRecomputed());
  }

  /** Every DAR already rebuilt by an earlier candidate: completed, but it changed nothing. */
  @Test
  void aCandidateWhoseDarsWereAlreadyRebuiltIsReportedUnchanged() {
    when(persistedDataUseDAO.findDarReferenceIdsByDatasetId(24)).thenReturn(List.of("ref-a"));
    when(persistedDataUseDAO.findDarReferenceIdsByDatasetId(25)).thenReturn(List.of("ref-a"));

    var report =
        service.run(List.of(row(24, OTHER_ONLY, "controlled"), row(25, OTHER_ONLY, "controlled")));

    verify(matchService).reprocessMatchesForPurpose("ref-a");
    verifyNoMoreInteractions(matchService);
    assertEquals(2, report.processed());
    assertEquals(1, report.unchanged());
    assertEquals(1, report.matchesRecomputed());
  }

  @Test
  void recomputeChangesNoStoredDataUse() {
    when(persistedDataUseDAO.findAllPersistedDataUse())
        .thenReturn(
            List.of(
                new PersistedDataUseRow(1, "{\"generalUse\":true}", "controlled", 1),
                new PersistedDataUseRow(2, OTHER_ONLY, "controlled", 1)));
    when(persistedDataUseDAO.findDarReferenceIdsByDatasetId(2)).thenReturn(List.of("ref-a"));

    var result = service.recomputeAbstainingMatches();

    verify(matchService).reprocessMatchesForPurpose("ref-a");
    // Read once for the candidates and the before report, once more to reconcile
    verify(persistedDataUseDAO, times(2)).findAllPersistedDataUse();
    assertEquals(1, result.run().processed());
    assertEquals(1, result.run().matchesRecomputed());
    assertEquals(0, result.run().failed());
    // A recompute-only run must not move any record between classifications
    assertTrue(result.leftClassificationsUnchanged());
    assertEquals(2, result.before().totalDatasets());
    assertEquals(
        Map.of("SINGLE(GRU)", 1, "SINGLE(OTHER)", 1), result.before().countsByClassification());
  }

  /** Abstaining rows with no DAR are left alone rather than counted as done. */
  @Test
  void recomputeLeavesUnreachableAbstainingRowsAlone() {
    when(persistedDataUseDAO.findAllPersistedDataUse())
        .thenReturn(List.of(new PersistedDataUseRow(1, OTHER_ONLY, "controlled", 0)));

    var result = service.recomputeAbstainingMatches();

    verifyNoInteractions(matchService);
    assertEquals(0, result.run().processed());
  }

  @Test
  void recomputeTouchesNothingWhenNothingAbstains() {
    when(persistedDataUseDAO.findAllPersistedDataUse())
        .thenReturn(List.of(new PersistedDataUseRow(1, "{\"generalUse\":true}", "controlled", 1)));

    var result = service.recomputeAbstainingMatches();

    verifyNoInteractions(matchService);
    assertEquals(0, result.run().processed());
    assertTrue(result.leftClassificationsUnchanged());
  }

  @Test
  void emptyCandidateListReportsZeros() {
    var report = service.run(List.of());

    assertEquals(0, report.processed());
    assertEquals(0, report.unchanged());
    assertEquals(0, report.failed());
    assertEquals(0, report.matchesRecomputed());
  }
}
