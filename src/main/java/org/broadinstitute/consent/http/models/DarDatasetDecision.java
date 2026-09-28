package org.broadinstitute.consent.http.models;

import java.time.Instant;
import org.broadinstitute.consent.http.enumeration.DecidedVia;
import org.broadinstitute.consent.http.enumeration.DecisionState;

public record DarDatasetDecision(
    String referenceId,
    Integer collectionId,
    Integer datasetId,
    Instant submissionDate,
    DecisionState state,
    DecidedVia decidedVia,
    Instant decisionDate) {}
