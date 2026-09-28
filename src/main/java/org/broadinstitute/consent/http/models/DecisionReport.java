package org.broadinstitute.consent.http.models;

import java.time.LocalDate;
import java.util.List;
import org.broadinstitute.consent.http.enumeration.MetricsBucket;

/**
 * Counts per submission bucket across the whole range, plus one page of the rows behind them.
 * {@code total} is the number of rows in the range, so a client can page through all of them.
 */
public record DecisionReport<T>(
    String from,
    String to,
    MetricsBucket bucket,
    long total,
    List<DecisionBucketCount> buckets,
    List<T> rows) {

  public static <T> DecisionReport<T> of(
      LocalDate from,
      LocalDate to,
      MetricsBucket bucket,
      List<DecisionBucketCount> buckets,
      List<T> rows) {
    long total = buckets.stream().mapToLong(DecisionBucketCount::count).sum();
    return new DecisionReport<>(from.toString(), to.toString(), bucket, total, buckets, rows);
  }
}
