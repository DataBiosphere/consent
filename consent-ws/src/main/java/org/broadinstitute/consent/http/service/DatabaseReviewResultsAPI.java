package org.broadinstitute.consent.http.service;

import org.broadinstitute.consent.http.db.ConsentDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.ElectionReview;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.ElectionReviewVote;
import java.util.Arrays;
import java.util.List;

public class DatabaseReviewResultsAPI extends AbstractReviewResultsAPI {

    private ElectionDAO electionDAO;
    private VoteDAO voteDAO;
    private ConsentDAO consentDAO;

    public static void initInstance(ElectionDAO electionDAO, VoteDAO voteDAO, ConsentDAO consentDAO) {
        ReviewResultsAPIHolder.setInstance(new DatabaseReviewResultsAPI(electionDAO, voteDAO, consentDAO));

    }

    protected DatabaseReviewResultsAPI(ElectionDAO electionDAO, VoteDAO voteDAO, ConsentDAO consentDAO) {
        this.electionDAO = electionDAO;
        this.voteDAO = voteDAO;
        this.consentDAO = consentDAO;
    }

    @Override
    public ElectionReview describeCollectElectionReviewByReferenceId(String referenceId, String type) {
        Election election = electionDAO.getOpenElectionWithFinalVoteByReferenceIdAndType(referenceId, type);
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
    public ElectionReview describeElectionReviewByElectionId(Integer electionId, Boolean isFinalAccess) {
        ElectionReview review = null;
        Election election = electionDAO.findElectionWithFinalVoteById(electionId);
        if(election != null){
            review = new ElectionReview();
            review.setElection(election);
            Consent consent = consentDAO.findConsentById(review.getElection().getReferenceId());
            List<ElectionReviewVote> rVotes = (isFinalAccess == null || isFinalAccess == false) ? voteDAO.findElectionReviewVotesByElectionId(electionId, VoteType.DAC.getValue()) :  voteDAO.findElectionReviewVotesByElectionId(electionId, VoteType.FINAL.getValue());
            review.setReviewVote(rVotes);
            review.setConsent(consent);
        }
        return review;
    }

    @Override
    public ElectionReview describeElectionReviewByReferenceId(String referenceId){
        List<String> status = Arrays.asList(ElectionStatus.CLOSED.getValue(), ElectionStatus.FINAL.getValue());
        Election election = electionDAO.findLastElectionWithFinalVoteByReferenceIdAndStatus(referenceId, status);
        return getElectionReview(referenceId, election);
    }

    @Override
    public List<Vote> describeAgreementVote(Integer electionId) {
        return voteDAO.findVoteByTypeAndElectionId(electionId, VoteType.AGREEMENT.getValue());
    }

    private ElectionReview getElectionReview(String referenceId, Election election) {
        ElectionReview review = null;
        if(election != null){
            review = new ElectionReview();
            List<ElectionReviewVote> rVotes = voteDAO.findElectionReviewVotesByElectionId(election.getElectionId());
            Consent consent = consentDAO.findConsentById(referenceId);
            review.setConsent(consent);
            review.setElection(election);
            review.setReviewVote(rVotes);
        }
        return review;
    }



}