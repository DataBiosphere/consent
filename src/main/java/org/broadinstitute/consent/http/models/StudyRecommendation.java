package org.broadinstitute.consent.http.models;

import java.util.List;

public record StudyRecommendation(
    Integer studyId,
    String studyName,
    String studyDescription,
    String piName,
    Long datasetCount,
    List<Integer> datasetIds) {}
