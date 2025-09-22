package org.broadinstitute.consent.http.models;

import java.sql.Timestamp;

public record DatasetAuthorizationReader(
    long id, long datasetId, long userId, long createdBy, Timestamp createdDate) {}
