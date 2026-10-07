package org.broadinstitute.consent.http.models;

import java.time.Instant;

public record ElectionBucket(Instant bucketStart, String status, long count) {}
