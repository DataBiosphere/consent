package org.broadinstitute.consent.http.service;

import com.google.inject.Inject;
import org.broadinstitute.consent.http.db.DACUserDAO;
import org.broadinstitute.consent.http.db.DataSetAssociationDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.Vote;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

public class VoteService {

    private DACUserDAO dacUserDAO;
    private DataSetAssociationDAO dataSetAssociationDAO;
    private VoteDAO voteDAO;

    @Inject
    public VoteService(DACUserDAO dacUserDAO, DataSetAssociationDAO dataSetAssociationDAO, VoteDAO voteDAO) {
        this.dacUserDAO = dacUserDAO;
        this.dataSetAssociationDAO = dataSetAssociationDAO;
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
     * Create votes for an election
     * TODO: Apply DAC logic to vote creation
     * @param electionId     The Election ID
     * @param electionType   The Election type
     * @param isManualReview Is this a manual review election
     * @return List of votes
     */
    public List<Vote> createVotes(Integer electionId, ElectionType electionType, Boolean isManualReview) {
        Set<DACUser> dacUserList = dacUserDAO.findDACUsersEnabledToVote();
        List<Vote> votes = new ArrayList<>();
        if (dacUserList != null) {
            for (DACUser user : dacUserList) {
                Integer id = voteDAO.insertVote(user.getDacUserId(), electionId, VoteType.DAC.getValue(), false);
                votes.add(voteDAO.findVoteById(id));
                if (isChairPerson(user)) {
                    Integer chairPersonVoteId = voteDAO.insertVote(user.getDacUserId(), electionId, VoteType.CHAIRPERSON.getValue(), false);
                    votes.add(voteDAO.findVoteById(chairPersonVoteId));
                }
                if (electionType.equals(ElectionType.DATA_ACCESS) && isChairPerson(user)) {
                    id = voteDAO.insertVote(user.getDacUserId(), electionId, VoteType.FINAL.getValue(), false);
                    votes.add(voteDAO.findVoteById(id));
                    if (!isManualReview) {
                        id = voteDAO.insertVote(user.getDacUserId(), electionId, VoteType.AGREEMENT.getValue(), false);
                        votes.add(voteDAO.findVoteById(id));
                    }
                }
            }
        }
        return votes;
    }

    /**
     * Create votes for elections.
     * @param elections List of Elections
     */
    public void createVotesForElections(List<Election> elections) {
        if (elections != null) {
            for (Election election : elections) {
                createVotes(election.getElectionId(), ElectionType.TRANSLATE_DUL, false);
            }
        }
    }

    /**
     * Create Votes for a data owner election
     * @param election Election
     * @return Votes for the election
     */
    @SuppressWarnings("UnusedReturnValue")
    public List<Vote> createDataOwnersReviewVotes(Election election) {
        List<Integer> dataOwners = dataSetAssociationDAO.getDataOwnersOfDataSet(election.getDataSetId());
        voteDAO.insertVotes(dataOwners, election.getElectionId(), VoteType.DATA_OWNER.getValue());
        return voteDAO.findVotesByElectionIdAndType(election.getElectionId(), VoteType.DATA_OWNER.getValue());
    }

    private boolean isChairPerson(DACUser user) {
        return user.getRoles().stream().anyMatch(userRole ->
                userRole.getRoleId().equals(UserRoles.CHAIRPERSON.getRoleId()) ||
                        userRole.getRoleId().equals(UserRoles.MEMBER.getRoleId()));
    }

    /**
     * Convenience method to ensure Vote non-nullable values are populated
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
