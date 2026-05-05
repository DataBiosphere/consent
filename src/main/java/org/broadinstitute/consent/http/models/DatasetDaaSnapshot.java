package org.broadinstitute.consent.http.models;

import java.sql.Timestamp;

public record DatasetDaaSnapshot(Integer daaId, Timestamp capturedAt) {}
