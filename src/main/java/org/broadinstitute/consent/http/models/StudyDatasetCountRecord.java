package org.broadinstitute.consent.http.models;

public record StudyDatasetCountRecord(
    String name, Integer id, String accessTypes, Integer datasetCount) {}
