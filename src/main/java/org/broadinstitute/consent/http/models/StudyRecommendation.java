package org.broadinstitute.consent.http.models;

import java.util.List;

public record StudyRecommendation(
    Integer studyId,
    String studyName,
    String studyDescription,
    String piName,
    String species,
    String phenotype,
    List<String> dataTypes,
    Long datasetCount,
    List<Integer> datasetIds,
    Long totalParticipants,
    Integer modelCount,
    Integer workspaceCount,
    List<String> accessTypes,
    List<String> dataUseCodes) {}
