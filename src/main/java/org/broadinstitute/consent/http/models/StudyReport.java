package org.broadinstitute.consent.http.models;

import java.time.LocalDate;
import java.util.List;
import org.broadinstitute.consent.http.enumeration.MetricsBucket;

/** Studies created in the range, per creation bucket. */
public record StudyReport(
    String from, String to, MetricsBucket bucket, long total, List<CreatedBucket> buckets) {

  public static StudyReport of(
      LocalDate from, LocalDate to, MetricsBucket bucket, List<CreatedBucket> buckets) {
    long total = buckets.stream().mapToLong(CreatedBucket::count).sum();
    return new StudyReport(from.toString(), to.toString(), bucket, total, buckets);
  }
}
