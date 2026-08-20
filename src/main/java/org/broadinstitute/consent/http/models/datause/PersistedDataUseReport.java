package org.broadinstitute.consent.http.models.datause;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Redacted reconciliation report over persisted Data Use shapes.
 *
 * <p>Counts only: every key is a classification label or access-management value, so a report is
 * safe to attach to a ticket or log.
 *
 * @param totalDatasets every dataset counted, so percentages can be recomputed by a reader
 * @param countsByClassification dataset count per classification label, highest count first
 * @param countsByAccessManagement per access management, the count per classification label
 * @param noncanonicalDatasets datasets whose shape the validator would now reject
 * @param noncanonicalDatasetsWithDars the subset referenced by at least one DAR
 * @param noncanonicalDarReferences DAR references across those datasets
 */
public record PersistedDataUseReport(
    int totalDatasets,
    Map<String, Integer> countsByClassification,
    Map<String, Map<String, Integer>> countsByAccessManagement,
    int noncanonicalDatasets,
    int noncanonicalDatasetsWithDars,
    int noncanonicalDarReferences) {

  public static PersistedDataUseReport from(List<PersistedDataUseRow> rows) {
    List<PersistedDataUseRow> noncanonical =
        rows.stream().filter(row -> !row.isCanonical()).toList();

    return new PersistedDataUseReport(
        rows.size(),
        countByLabelDescending(rows),
        rows.stream()
            .collect(
                Collectors.groupingBy(
                    PersistedDataUseRow::accessManagementLabel,
                    // Sorted so the cross-tabulation reads the same way on every run
                    TreeMap::new,
                    Collectors.collectingAndThen(
                        Collectors.toList(), PersistedDataUseReport::countByLabelDescending))),
        noncanonical.size(),
        (int)
            noncanonical.stream()
                .filter(row -> row.darCount() != null && row.darCount() > 0)
                .count(),
        noncanonical.stream().mapToInt(row -> row.darCount() == null ? 0 : row.darCount()).sum());
  }

  private static Map<String, Integer> countByLabelDescending(List<PersistedDataUseRow> rows) {
    Map<String, Integer> ordered = new LinkedHashMap<>();
    rows.stream()
        .collect(Collectors.groupingBy(row -> row.classification().label(), Collectors.counting()))
        .entrySet()
        .stream()
        .sorted(
            Map.Entry.<String, Long>comparingByValue()
                .reversed()
                .thenComparing(Map.Entry.comparingByKey()))
        .forEachOrdered(entry -> ordered.put(entry.getKey(), entry.getValue().intValue()));
    // Not Map.copyOf, which would discard the ordering just established
    return Collections.unmodifiableMap(ordered);
  }

  /** Percentage of all datasets carrying a classification. */
  public double percentage(String classificationLabel) {
    if (totalDatasets == 0) {
      return 0d;
    }
    return countsByClassification.getOrDefault(classificationLabel, 0) * 100d / totalDatasets;
  }

  /** Whether two reports describe the same population, which is how pre/post states reconcile. */
  public boolean reconcilesWith(PersistedDataUseReport other) {
    return other != null && totalDatasets == other.totalDatasets;
  }
}
