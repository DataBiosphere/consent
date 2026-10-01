package org.broadinstitute.consent.http.models;

import java.time.Instant;
import org.broadinstitute.consent.http.enumeration.DecidedVia;

public record DarTurnaround(
    String referenceId,
    Integer collectionId,
    Instant submissionDate,
    Instant decisionDate,
    DecidedVia decidedVia,
    Double elapsedDays) {}
