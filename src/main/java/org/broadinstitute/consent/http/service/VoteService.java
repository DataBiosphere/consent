package org.broadinstitute.consent.http.service;

import com.google.inject.Inject;
import org.broadinstitute.consent.http.db.DACUserDAO;
import org.broadinstitute.consent.http.db.DataSetAssociationDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.Vote;

import javax.ws.rs.NotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class VoteService {

    private final DACUserDAO dacUserDAO;
    private final DataSetAssociationDAO dataSetAssociationDAO;
    private final ElectionDAO electionDAO;
    private final VoteDAO voteDAO;

    @Inject
    public VoteService(DACUserDAO dacUserDAO, DataSetAssociationDAO dataSetAssociationDAO,
                       ElectionDAO electionDAO, VoteDAO voteDAO) {
        this.dacUserDAO = dacUserDAO;
        this.dataSetAssociationDAO = dataSetAssociationDAO;
        this.electionDAO = electionDAO;
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
                Objects.isNull(vote.getUpdateDate()) ? now : vote.getUpdateDate(),
                vote.getVoteId(),
                vote.getIsReminderSent(),
                vote.getElectionId(),
                Objects.isNull(vote.getCreateDate()) ? now : vote.getCreateDate(),
                vote.getHasConcerns()
        );
        return voteDAO.findVoteById(vote.getVoteId());
    }

    /**
     * Create votes for an election
     *
     * TODO: Refactor duplicated code when DatabaseElectionAPI is fully replaced by ElectionService
     *
     * @param election       The Election
     * @param electionType   The Election type
     * @param isManualReview Is this a manual review election
     * @return List of votes
     */
    @SuppressWarnings("DuplicatedCode")
    public List<Vote> createVotes(Election election, ElectionType electionType, Boolean isManualReview) {
        Dac dac = electionDAO.findDacForElection(election.getElectionId());
        Set<DACUser> dacUsers;
        if (dac != null) {
            dacUsers = dacUserDAO.findDACUsersEnabledToVoteByDAC(dac.getDacId());
        } else {
            dacUsers = dacUserDAO.findNonDACUsersEnabledToVote();
        }
        List<Vote> votes = new ArrayList<>();
        if (dacUsers != null) {
            for (DACUser user : dacUsers) {
                votes.addAll(createVotesForUser(user, election, electionType, isManualReview));
            }
        }
        return votes;
    }

    /**
     * Create election votes for a user
     *
     * @param user DACUser
     * @param election Election
     * @param electionType ElectionType
     * @param isManualReview Is election manual review
     * @return List of created votes
     */
    List<Vote> createVotesForUser(DACUser user, Election election, ElectionType electionType, Boolean isManualReview) {
        Dac dac = electionDAO.findDacForElection(election.getElectionId());
        List<Vote> votes = new ArrayList<>();
        Integer dacVoteId = voteDAO.insertVote(user.getDacUserId(), election.getElectionId(), VoteType.DAC.getValue());
        votes.add(voteDAO.findVoteById(dacVoteId));
        if (isDacChairPerson(dac, user)) {
            Integer chairVoteId = voteDAO.insertVote(user.getDacUserId(), election.getElectionId(), VoteType.CHAIRPERSON.getValue());
            votes.add(voteDAO.findVoteById(chairVoteId));
            // Requires Chairperson role to create a final and agreement vote in the Data Access case
            if (electionType.equals(ElectionType.DATA_ACCESS)) {
                Integer finalVoteId = voteDAO.insertVote(user.getDacUserId(), election.getElectionId(), VoteType.FINAL.getValue());
                votes.add(voteDAO.findVoteById(finalVoteId));
                if (!isManualReview) {
                    Integer agreementVoteId = voteDAO.insertVote(user.getDacUserId(), election.getElectionId(), VoteType.AGREEMENT.getValue());
                    votes.add(voteDAO.findVoteById(agreementVoteId));
                }
            }
        }
        return votes;
    }

    /**
     * Create Votes for a data owner election
     *
     * @param election Election
     * @return Votes for the election
     */
    @SuppressWarnings("UnusedReturnValue")
    public List<Vote> createDataOwnersReviewVotes(Election election) {
        List<Integer> dataOwners = dataSetAssociationDAO.getDataOwnersOfDataSet(election.getDataSetId());
        voteDAO.insertVotes(dataOwners, election.getElectionId(), VoteType.DATA_OWNER.getValue());
        return voteDAO.findVotesByElectionIdAndType(election.getElectionId(), VoteType.DATA_OWNER.getValue());
    }

    /**
     * Find the final access vote for this election. In practice, there can be more than one final vote
     * if multiple chairpersons exist. If no one has voted yet, then they are effectively all the same
     * null vote, so any one can be returned. If any chair(s) has voted, then we need to get the most recent
     * vote as that will reflect the latest decision point of the chair(s).
     *
     * @param electionId Election Id
     * @return Final Access Vote
     * @throws NotFoundException No final vote exists for this election
     */
    public Vote describeFinalAccessVoteByElectionId(Integer electionId) throws NotFoundException {
        List<Vote> votes = voteDAO.findFinalVotesByElectionId(electionId);
        if (Objects.isNull(votes) || votes.isEmpty()) {
            throw new NotFoundException("Could not find vote for specified id. Election id: " + electionId);
        }
        // Look for the most recent vote with a value if there are more than one.
        // If there's only one, or all votes null, then return the first one
        if (votes.size() == 1 || votes.stream().allMatch(v -> Objects.isNull(v.getVote()))) {
            return votes.get(0);
        }
        // Look for votes with a value, find by the most recent (max update date)
        // Fall back to the first list vote if we can't find what we're looking for.
        Optional<Vote> mostRecentVote = votes.stream().
                filter(v -> Objects.nonNull(v.getVote())).
                max(Comparator.comparing(Vote::getUpdateDate));
        return mostRecentVote.orElse(votes.get(0));
    }

    /**
     * Delete any votes in Open elections for the specified user in the specified Dac.
     *
     * @param dac The Dac we are restricting elections to
     * @param user The Dac member we are deleting votes for
     */
    void deleteOpenDacVotesForUser(Dac dac, DACUser user) {
        List<Integer> openElectionIds = electionDAO.findOpenElectionsByDacId(dac.getDacId()).stream().
                map(Election::getElectionId).
                collect(Collectors.toList());
        List<Integer> openUserVoteIds = voteDAO.findVotesByElectionIds(openElectionIds).stream().
                filter(v -> v.getDacUserId().equals(user.getDacUserId())).
                map(Vote::getVoteId).
                collect(Collectors.toList());
        voteDAO.removeVotesByIds(openUserVoteIds);
    }

    private boolean isDacChairPerson(Dac dac, DACUser user) {
        if (dac != null) {
            return user.getRoles().
                    stream().
                    anyMatch(userRole -> Objects.nonNull(userRole.getRoleId()) &&
                            Objects.nonNull(userRole.getDacId()) &&
                            userRole.getRoleId().equals(UserRoles.CHAIRPERSON.getRoleId()) &&
                            userRole.getDacId().equals(dac.getDacId()));
        }
        return user.getRoles().
                stream().
                anyMatch(userRole -> Objects.nonNull(userRole.getRoleId()) &&
                        userRole.getRoleId().equals(UserRoles.CHAIRPERSON.getRoleId()));
    }

    /**
     * Convenience method to ensure Vote non-nullable values are populated
     *
     * @param vote The Vote to validate
     */
    private void validateVote(Vote vote) {
        if (Objects.isNull(vote) ||
                Objects.isNull(vote.getVoteId()) ||
                Objects.isNull(vote.getDacUserId()) ||
                Objects.isNull(vote.getElectionId())) {
            throw new IllegalArgumentException("Invalid vote: " + vote);
        }
        if (Objects.isNull(voteDAO.findVoteById(vote.getVoteId()))) {
            throw new IllegalArgumentException("No vote exists with the id of " + vote.getVoteId());
        }
    }

}
