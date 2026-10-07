package org.broadinstitute.consent.http.models;

import java.time.LocalDate;
import java.util.List;
import org.broadinstitute.consent.http.enumeration.MetricsBucket;

/** Datasets created in the range, per creation bucket, with how many the DAC has approved now. */
public record DatasetReport(
    String from,
    String to,
    MetricsBucket bucket,
    long total,
    long dacApproved,
    List<DatasetBucket> buckets) {

  public static DatasetReport of(
      LocalDate from, LocalDate to, MetricsBucket bucket, List<DatasetBucket> buckets) {
    long total = buckets.stream().mapToLong(DatasetBucket::count).sum();
    long dacApproved = buckets.stream().mapToLong(DatasetBucket::dacApproved).sum();
    return new DatasetReport(from.toString(), to.toString(), bucket, total, dacApproved, buckets);
  }
}
