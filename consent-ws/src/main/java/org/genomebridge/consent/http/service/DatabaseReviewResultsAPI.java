package org.genomebridge.consent.http.service;

import org.genomebridge.consent.http.db.ConsentDAO;
import org.genomebridge.consent.http.db.DACUserDAO;
import org.genomebridge.consent.http.db.ElectionDAO;
import org.genomebridge.consent.http.db.VoteDAO;
import org.genomebridge.consent.http.models.Consent;
import org.genomebridge.consent.http.models.ElectionReview;
import org.genomebridge.consent.http.models.ElectionReviewVote;

import java.util.List;

public class DatabaseReviewResultsAPI extends AbstractReviewResultsAPI {

    private ElectionDAO electionDAO;
    private VoteDAO voteDAO;
    private ConsentDAO consentDAO;
    private DACUserDAO userDAO;

    public static void initInstance(ElectionDAO electionDAO, VoteDAO voteDAO, ConsentDAO consentDAO, DACUserDAO userDAO) {
        ReviewResultsAPIHolder.setInstance(new DatabaseReviewResultsAPI(electionDAO, voteDAO, consentDAO, userDAO));

    }

    private DatabaseReviewResultsAPI(ElectionDAO electionDAO, VoteDAO voteDAO, ConsentDAO consentDAO, DACUserDAO userDAO) {
        this.electionDAO = electionDAO;
        this.voteDAO = voteDAO;
        this.consentDAO = consentDAO;
        this.userDAO = userDAO;
    }

    @Override
    public ElectionReview describeElectionReview(String referenceId) {
        ElectionReview review = new ElectionReview();
        review.setElection(electionDAO.getOpenElectionByReferenceId(referenceId));
        Consent consent = consentDAO.findConsentById(referenceId);
        List<ElectionReviewVote> rVotes = voteDAO.findElectionReviewVotesByReferenceId(referenceId);
        review.setReviewVote(rVotes);
        review.setConsent(consent);
        return review;
    }

    @Override
    public Boolean openElections() {
        Boolean openElections = false;
        if(electionDAO.verifyOpenElections() != null && electionDAO.verifyOpenElections() > 0){
            openElections = true;
        }
        return openElections;
    }
}