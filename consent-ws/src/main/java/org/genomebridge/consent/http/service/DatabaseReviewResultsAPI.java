package org.genomebridge.consent.http.service;

import org.genomebridge.consent.http.db.ConsentDAO;
import org.genomebridge.consent.http.db.DACUserDAO;
import org.genomebridge.consent.http.db.ElectionDAO;
import org.genomebridge.consent.http.db.VoteDAO;
import org.genomebridge.consent.http.models.*;

import java.util.ArrayList;
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
        review.setDataUseLetter(consent.getDataUseLetter());
        review.setReferenceId(consent.consentId);
        List<Vote> votes = voteDAO.findVotesByReferenceId(referenceId);
        List<ElectionReviewVote> rVotes = new ArrayList<>();
        for (Vote v : votes) {
            ElectionReviewVote rVote = new ElectionReviewVote();
            rVote.setVote(v);
            DACUser user = userDAO.findDACUserById(v.getDacUserId());
            rVote.setEmail(user.getEmail());
            rVote.setDisplayName(user.getDisplayName());
            rVotes.add(rVote);
        }
        review.setReviewVote(rVotes);
        return review;
    }
}