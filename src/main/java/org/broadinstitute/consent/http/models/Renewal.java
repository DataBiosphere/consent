package org.broadinstitute.consent.http.models;

import java.time.Instant;
import org.broadinstitute.consent.http.enumeration.DecidedVia;

/** One dataset's renewal: a progress report approved on it, and when that vote was cast. */
public record Renewal(
    String referenceId,
    Integer collectionId,
    Integer datasetId,
    Instant submissionDate,
    DecidedVia decidedVia,
    Instant approvalDate) {}
