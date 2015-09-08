package org.genomebridge.consent.http.service;

import org.genomebridge.consent.http.models.Election;
import org.genomebridge.consent.http.models.Vote;

import javax.ws.rs.NotFoundException;
import java.util.List;

public interface VoteAPI {

    List<Vote> createVotes(Integer electionId, Boolean isConsent) throws IllegalArgumentException;

    Vote firstVoteUpdate(Vote rec, String referenceId, String voteId) throws IllegalArgumentException;

    Vote updateVote(Vote rec, Integer voteId, String referenceId) throws IllegalArgumentException;

    List<Vote> describeVotes(String referenceId);

    Vote describeVoteById(Integer voteId, String referenceId) throws NotFoundException;

    void deleteVote(Integer voteId, String referenceId) throws IllegalArgumentException, UnknownIdentifierException;

    void deleteVotes(String referenceId) throws IllegalArgumentException, UnknownIdentifierException;

    void createVotesForElections(List<Election> elections, Boolean isConsent);

    void setReminderFlag(Integer voteId);

}
