package org.genomebridge.consent.http.service;

import org.genomebridge.consent.http.models.ElectionReview;
import org.genomebridge.consent.http.models.Vote;

import javax.ws.rs.NotFoundException;

public interface ReviewResultsAPI {

    ElectionReview describeCollectElectionReviewByReferenceId(String referenceId, String type);

    Boolean openElections();

    ElectionReview describeElectionReviewByElectionId(Integer electionId,Boolean isFinalAccess);

    ElectionReview describeElectionReviewByReferenceId(String referenceId);

    Vote describeAgreementVote(Integer electionId) throws NotFoundException;
}
