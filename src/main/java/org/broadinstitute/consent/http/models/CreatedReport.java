package org.broadinstitute.consent.http.models;

import java.time.LocalDate;
import java.util.List;
import org.broadinstitute.consent.http.enumeration.MetricsBucket;

/** Records created in the range, per creation bucket. */
public record CreatedReport(
    String from, String to, MetricsBucket bucket, long total, List<CreatedBucket> buckets) {

  public static CreatedReport of(
      LocalDate from, LocalDate to, MetricsBucket bucket, List<CreatedBucket> buckets) {
    long total = buckets.stream().mapToLong(CreatedBucket::count).sum();
    return new CreatedReport(from.toString(), to.toString(), bucket, total, buckets);
  }
}
