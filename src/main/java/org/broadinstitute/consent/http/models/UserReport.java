package org.broadinstitute.consent.http.models;

import java.time.LocalDate;
import java.util.List;
import org.broadinstitute.consent.http.enumeration.MetricsBucket;

/** Users created in the range, per creation bucket and per role they hold now. */
public record UserReport(
    String from,
    String to,
    MetricsBucket bucket,
    long total,
    List<CreatedBucket> buckets,
    List<RoleUserCount> roles) {

  public static UserReport of(
      LocalDate from,
      LocalDate to,
      MetricsBucket bucket,
      List<CreatedBucket> buckets,
      List<RoleUserCount> roles) {
    long total = buckets.stream().mapToLong(CreatedBucket::count).sum();
    return new UserReport(from.toString(), to.toString(), bucket, total, buckets, roles);
  }
}
