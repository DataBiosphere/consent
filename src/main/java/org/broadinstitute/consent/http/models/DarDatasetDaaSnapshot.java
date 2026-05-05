package org.broadinstitute.consent.http.models;

import java.sql.Timestamp;

public record DarDatasetDaaSnapshot(
    Integer darId, Integer datasetId, Integer daaId, Timestamp capturedAt) {}
