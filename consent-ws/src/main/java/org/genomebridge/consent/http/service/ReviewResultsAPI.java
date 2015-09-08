package org.genomebridge.consent.http.service;

import org.genomebridge.consent.http.models.ElectionReview;

public interface ReviewResultsAPI {

    ElectionReview describeElectionReview(String referenceId);

    Boolean openElections();
}