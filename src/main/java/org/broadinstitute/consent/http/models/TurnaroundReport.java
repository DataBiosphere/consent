package org.broadinstitute.consent.http.models;

import java.time.LocalDate;
import java.util.List;
import org.broadinstitute.consent.http.enumeration.MetricsBucket;

/**
 * Turnaround per submission bucket across the whole range, plus one page of the measured rows
 * behind it. {@code total} is the number of measured rows, so a client can page through all of
 * them; {@code undated} decisions are counted apart because they can't be measured.
 */
public record TurnaroundReport<T>(
    String from,
    String to,
    MetricsBucket bucket,
    long total,
    long undated,
    List<TurnaroundBucket> buckets,
    List<T> rows) {

  public static <T> TurnaroundReport<T> of(
      LocalDate from,
      LocalDate to,
      MetricsBucket bucket,
      List<TurnaroundBucket> buckets,
      List<T> rows) {
    long total = buckets.stream().mapToLong(TurnaroundBucket::count).sum();
    long undated = buckets.stream().mapToLong(TurnaroundBucket::undated).sum();
    return new TurnaroundReport<>(
        from.toString(), to.toString(), bucket, total, undated, buckets, rows);
  }
}
