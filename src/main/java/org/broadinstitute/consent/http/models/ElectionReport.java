package org.broadinstitute.consent.http.models;

import java.time.LocalDate;
import java.util.List;
import org.broadinstitute.consent.http.enumeration.MetricsBucket;

/** Data access elections opened and votes cast in the range, per bucket. */
public record ElectionReport(
    String from,
    String to,
    MetricsBucket bucket,
    long electionsOpened,
    long votesCast,
    List<ElectionBucket> elections,
    List<VoteBucket> votes) {

  public static ElectionReport of(
      LocalDate from,
      LocalDate to,
      MetricsBucket bucket,
      List<ElectionBucket> elections,
      List<VoteBucket> votes) {
    long opened = elections.stream().mapToLong(ElectionBucket::count).sum();
    long cast = votes.stream().mapToLong(VoteBucket::count).sum();
    return new ElectionReport(
        from.toString(), to.toString(), bucket, opened, cast, elections, votes);
  }
}
