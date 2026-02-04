package org.broadinstitute.consent.http.models;

import java.time.Instant;

public record Reminder(Integer userId, String darCode, Integer collectionId, Instant createDate) {}
