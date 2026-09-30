package org.broadinstitute.consent.http.models;

import java.time.Instant;

/** Renewals submitted in one bucket, and the collections they renewed. */
public record RenewalBucket(Instant bucketStart, Long renewalCount, Long collectionCount) {}
