package org.broadinstitute.consent.http.models;

import java.time.LocalDate;
import java.util.List;
import org.broadinstitute.consent.http.enumeration.MetricsBucket;

/**
 * Renewals submitted in the range, counted per submission bucket, plus one page of them. {@code
 * total} is the number of renewals, so a client can page through all of them.
 */
public record RenewalReport(
    String from,
    String to,
    MetricsBucket bucket,
    long total,
    List<RenewalBucket> buckets,
    List<Renewal> rows) {

  public static RenewalReport of(
      LocalDate from,
      LocalDate to,
      MetricsBucket bucket,
      List<RenewalBucket> buckets,
      List<Renewal> rows) {
    long total = buckets.stream().mapToLong(RenewalBucket::renewalCount).sum();
    return new RenewalReport(from.toString(), to.toString(), bucket, total, buckets, rows);
  }
}
