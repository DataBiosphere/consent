package org.broadinstitute.consent.http.models;

import java.sql.Timestamp;

public record DatasetDaaSnapshotDetail(Integer datasetId, Integer daaId, Timestamp capturedAt) {}
