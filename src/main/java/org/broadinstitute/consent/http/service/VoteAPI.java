package org.broadinstitute.consent.http.service;

import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.Vote;
import javax.ws.rs.NotFoundException;
import java.util.List;

@Deprecated // Use VoteService
public interface VoteAPI {

    Vote firstVoteUpdate(Vote rec,  Integer voteId) throws IllegalArgumentException;

    Vote updateVote(Vote rec, Integer voteId, String referenceId) throws IllegalArgumentException;

    List<Vote> describeVotes(String referenceId);

    Vote describeVoteById(Integer voteId, String referenceId) throws NotFoundException;

    Vote describeVoteFinalAccessVoteById(Integer requestId) throws NotFoundException;

    void deleteVote(Integer voteId, String referenceId) throws IllegalArgumentException, UnknownIdentifierException;

    void deleteVotes(String referenceId) throws IllegalArgumentException, UnknownIdentifierException;

    List<Vote> describeVoteByTypeAndElectionId(String type, Integer electionId);

    List<Vote> createDataOwnersReviewVotes(Election electionId);

    Vote describeDataOwnerVote(String requestId, Integer dataOwnerId) throws NotFoundException;

}
