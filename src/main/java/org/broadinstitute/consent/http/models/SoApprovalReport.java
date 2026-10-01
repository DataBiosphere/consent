package org.broadinstitute.consent.http.models;

import java.time.LocalDate;
import java.util.List;
import org.broadinstitute.consent.http.enumeration.MetricsBucket;

/**
 * SO approval per submission bucket, kind and status across the whole range, plus one page of the
 * DARs behind them. {@code total} is the number of DARs in the range, so a client can page through
 * all of them.
 */
public record SoApprovalReport(
    String from,
    String to,
    MetricsBucket bucket,
    long total,
    List<SoApprovalBucket> buckets,
    List<SoApproval> rows) {

  public static SoApprovalReport of(
      LocalDate from,
      LocalDate to,
      MetricsBucket bucket,
      List<SoApprovalBucket> buckets,
      List<SoApproval> rows) {
    long total = buckets.stream().mapToLong(SoApprovalBucket::count).sum();
    return new SoApprovalReport(from.toString(), to.toString(), bucket, total, buckets, rows);
  }
}
