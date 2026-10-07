package org.broadinstitute.consent.http.models;

import java.time.Instant;

public record DatasetBucket(Instant bucketStart, long count, long dacApproved) {}
