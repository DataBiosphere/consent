package org.genomebridge.consent.http.service;

import org.genomebridge.consent.http.models.DataRequestElectionReview;
import org.genomebridge.consent.http.models.ElectionReview;

public interface ReviewResultsAPI {

    ElectionReview describeCollectElectionReviewByReferenceId(String referenceId);

    Boolean openElections();

    ElectionReview describeElectionReviewByElectionId(Integer electionId,Boolean isFinalAccess);

    ElectionReview describeElectionReviewByReferenceId(String referenceId);
}
