package org.broadinstitute.consent.http.models;

import java.time.Instant;

public record VolumeBucketCount(
    Instant bucketStart,
    Long darCount,
    Long researcherCount,
    Long institutionCount,
    Long datasetCount) {}
