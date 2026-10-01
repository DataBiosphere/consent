package org.broadinstitute.consent.http.models;

import java.time.Instant;
import org.broadinstitute.consent.http.enumeration.DecidedVia;

public record DarDatasetTurnaround(
    String referenceId,
    Integer collectionId,
    Integer datasetId,
    Instant submissionDate,
    Instant decisionDate,
    DecidedVia decidedVia,
    Double elapsedDays) {}
