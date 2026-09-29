package org.broadinstitute.consent.http.models;

import java.time.LocalDate;
import java.util.List;
import org.broadinstitute.consent.http.enumeration.MetricsBucket;

/**
 * Submission volume over a range: counts per submission bucket, DARs per institution and per
 * researcher across the whole range, and one page of the DARs behind them. {@code total} is the
 * number of DARs in the range, so a client can page through all of them.
 */
public record VolumeReport(
    String from,
    String to,
    MetricsBucket bucket,
    long total,
    List<VolumeBucketCount> buckets,
    List<InstitutionDarCount> institutions,
    List<ResearcherDarCount> researchers,
    List<DarVolume> rows) {

  public static VolumeReport of(
      LocalDate from,
      LocalDate to,
      MetricsBucket bucket,
      List<VolumeBucketCount> buckets,
      List<InstitutionDarCount> institutions,
      List<ResearcherDarCount> researchers,
      List<DarVolume> rows) {
    long total = buckets.stream().mapToLong(VolumeBucketCount::darCount).sum();
    return new VolumeReport(
        from.toString(), to.toString(), bucket, total, buckets, institutions, researchers, rows);
  }
}
