package org.broadinstitute.consent.http.models;

import java.time.Instant;
import org.broadinstitute.consent.http.enumeration.DecidedVia;
import org.broadinstitute.consent.http.enumeration.DecisionState;

public record DarDecision(
    String referenceId,
    Integer collectionId,
    Instant submissionDate,
    Integer datasetCount,
    DecisionState state,
    DecidedVia decidedVia,
    Instant decisionDate) {}
