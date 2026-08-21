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

  /**
   * Copied so a caller's map cannot mutate a report the pre/post comparison holds by value. Not
   * {@code Map.copyOf}, which would discard the ordering {@link #from} establishes.
   */
  public PersistedDataUseReport {
    countsByClassification =
        Collections.unmodifiableMap(new LinkedHashMap<>(countsByClassification));
    Map<String, Map<String, Integer>> copiedRows = new TreeMap<>();
    countsByAccessManagement.forEach(
        (key, counts) ->
            copiedRows.put(key, Collections.unmodifiableMap(new LinkedHashMap<>(counts))));
    countsByAccessManagement = Collections.unmodifiableMap(copiedRows);
  }

  public static PersistedDataUseReport from(List<PersistedDataUseRow> rows) {
    // Classified once: every count below asks the same parse a different question
    List<Classified> classified = rows.stream().map(Classified::of).toList();
    List<Classified> noncanonical = classified.stream().filter(row -> !row.isCanonical()).toList();

    return new PersistedDataUseReport(
        rows.size(),
        countByLabelDescending(classified),
        classified.stream()
            .collect(
                Collectors.groupingBy(
                    row -> row.row().accessManagementLabel(),
                    // Sorted so the cross-tabulation reads the same way on every run
                    TreeMap::new,
                    Collectors.collectingAndThen(
                        Collectors.toList(), PersistedDataUseReport::countByLabelDescending))),
        noncanonical.size(),
        (int) noncanonical.stream().filter(row -> darCount(row) > 0).count(),
        noncanonical.stream().mapToInt(PersistedDataUseReport::darCount).sum());
  }

  private static int darCount(Classified classified) {
    return classified.row().darCount() == null ? 0 : classified.row().darCount();
  }

  private static Map<String, Integer> countByLabelDescending(List<Classified> rows) {
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
    return ordered;
  }

  /** One row and its single parse, so canonicality and the label come from the same one. */
  private record Classified(
      PersistedDataUseRow row, PersistedDataUseClassification classification) {

    static Classified of(PersistedDataUseRow row) {
      return new Classified(row, row.classification());
    }

    boolean isCanonical() {
      return row.isCanonical(classification);
    }
  }
}
