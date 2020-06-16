package org.broadinstitute.consent.http.service;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.ws.rs.NotFoundException;
import org.broadinstitute.consent.http.db.DataSetAssociationDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.Vote;

public class VoteService {

    private final DataSetAssociationDAO dataSetAssociationDAO;
    private final ElectionDAO electionDAO;
    private final UserDAO userDAO;
    private final VoteDAO voteDAO;

    @Inject
    public VoteService(DataSetAssociationDAO dataSetAssociationDAO, ElectionDAO electionDAO,
            UserDAO userDAO, VoteDAO voteDAO) {
        this.dataSetAssociationDAO = dataSetAssociationDAO;
        this.electionDAO = electionDAO;
        this.userDAO = userDAO;
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
     * Find a vote by id
     *
     * @param voteId The vote id
     * @return Vote
     * @throws NotFoundException The exception
     */
    public Vote findVoteById(Integer voteId) throws NotFoundException {
        Vote vote = voteDAO.findVoteById(voteId);
        if (vote == null) {
            throw new NotFoundException("Could not find vote for specified id. Vote id: " + voteId);
        }
        return vote;
    }

    /**
     * Delete a vote by id
     *
     * @param voteId The vote id
     */
    public void deleteVote(Integer voteId) {
        if (voteDAO.findVoteById(voteId) == null) {
            throw new NotFoundException("Does not exist vote for the specified id. Id: " + voteId);
        }
        voteDAO.deleteVoteById(voteId);
    }

    /**
     * Delete votes for an election reference id
     *
     * @param referenceId The election's reference id
     * @throws NotFoundException The exception
     */
    public void deleteVotes(String referenceId) throws NotFoundException {
        if (electionDAO.findElectionsWithFinalVoteByReferenceId(referenceId) == null) {
            throw new NotFoundException();
        }
        voteDAO.deleteVotes(referenceId);
    }

    /**
     * Find the data owner vote for a reference id and user id
     *
     * @param referenceId data owner Election reference id
     * @param dataOwnerId data owner user id
     * @return data owner Vote
     * @throws NotFoundException The exception
     */
    public Vote describeDataOwnerVote(String referenceId, Integer dataOwnerId) throws NotFoundException {
        Vote vote = voteDAO.findVotesByReferenceIdTypeAndUser(
                referenceId, dataOwnerId, VoteType.DATA_OWNER.getValue());
        if (vote == null) {
          throw new NotFoundException("Vote doesn't exist for the specified dataOwnerId");
        }
        return vote;
    }

    /**
     * Update a vote for the first time.
     *
     * @param vote The vote with populated values
     * @param voteId The vote id
     * @return Updated vote
     * @throws NotFoundException The exception
     */
    public Vote firstVoteUpdate(Vote vote,  Integer voteId) throws NotFoundException {
        Vote existingVote = voteDAO.findVoteById(voteId);
        if (Objects.isNull(existingVote)) {
            throw new NotFoundException("Could not find vote for specified id. Vote id: " + voteId);
        }
        vote.setVoteId(voteId);
        vote.setCreateDate(new Date());
        vote.setElectionId(existingVote.getElectionId());
        updateVote(vote);
        return voteDAO.findVoteById(voteId);
    }

    /**
     * Find votes by vote type and election id
     *
     * @param type The type of vote
     * @param electionId The election id
     * @return List of votes
     */
    public List<Vote> describeVoteByTypeAndElectionId(String type, Integer electionId) {
        return voteDAO.findVoteByTypeAndElectionId(electionId, type);
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
        Set<User> users;
        if (dac != null) {
            users = userDAO.findUsersEnabledToVoteByDAC(dac.getDacId());
        } else {
            users = userDAO.findNonDacUsersEnabledToVote();
        }
        List<Vote> votes = new ArrayList<>();
        if (users != null) {
            for (User user : users) {
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
    public List<Vote> createVotesForUser(User user, Election election, ElectionType electionType, Boolean isManualReview) {
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
    public void deleteOpenDacVotesForUser(Dac dac, User user) {
        List<Integer> openElectionIds = electionDAO.findOpenElectionsByDacId(dac.getDacId()).stream().
                map(Election::getElectionId).
                collect(Collectors.toList());
        if (!openElectionIds.isEmpty()) {
            List<Integer> openUserVoteIds = voteDAO.findVotesByElectionIds(openElectionIds).stream().
                    filter(v -> v.getDacUserId().equals(user.getDacUserId())).
                    map(Vote::getVoteId).
                    collect(Collectors.toList());
            if (!openUserVoteIds.isEmpty()) {
                voteDAO.removeVotesByIds(openUserVoteIds);
            }
        }
    }

    private boolean isDacChairPerson(Dac dac, User user) {
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
