package org.broadinstitute.consent.http.service;

import org.broadinstitute.consent.http.models.ElectionReview;
import org.broadinstitute.consent.http.models.Vote;
import javax.ws.rs.NotFoundException;
import java.util.List;

public interface ReviewResultsAPI {

    ElectionReview describeCollectElectionReviewByReferenceId(String referenceId, String type);

    Boolean openElections();

    ElectionReview describeElectionReviewByElectionId(Integer electionId,Boolean isFinalAccess);

    ElectionReview describeElectionReviewByReferenceId(String referenceId);

    List<Vote> describeAgreementVote(Integer electionId) throws NotFoundException;
}
