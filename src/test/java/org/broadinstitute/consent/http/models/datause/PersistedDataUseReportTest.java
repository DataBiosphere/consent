package org.broadinstitute.consent.http.models.datause;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class PersistedDataUseReportTest {

  private static final String GRU = "{\"generalUse\":true}";
  private static final String HMB_AND_OTHER = "{\"hmbResearch\":true,\"other\":\"text\"}";

  private static PersistedDataUseRow row(int id, String dataUse, String access, Integer darCount) {
    return new PersistedDataUseRow(id, dataUse, access, darCount);
  }

  @Test
  void emptyPopulationReportsZeros() {
    var report = PersistedDataUseReport.from(List.of());

    assertEquals(0, report.totalDatasets());
    assertEquals(Map.of(), report.countsByClassification());
    assertEquals(0, report.noncanonicalDatasets());
    assertEquals(0, report.noncanonicalDarReferences());
  }

  @Test
  void countsAreOrderedByDescendingCountThenLabel() {
    var report =
        PersistedDataUseReport.from(
            List.of(
                row(1, GRU, "controlled", 0),
                row(2, GRU, "controlled", 0),
                row(3, "{\"hmbResearch\":true}", "controlled", 0),
                row(4, "{\"populationOriginsAncestry\":true}", "controlled", 0)));

    // SINGLE(GRU) leads on count; the two singletons tie and break alphabetically
    assertEquals(
        List.of("SINGLE(GRU)", "SINGLE(HMB)", "SINGLE(POA)"),
        List.copyOf(report.countsByClassification().keySet()));
  }

  @Test
  void crossTabulatesByAccessManagementInAStableOrder() {
    var report =
        PersistedDataUseReport.from(
            List.of(
                row(1, GRU, "external", 0),
                row(2, "{}", "open", 0),
                row(3, GRU, "controlled", 0),
                row(4, GRU, null, 0)));

    assertEquals(
        List.of("controlled", "external", "missing", "open"),
        List.copyOf(report.countsByAccessManagement().keySet()));
    assertEquals(Map.of("SINGLE(GRU)", 1), report.countsByAccessManagement().get("controlled"));
    assertEquals(Map.of("NONE", 1), report.countsByAccessManagement().get("open"));
  }

  @Test
  void countsNoncanonicalDatasetsAndTheirDarReferences() {
    var report =
        PersistedDataUseReport.from(
            List.of(
                row(1, GRU, "controlled", 5),
                row(2, HMB_AND_OTHER, "controlled", 2),
                row(3, "{}", "controlled", 3),
                row(4, GRU, "open", 4)));

    // 2 is MULTIPLE, 3 has no primary under controlled, 4 has a primary under open
    assertEquals(3, report.noncanonicalDatasets());
    assertEquals(3, report.noncanonicalDatasetsWithDars());
    assertEquals(9, report.noncanonicalDarReferences());
  }

  /** A dataset with no DAR relation counts as noncanonical but adds no references. */
  @Test
  void noncanonicalDatasetWithoutDarsIsCountedButAddsNoReferences() {
    var report =
        PersistedDataUseReport.from(
            List.of(row(1, HMB_AND_OTHER, "controlled", 0), row(2, "{}", "controlled", null)));

    assertEquals(2, report.noncanonicalDatasets());
    assertEquals(0, report.noncanonicalDatasetsWithDars());
    assertEquals(0, report.noncanonicalDarReferences());
  }

  @Test
  void aDirectlyConstructedReportCopiesTheMapsItIsGiven() {
    // The pre/post comparison holds reports by value, so a caller's map must not reach inside one
    Map<String, Integer> counts = new LinkedHashMap<>(Map.of("SINGLE(GRU)", 1));
    Map<String, Map<String, Integer>> byAccessManagement = new LinkedHashMap<>();
    byAccessManagement.put("controlled", counts);
    var report = new PersistedDataUseReport(1, counts, byAccessManagement, 0, 0, 0);

    counts.put("NONE", 7);
    byAccessManagement.put("open", Map.of("NONE", 7));

    assertEquals(Map.of("SINGLE(GRU)", 1), report.countsByClassification());
    assertEquals(Map.of("controlled", Map.of("SINGLE(GRU)", 1)), report.countsByAccessManagement());
  }

  @Test
  void aReportKeepsItsCountsOrderedAfterTheCopy() {
    var report =
        PersistedDataUseReport.from(
            List.of(
                row(1, GRU, "controlled", 0),
                row(2, GRU, "controlled", 0),
                row(3, HMB_AND_OTHER, "controlled", 1)));

    assertEquals(
        List.of("SINGLE(GRU)", "MULTIPLE(HMB,OTHER)"),
        List.copyOf(report.countsByClassification().keySet()));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   "})
  void rowWithoutAccessManagementIsLabelledMissing(String accessManagement) {
    var row = row(1, GRU, accessManagement, 0);

    assertEquals("missing", row.accessManagementLabel());
    assertFalse(row.isOpenAccess());
    // A single primary is canonical for everything except open access
    assertTrue(row.isCanonical());
  }

  @Test
  void rowNormalizesAStoredAccessManagementLabelToLowerCase() {
    assertEquals("controlled", row(1, GRU, "CONTROLLED", 0).accessManagementLabel());
  }

  @Test
  void rowNeedsNoRecomputeWithoutADarToReachItThrough() {
    assertFalse(row(1, HMB_AND_OTHER, "controlled", null).needsMatchRecompute());
    assertFalse(row(2, HMB_AND_OTHER, "controlled", 0).needsMatchRecompute());
    assertTrue(row(3, HMB_AND_OTHER, "controlled", 1).needsMatchRecompute());
  }

  @Test
  void rowNeedsNoRecomputeForACanonicalSinglePrimary() {
    assertFalse(row(1, GRU, "controlled", 3).needsMatchRecompute());
    assertTrue(row(2, "{\"other\":\"text\"}", "controlled", 3).needsMatchRecompute());
  }
}
