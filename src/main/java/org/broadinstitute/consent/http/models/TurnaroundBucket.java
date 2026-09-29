package org.broadinstitute.consent.http.models;

import java.time.Instant;

/**
 * Turnaround in days for the decisions submitted in one bucket. {@code undated} decisions have no
 * vote date, so they are counted but not measured; the mode is over whole days.
 */
public record TurnaroundBucket(
    Instant bucketStart,
    Long count,
    Long undated,
    Double meanDays,
    Double medianDays,
    Integer modeDays) {}
