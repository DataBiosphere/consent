package org.broadinstitute.consent.http.models;

import java.sql.Timestamp;

public record DarMetricsSummary(
    Timestamp updateDate,
    Timestamp submissionDate,
    String projectTitle,
    String darCode,
    String nonTechRus,
    String rus,
    String referenceId,
    String institutionName,
    Boolean expired) {

  /**
   * The same summary with no requester identity on it.
   *
   * <p>A record's canonical constructor is positional, so reordering two same-typed components
   * still compiles and quietly moves values into the wrong slots. This does not escape that - the
   * call below is positional too - it confines it: callers say what they want instead of rebuilding
   * the record, so there is one such call rather than one per call site, and {@code
   * MetricsServiceTest#testWithoutRequesterIdentityKeepsEverythingElse} asserts every surviving
   * field, which is what would actually catch a reorder.
   */
  public DarMetricsSummary withoutRequesterIdentity() {
    return new DarMetricsSummary(
        updateDate,
        submissionDate,
        projectTitle,
        darCode,
        nonTechRus,
        rus,
        referenceId,
        null,
        expired);
  }

  public DarMetricsSummary(
      Timestamp updateDate,
      String projectTitle,
      String darCode,
      String nonTechRus,
      String referenceId,
      Boolean expired) {
    this(updateDate, null, projectTitle, darCode, nonTechRus, null, referenceId, null, expired);
  }
}
