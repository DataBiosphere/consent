package org.genomebridge.consent.http.service;

import org.genomebridge.consent.http.db.ConsentDAO;
import org.genomebridge.consent.http.db.ElectionDAO;
import org.genomebridge.consent.http.db.VoteDAO;
import org.genomebridge.consent.http.enumeration.ElectionStatus;
import org.genomebridge.consent.http.models.*;
import org.genomebridge.consent.http.enumeration.ElectionType;

import java.util.ArrayList;
import java.util.List;

public class DatabaseReviewResultsAPI extends AbstractReviewResultsAPI {

    private ElectionDAO electionDAO;
    private VoteDAO voteDAO;
    private ConsentDAO consentDAO;

    public static void initInstance(ElectionDAO electionDAO, VoteDAO voteDAO, ConsentDAO consentDAO) {
        ReviewResultsAPIHolder.setInstance(new DatabaseReviewResultsAPI(electionDAO, voteDAO, consentDAO));

    }

    private DatabaseReviewResultsAPI(ElectionDAO electionDAO, VoteDAO voteDAO, ConsentDAO consentDAO) {
        this.electionDAO = electionDAO;
        this.voteDAO = voteDAO;
        this.consentDAO = consentDAO;
    }

    @Override
    public ElectionReview describeCollectElectionReviewByReferenceId(String referenceId) {
        Election election = electionDAO.getOpenElectionByReferenceId(referenceId);
        return getElectionReview(referenceId, election);
    }




    @Override
    public Boolean openElections() {
        Boolean openElections = false;
        if(electionDAO.verifyOpenElections() != null && electionDAO.verifyOpenElections() > 0){
            openElections = true;
        }
        return openElections;
    }


    @Override
    public ElectionReview describeElectionReviewByElectionId(Integer electionId,Boolean isFinalAccess) {
        ElectionReview review = new ElectionReview();
        review.setElection(electionDAO.findElectionById(electionId));
        Consent consent = consentDAO.findConsentById(review.getElection().getReferenceId());
        List<ElectionReviewVote> rVotes = isFinalAccess == null ? voteDAO.findElectionReviewVotesByElectionId(electionId) :  voteDAO.findElectionReviewVotesByElectionId(electionId,isFinalAccess);
        review.setReviewVote(rVotes);
        review.setConsent(consent);
        return review;
    }

    @Override
    public ElectionReview describeElectionReviewByReferenceId(String referenceId){
        Election election = electionDAO.findLastElectionByReferenceIdAndStatus(referenceId, ElectionStatus.CLOSED.getValue());
        return getElectionReview(referenceId, election);
    }


    private ElectionReview getElectionReview(String referenceId, Election election) {
        List<ElectionReviewVote> rVotes = voteDAO.findElectionReviewVotesByElectionId(election.getElectionId());
        ElectionReview review = new ElectionReview();
        Consent consent = consentDAO.findConsentById(referenceId);
        review.setConsent(consent);
        review.setElection(election);
        review.setReviewVote(rVotes);
        return review;

    }



}