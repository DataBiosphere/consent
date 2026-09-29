package org.broadinstitute.consent.http.models;

import java.time.Instant;

/**
 * Turnaround in days for the decisions submitted in one bucket. {@code unmeasured} decisions have
 * no vote date or one before the submission date, so they are counted but not timed; the mode is
 * over whole days.
 */
public record TurnaroundBucket(
    Instant bucketStart,
    Long count,
    Long unmeasured,
    Double meanDays,
    Double medianDays,
    Integer modeDays) {}
