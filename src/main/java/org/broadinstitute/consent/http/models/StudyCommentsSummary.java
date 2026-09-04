package org.broadinstitute.consent.http.models;

import java.util.List;

public record StudyCommentsSummary(List<StudyComment> comments, Double averageRating) {}
