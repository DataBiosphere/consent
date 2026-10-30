package org.broadinstitute.consent.http.models;

import java.sql.Timestamp;
import java.util.List;

/** A row of study_workspace. {@code dataUse} holds JSON text. */
public record StudyWorkspaceRecord(
    Integer workspaceId,
    Integer studyId,
    String legacyKey,
    String name,
    String platform,
    String url,
    List<String> cloud,
    String description,
    List<String> tools,
    String access,
    List<String> tags,
    String accessManagement,
    String dataUse,
    Timestamp createDate,
    Timestamp updateDate,
    Timestamp retiredDate) {}
