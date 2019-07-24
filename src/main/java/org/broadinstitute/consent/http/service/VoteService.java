package org.broadinstitute.consent.http.service;

import com.google.inject.Inject;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.models.Vote;

import java.util.Collection;
import java.util.Date;

public class VoteService {

    private VoteDAO voteDAO;

    @Inject
    public VoteService(VoteDAO voteDAO) {
        this.voteDAO = voteDAO;
    }

    /**
     * Find all votes for a reference id. This can find votes for multiple elections as there
     * are usually multiple forms of election per thing being voted upon.
     *
     * @param referenceId The reference id for the election.
     * @return Collection of votes for the given reference id
     */
    public Collection<Vote> findVotesByReferenceId(String referenceId) {
        return voteDAO.findVotesByReferenceId(referenceId);
    }

    /**
     * Update votes such that they have the provided value and rationale.
     *
     * @param voteList  Collection of votes to advance
     * @param voteValue The new vote value
     * @param rationale The new rationale
     */
    public void advanceVotes(Collection<Vote> voteList, boolean voteValue, String rationale) {
        Date now = new Date();
        voteList.forEach(v -> {
            v.setUpdateDate(now);
            v.setCreateDate(now);
            v.setVote(voteValue);
            v.setRationale(rationale);
            updateVote(v);
        });
    }

    /**
     * @param vote Vote to update
     * @return The updated Vote
     */
    public Vote updateVote(Vote vote) {
        validateVote(vote);
        Date now = new Date();
        voteDAO.updateVote(
                vote.getVote(),
                vote.getRationale(),
                (vote.getUpdateDate() == null) ? now : vote.getUpdateDate(),
                vote.getVoteId(),
                vote.getIsReminderSent(),
                vote.getElectionId(),
                (vote.getCreateDate() == null) ? now : vote.getCreateDate(),
                vote.getHasConcerns()
        );
        return voteDAO.findVoteById(vote.getVoteId());
    }

    /**
     * Convenience method to ensure Vote non-null values are valid
     *
     * @param vote The Vote to validate
     */
    private void validateVote(Vote vote) {
        if (vote == null ||
                vote.getVoteId() == null ||
                vote.getDacUserId() == null ||
                vote.getElectionId() == null) {
            throw new IllegalArgumentException("Invalid vote: " + vote);
        }
        if (voteDAO.findVoteById(vote.getVoteId()) == null) {
            throw new IllegalArgumentException("No vote exists with the id of " + vote.getVoteId());
        }
    }

}
