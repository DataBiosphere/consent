package org.broadinstitute.consent.http.models;

import java.sql.Timestamp;

public record StudyComment(
    Integer studyCommentId,
    Integer studyId,
    Integer userId,
    Integer rating,
    String commentText,
    Timestamp createDate,
    Timestamp updateDate,
    String displayName,
    String institutionName) {}
