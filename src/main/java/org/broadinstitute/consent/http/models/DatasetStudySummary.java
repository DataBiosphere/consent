package org.broadinstitute.consent.http.models;

public record DatasetStudySummary(Integer dataset_id, String dataset_name, String identifier, Integer study_id, String study_name) {
}
