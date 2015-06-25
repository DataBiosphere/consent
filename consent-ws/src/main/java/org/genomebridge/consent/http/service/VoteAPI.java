package org.genomebridge.consent.http.service;

import java.util.List;

import org.genomebridge.consent.http.models.Vote;

import com.sun.jersey.api.NotFoundException;

public interface VoteAPI {

   public Vote createVote(Vote rec, String referenceId) throws  IllegalArgumentException;
   
   public Vote updateVote(Vote rec, Integer voteId, String referenceId) throws IllegalArgumentException;
   
   public List<Vote> describeVotes(String referenceId);
   
   public Vote describeVoteById(Integer voteId, String referenceId) throws NotFoundException;
   
   public void deleteVote(Integer voteId,String referenceId) throws IllegalArgumentException, UnknownIdentifierException;
   
   public void deleteVotes(String referenceId) throws IllegalArgumentException, UnknownIdentifierException;
   
}
