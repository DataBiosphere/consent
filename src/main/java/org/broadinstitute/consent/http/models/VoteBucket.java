package org.broadinstitute.consent.http.models;

import java.time.Instant;

public record VoteBucket(Instant bucketStart, String type, long count) {}
