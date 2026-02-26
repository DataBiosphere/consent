package org.broadinstitute.consent.http.models;

public record DatasetStudySummary(
    Integer dataset_id,
    Integer dataset_create_user_id,
    String dataset_name,
    String identifier,
    Integer study_id,
    String study_name,
    Integer study_create_user_id,
    Boolean public_visibility) {}
