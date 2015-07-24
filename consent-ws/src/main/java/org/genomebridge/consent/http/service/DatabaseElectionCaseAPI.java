package org.genomebridge.consent.http.service;

import java.util.ArrayList;
import java.util.List;

import org.genomebridge.consent.http.db.ElectionDAO;
import org.genomebridge.consent.http.db.VoteDAO;
import org.genomebridge.consent.http.enumeration.ElectionStatus;
import org.genomebridge.consent.http.enumeration.ElectionType;
import org.genomebridge.consent.http.enumeration.VoteStatus;
import org.genomebridge.consent.http.models.Election;
import org.genomebridge.consent.http.models.PendingCase;
import org.genomebridge.consent.http.models.Vote;

import com.sun.jersey.api.NotFoundException;

public class DatabaseElectionCaseAPI extends AbstractPendingCaseAPI {

    private ElectionDAO electionDAO;
    private VoteDAO voteDAO;

    public static void initInstance(ElectionDAO electionDAO, VoteDAO voteDAO) {
        PendingCaseAPIHolder.setInstance(new DatabaseElectionCaseAPI(electionDAO, voteDAO));

    }

    private DatabaseElectionCaseAPI(ElectionDAO electionDAO, VoteDAO voteDAO) {
        this.electionDAO = electionDAO;
        this.voteDAO = voteDAO;
    }

    @Override
    public List<PendingCase> describeConsentPendingCases(Integer dacUserId) throws NotFoundException {
    	String type = electionDAO.findElectionTypeByType(ElectionType.TRANSLATE_DUL.getValue());
        List<Election> elections = electionDAO.findElectionsByTypeAndStatus(type, ElectionStatus.OPEN.getValue());
        List<PendingCase> pendingCases = new ArrayList<PendingCase>();
        if (elections != null) {
            for (Election election : elections) {
                Vote vote = voteDAO.findVoteByElectionIdAndDACUserId(election.getElectionId(),
                        dacUserId);
                if (vote == null) {
                    continue;
                }
                PendingCase pendingCase = setGeneralFields(dacUserId, election, vote);
                pendingCases.add(pendingCase);
            }
        }
        return pendingCases;
    }

    @Override
    public List<PendingCase> describeDataRequestPendingCases(Integer dacUserId) throws NotFoundException {
        String type = electionDAO.findElectionTypeByType(ElectionType.DATA_ACCESS.getValue());
        List<Election> elections = electionDAO.findElectionsByTypeAndStatus(type, ElectionStatus.OPEN.getValue());
        List<PendingCase> pendingCases = new ArrayList<PendingCase>();
        if (elections != null) {
            for (Election election : elections) {
                Vote vote = voteDAO.findVoteByElectionIdAndDACUserId(election.getElectionId(),
                        dacUserId);
                if (vote == null) {
                    continue;
                }
                PendingCase pendingCase = setGeneralFields(dacUserId, election, vote);
                // if it's already voted, we should collect vote or do the final election vote
                // it depends if the chairperson vote was done after collect votes
                setFinalVote(dacUserId, election, pendingCase);
                pendingCases.add(pendingCase);
            }

        }
        return pendingCases;
    }
    
   
	private void setFinalVote(Integer dacUserId, Election election,	PendingCase pendingCase) {
		if (pendingCase.getAlreadyVoted()) {
		    Vote chairPersonVote = voteDAO.findChairPersonVoteByElectionIdAndDACUserId(
		            election.getElectionId(), dacUserId);
		    if(chairPersonVote != null){
		    	pendingCase.setIsFinalVote(chairPersonVote.getVote() == null ? false : true);	
		    }
		} else {
		    pendingCase.setIsFinalVote(false);
		}
	}

    private PendingCase setGeneralFields(Integer dacUserId, Election election, Vote vote) {
        PendingCase pendingCase = new PendingCase();
        pendingCase.setReferenceId(election.getReferenceId());
        pendingCase.setLogged(setLogged(election));
        pendingCase.setAlreadyVoted(vote.getVote() == null ? false : true);
        pendingCase.setStatus(vote.getVote() == null ? VoteStatus.PENDING.getValue() : VoteStatus.EDITABLE.getValue());
        pendingCase.setVoteId(vote.getVoteId());
        return pendingCase;
    }

    private String setLogged(Election election) {
        StringBuilder logged = new StringBuilder();
        List<Vote> votes = voteDAO.findDACVotesByElectionId(election.getElectionId());
        List<Vote> pendingVotes = voteDAO.findPendingDACVotesByElectionId(election.getElectionId());
        if (votes != null) {
            if (pendingVotes != null) {
                logged.append(votes.size() - pendingVotes.size())
                      .append("/")
                      .append(votes.size());
            } else {
                logged.append("0/")
                      .append(votes.size());
            }
        }
        return logged.toString();
    }

	
    

}
