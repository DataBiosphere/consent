package org.broadinstitute.consent.http.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import java.util.List;
import org.broadinstitute.consent.http.db.PersistedDataUseDAO;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.datause.LegacyDataUseDisposition;
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
  @Mock private DatasetService datasetService;
  @Mock private MatchService matchService;

  private LegacyDataUseService service;
  private User admin;

  @BeforeEach
  void setUp() {
    service = new LegacyDataUseService(persistedDataUseDAO, datasetService, matchService);
    admin = new User();
    admin.setUserId(1);
  }

  private static PersistedDataUseRow row(int datasetId, String dataUse, String accessManagement) {
    return new PersistedDataUseRow(datasetId, dataUse, accessManagement, 1);
  }

  private static DataUse hmbOnly() {
    return new DataUseBuilder().setHmbResearch(true).build();
  }

  @Test
  void findNoncanonicalRowsExcludesValidShapes() {
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
        service.findNoncanonicalRows().stream().map(PersistedDataUseRow::datasetId).toList();

    // 3 is MULTIPLE, 4 has no primary under controlled, 6 has a primary under open
    assertEquals(List.of(3, 4, 6), noncanonical);
  }

  @Test
  void reportCountsEveryClassification() {
    when(persistedDataUseDAO.findAllPersistedDataUse())
        .thenReturn(
            List.of(
                row(1, "{\"generalUse\":true}", "controlled"),
                row(2, "{\"generalUse\":true}", "controlled"),
                row(3, HMB_AND_OTHER, "controlled"),
                row(4, null, "controlled")));

    var report = service.report();

    assertEquals(4, report.totalDatasets());
    assertEquals(2, report.countsByClassification().get("SINGLE(GRU)"));
    assertEquals(1, report.countsByClassification().get("MULTIPLE(HMB,OTHER)"));
    assertEquals(1, report.countsByClassification().get("NULL"));
    assertEquals(50d, report.percentage("SINGLE(GRU)"));
    // MULTIPLE, plus the null value under controlled access
    assertEquals(2, report.noncanonicalDatasets());
    assertEquals(2, report.noncanonicalDatasetsWithDars());
  }

  @Test
  void normalizeWritesThroughTheValidatedPathThenRecomputesMatches() {
    var candidate = row(7, HMB_AND_OTHER, "controlled");
    when(persistedDataUseDAO.findDarReferenceIdsByDatasetId(7))
        .thenReturn(List.of("ref-a", "ref-b"));

    var report =
        service.run(
            admin,
            List.of(candidate),
            _ -> new LegacyDataUseDisposition.Normalize(hmbOnly(), "DT-3861 review"));

    verify(datasetService).updateDatasetDataUse(admin, 7, hmbOnly());
    verify(matchService).reprocessMatchesForPurpose("ref-a");
    verify(matchService).reprocessMatchesForPurpose("ref-b");
    assertEquals(1, report.processed());
    assertEquals(2, report.matchesRecomputed());
    assertEquals(0, report.failed());
    assertTrue(report.isComplete(1));
  }

  @Test
  void recomputeOnlyLeavesTheStoredValueAlone() {
    var candidate = row(8, OTHER_ONLY, "controlled");
    when(persistedDataUseDAO.findDarReferenceIdsByDatasetId(8)).thenReturn(List.of("ref-a"));

    var report =
        service.run(
            admin, List.of(candidate), _ -> new LegacyDataUseDisposition.RecomputeMatchesOnly());

    verifyNoInteractions(datasetService);
    verify(matchService).reprocessMatchesForPurpose("ref-a");
    assertEquals(1, report.processed());
  }

  @Test
  void deferTakesNoAction() {
    var candidate = row(9, OTHER_ONLY, "controlled");

    var report =
        service.run(
            admin, List.of(candidate), _ -> new LegacyDataUseDisposition.Defer("no mapping"));

    verifyNoInteractions(datasetService);
    verifyNoInteractions(matchService);
    assertEquals(0, report.processed());
    assertEquals(1, report.skipped());
    assertTrue(report.isComplete(1));
  }

  /** A record with no supplied disposition must never be guessed at. */
  @Test
  void missingDispositionIsSkipped() {
    var report = service.run(admin, List.of(row(10, HMB_AND_OTHER, "controlled")), _ -> null);

    verifyNoInteractions(datasetService);
    assertEquals(1, report.skipped());
  }

  /** Restartability: a rerun sees the approved value already stored and does nothing. */
  @Test
  void rerunSkipsRecordsAlreadyHoldingTheApprovedValue() {
    var candidate = row(11, "{\"hmbResearch\":true}", "controlled");

    var report =
        service.run(
            admin,
            List.of(candidate),
            _ -> new LegacyDataUseDisposition.Normalize(hmbOnly(), "DT-3861 review"));

    verifyNoInteractions(datasetService);
    verifyNoInteractions(matchService);
    assertEquals(1, report.skipped());
    assertEquals(0, report.processed());
  }

  /** Two disease lists share a classification, so the skip must compare values, not shapes. */
  @Test
  void rerunDoesNotSkipADifferentValueWithTheSameClassification() {
    var candidate = row(12, "{\"diseaseRestrictions\":[\"DOID:1\"]}", "controlled");
    DataUse approved = new DataUseBuilder().setDiseaseRestrictions(List.of("DOID:2")).build();
    when(persistedDataUseDAO.findDarReferenceIdsByDatasetId(12)).thenReturn(List.of());

    var report =
        service.run(
            admin,
            List.of(candidate),
            _ -> new LegacyDataUseDisposition.Normalize(approved, "DT-3861 review"));

    verify(datasetService).updateDatasetDataUse(admin, 12, approved);
    assertEquals(1, report.processed());
  }

  @Test
  void validationFailureIsNotRetried() {
    var candidate = row(13, HMB_AND_OTHER, "controlled");
    doThrow(new BadRequestException("rejected"))
        .when(datasetService)
        .updateDatasetDataUse(any(), anyInt(), any());

    var report =
        service.run(
            admin,
            List.of(candidate),
            _ -> new LegacyDataUseDisposition.Normalize(hmbOnly(), "DT-3861 review"));

    verify(datasetService, times(1)).updateDatasetDataUse(any(), anyInt(), any());
    verifyNoInteractions(matchService);
    assertEquals(1, report.failed());
    assertEquals(0, report.retried());
    assertEquals(1, report.failuresByReason().get("validation"));
    assertEquals(List.of(13), report.failedDatasetIds());
  }

  @Test
  void missingDatasetIsNotRetried() {
    var candidate = row(14, HMB_AND_OTHER, "controlled");
    doThrow(new NotFoundException("gone"))
        .when(datasetService)
        .updateDatasetDataUse(any(), anyInt(), any());

    var report =
        service.run(
            admin,
            List.of(candidate),
            _ -> new LegacyDataUseDisposition.Normalize(hmbOnly(), "DT-3861 review"));

    assertEquals(0, report.retried());
    assertEquals(1, report.failuresByReason().get("not-found"));
  }

  @Test
  void transientFailureIsRetriedOnceAndCanSucceed() {
    var candidate = row(15, HMB_AND_OTHER, "controlled");
    when(datasetService.updateDatasetDataUse(any(), anyInt(), any()))
        .thenThrow(new RuntimeException("search sync unavailable"))
        .thenReturn(new Dataset());
    when(persistedDataUseDAO.findDarReferenceIdsByDatasetId(15)).thenReturn(List.of("ref-a"));

    var report =
        service.run(
            admin,
            List.of(candidate),
            _ -> new LegacyDataUseDisposition.Normalize(hmbOnly(), "DT-3861 review"));

    verify(datasetService, times(2)).updateDatasetDataUse(any(), anyInt(), any());
    assertEquals(1, report.processed());
    assertEquals(1, report.retried());
    assertEquals(0, report.failed());
  }

  @Test
  void transientFailureTwiceCountsAsFailedAndRetried() {
    var candidate = row(16, HMB_AND_OTHER, "controlled");
    doThrow(new RuntimeException("still unavailable"))
        .when(datasetService)
        .updateDatasetDataUse(any(), anyInt(), any());

    var report =
        service.run(
            admin,
            List.of(candidate),
            _ -> new LegacyDataUseDisposition.Normalize(hmbOnly(), "DT-3861 review"));

    verify(datasetService, times(2)).updateDatasetDataUse(any(), anyInt(), any());
    assertEquals(1, report.failed());
    assertEquals(1, report.retried());
    assertEquals(1, report.failuresByReason().get("unexpected"));
  }

  /** One record failing must not stop the rest, and the report must scope the rerun. */
  @Test
  void oneFailureDoesNotStopTheRun() {
    var failing = row(17, HMB_AND_OTHER, "controlled");
    var succeeding = row(18, HMB_AND_OTHER, "controlled");
    doThrow(new BadRequestException("rejected"))
        .when(datasetService)
        .updateDatasetDataUse(any(), eq(17), any());
    when(persistedDataUseDAO.findDarReferenceIdsByDatasetId(18)).thenReturn(List.of("ref-b"));

    var report =
        service.run(
            admin,
            List.of(failing, succeeding),
            _ -> new LegacyDataUseDisposition.Normalize(hmbOnly(), "DT-3861 review"));

    verify(datasetService).updateDatasetDataUse(admin, 18, hmbOnly());
    assertEquals(1, report.processed());
    assertEquals(1, report.failed());
    assertEquals(List.of(17), report.failedDatasetIds());
    assertTrue(report.isComplete(2));
  }

  @Test
  void datasetWithNoDarsChangesNoMatches() {
    var candidate = row(19, HMB_AND_OTHER, "controlled");
    when(persistedDataUseDAO.findDarReferenceIdsByDatasetId(19)).thenReturn(List.of());

    var report =
        service.run(
            admin,
            List.of(candidate),
            _ -> new LegacyDataUseDisposition.Normalize(hmbOnly(), "DT-3861 review"));

    verify(matchService, never()).reprocessMatchesForPurpose(any());
    assertEquals(1, report.processed());
    assertEquals(0, report.matchesRecomputed());
  }

  @Test
  void emptyCandidateListIsComplete() {
    var report = service.run(admin, List.of(), _ -> new LegacyDataUseDisposition.Defer("unused"));

    assertEquals(0, report.processed());
    assertTrue(report.isComplete(0));
    assertFalse(report.isComplete(1));
  }
}
