package org.broadinstitute.consent.http.models;

import java.time.Instant;
import org.broadinstitute.consent.http.enumeration.AccessEndReason;

/** DAR collections whose access ended in one bucket, for one reason. */
public record ExpirationBucket(Instant bucketStart, AccessEndReason reason, Long count) {}
