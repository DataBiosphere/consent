package org.genomebridge.consent.http.service;

import java.util.List;

import org.genomebridge.consent.http.models.Vote;

import com.sun.jersey.api.NotFoundException;

public interface VoteAPI {

    Vote createVote(Vote rec, String referenceId) throws IllegalArgumentException;

    Vote updateVote(Vote rec, Integer voteId, String referenceId) throws IllegalArgumentException;

    List<Vote> describeVotes(String referenceId);

    Vote describeVoteById(Integer voteId, String referenceId) throws NotFoundException;

    void deleteVote(Integer voteId, String referenceId) throws IllegalArgumentException, UnknownIdentifierException;

    void deleteVotes(String referenceId) throws IllegalArgumentException, UnknownIdentifierException;

}
