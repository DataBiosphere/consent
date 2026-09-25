package org.broadinstitute.consent.http.models;

import java.time.Instant;
import org.broadinstitute.consent.http.enumeration.DecidedVia;
import org.broadinstitute.consent.http.enumeration.DecisionState;

public record DecisionBucketCount(
    Instant bucketStart, DecisionState state, DecidedVia decidedVia, Long count) {}
