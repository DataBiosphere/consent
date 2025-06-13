package org.broadinstitute.consent.http.models;

public record Collaborator(
    Boolean approverStatus,
    String email,
    String eraCommonsId,
    String name,
    String title,
    String uuid,
    String countryOfOperation) {}
