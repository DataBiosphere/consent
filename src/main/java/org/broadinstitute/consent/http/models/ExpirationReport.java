package org.broadinstitute.consent.http.models;

import java.time.LocalDate;
import java.util.List;
import org.broadinstitute.consent.http.enumeration.MetricsBucket;

/**
 * DAR collections whose access ended in the range, counted per access-end bucket and reason, plus
 * one page of them. {@code total} is the number of collections, so a client can page through all of
 * them.
 */
public record ExpirationReport(
    String from,
    String to,
    MetricsBucket bucket,
    long total,
    List<ExpirationBucket> buckets,
    List<ExpiredCollection> rows) {

  public static ExpirationReport of(
      LocalDate from,
      LocalDate to,
      MetricsBucket bucket,
      List<ExpirationBucket> buckets,
      List<ExpiredCollection> rows) {
    long total = buckets.stream().mapToLong(ExpirationBucket::count).sum();
    return new ExpirationReport(from.toString(), to.toString(), bucket, total, buckets, rows);
  }
}
