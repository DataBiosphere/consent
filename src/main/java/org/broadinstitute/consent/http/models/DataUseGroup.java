package org.broadinstitute.consent.http.models;

import com.google.gson.annotations.Expose;
import java.util.List;

/**
 * A collection's datasets grouped by the data use they share, as rendered by the Data Access
 * Requests table. Votes are populated only for the DAC that casts them.
 */
public record DataUseGroup(
    @Expose String key,
    @Expose String label,
    @Expose List<GroupDataset> datasets,
    @Expose List<GroupVote> votes) {

  /** A dataset in the group, named as the table's dataset-count tooltip shows it. */
  public record GroupDataset(
      @Expose Integer datasetId, @Expose String name, @Expose String datasetIdentifier) {}

  /** One DAC member's vote on the group. A null vote is still pending. */
  public record GroupVote(
      @Expose Integer userId, @Expose Boolean vote, @Expose String displayName) {}
}
