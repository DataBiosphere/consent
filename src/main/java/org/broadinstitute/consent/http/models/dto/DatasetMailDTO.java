package org.broadinstitute.consent.http.models.dto;

public record DatasetMailDTO(
    Integer datasetId, String name, String identifier, String dataLocationUrl) {}
