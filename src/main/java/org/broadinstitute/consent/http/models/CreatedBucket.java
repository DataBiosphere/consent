package org.broadinstitute.consent.http.models;

import java.time.Instant;

public record CreatedBucket(Instant bucketStart, long count) {}
