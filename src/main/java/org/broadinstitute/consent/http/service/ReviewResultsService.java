package org.broadinstitute.consent.http.service;

import com.google.inject.Inject;
import org.broadinstitute.consent.http.db.ConsentDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.ElectionReview;
import org.broadinstitute.consent.http.models.ElectionReviewVote;
import org.broadinstitute.consent.http.models.Vote;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ReviewResultsService {
    private ElectionDAO electionDAO;
    private VoteDAO voteDAO;
    private ConsentDAO consentDAO;

    @Inject
    public ReviewResultsService(ElectionDAO electionDAO, VoteDAO voteDAO, ConsentDAO consentDAO) {
        this.electionDAO = electionDAO;
        this.voteDAO = voteDAO;
        this.consentDAO = consentDAO;
    }

    public ElectionReview describeLastElectionReviewByReferenceIdAndType(String referenceId, String type) {
        Election election = electionDAO.findLastElectionByReferenceIdAndType(referenceId, type);
        return getElectionReview(referenceId, election);
    }

    public Boolean openElections() {
        Boolean openElections = false;
        if(electionDAO.verifyOpenElections() != null && electionDAO.verifyOpenElections() > 0){
            openElections = true;
        }
        return openElections;
    }

    public ElectionReview describeElectionReviewByElectionId(Integer electionId) {
        ElectionReview review = null;
        Election election = electionDAO.findElectionWithFinalVoteById(electionId);
        if(election != null){
            review = new ElectionReview();
            review.setElection(election);
            Consent consent = consentDAO.findConsentById(review.getElection().getReferenceId());
            List<ElectionReviewVote> rVotes = voteDAO.findElectionReviewVotesByElectionId(electionId);
            review.setReviewVote(rVotes);
            review.setConsent(consent);
        }
        return review;
    }

    public ElectionReview describeElectionReviewByReferenceId(String referenceId){
        List<String> statuses = Stream.of(ElectionStatus.CLOSED.getValue(), ElectionStatus.FINAL.getValue()).
                map(String::toLowerCase).
                collect(Collectors.toList());
        Election election = electionDAO.findLastElectionWithFinalVoteByReferenceIdAndStatus(referenceId, statuses);
        return getElectionReview(referenceId, election);
    }

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
