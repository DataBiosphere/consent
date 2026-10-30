package org.broadinstitute.consent.http.models;

import java.sql.Timestamp;
import java.util.List;

/** A row of study_model. {@code maintainer} and {@code dataUse} hold JSON text. */
public record StudyModelRecord(
    Integer modelId,
    Integer studyId,
    String legacyKey,
    String name,
    String description,
    String url,
    List<String> cloud,
    String format,
    String license,
    List<String> trainedOnDatasets,
    String maintainer,
    List<String> tags,
    String accessManagement,
    String dataUse,
    Timestamp createDate,
    Timestamp updateDate,
    Timestamp retiredDate) {}
