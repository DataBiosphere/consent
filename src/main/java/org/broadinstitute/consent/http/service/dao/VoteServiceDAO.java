package org.broadinstitute.consent.http.service.dao;

import com.google.inject.Inject;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.Vote;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.Update;

/** Handle transactional, multi-table queries for vote operations. */
public class VoteServiceDAO {

  private final Jdbi jdbi;
  private final VoteDAO voteDAO;

  @Inject
  public VoteServiceDAO(Jdbi jdbi) {
    this.jdbi = jdbi;
    this.voteDAO = jdbi.onDemand(VoteDAO.class);
  }

  /**
   * Update vote values. 'FINAL' votes impact elections so matching elections marked as
   * ElectionStatus.CLOSED as well.
   *
   * @param votes List of Votes to update values and rationales for
   * @param voteValue Value to update the votes to
   * @param rationale Value to update the rationales to. Only update if non-null.
   * @return The updated Vote
   * @throws IllegalArgumentException when there are non-open elections on any of the votes
   */
  public List<Vote> updateVotesWithValue(List<Vote> votes, boolean voteValue, String rationale)
      throws IllegalArgumentException {
    // Update all votes in an atomic transaction, rollback on all if any fail
    jdbi.useTransaction(
        h -> {
          final String updateVoteWithRationale =
              "UPDATE vote SET vote = :vote, update_date = :updateDate, rationale = :rationale WHERE vote_id = :voteId";
          final String updateVoteWithoutRationale =
              "UPDATE vote SET vote = :vote, update_date = :updateDate WHERE vote_id = :voteId";
          final String updateElectionStatus =
              "UPDATE election SET status = :status WHERE election_id = :electionId";
          final Date now = new Date();
          votes.forEach(
              vote -> {
                Update voteUpdate;
                if (Objects.isNull(rationale)) {
                  voteUpdate = h.createUpdate(updateVoteWithoutRationale);
                } else {
                  voteUpdate = h.createUpdate(updateVoteWithRationale);
                  voteUpdate.bind("rationale", rationale);
                }
                voteUpdate.bind("vote", voteValue);
                voteUpdate.bind("updateDate", now);
                voteUpdate.bind("voteId", vote.getVoteId());
                voteUpdate.execute();
                if (vote.getType().equalsIgnoreCase(VoteType.FINAL.getValue())
                    || vote.getType().equalsIgnoreCase(VoteType.RADAR_APPROVE.getValue())) {
                  Update electionUpdate = h.createUpdate(updateElectionStatus);
                  electionUpdate.bind("status", ElectionStatus.CLOSED.getValue());
                  electionUpdate.bind("electionId", vote.getElectionId());
                  electionUpdate.execute();
                }
              });
          h.commit();
        });
    return voteDAO.findVotesByIds(votes.stream().map(Vote::getVoteId).toList());
  }
}
