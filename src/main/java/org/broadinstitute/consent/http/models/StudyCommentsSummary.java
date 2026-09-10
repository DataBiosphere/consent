package org.broadinstitute.consent.http.models;

import java.util.List;

/**
 * A page of a study's comments.
 *
 * @param comments the requested page, newest first by creation
 * @param averageRating the mean rating across every comment on the study, not just this page, so it
 *     does not drift as a caller pages through. Null when the study has no comments.
 * @param total how many comments the study has, so a caller knows whether more pages exist
 * @param yourComment the requesting user's own comment, or null when they have none. Carried
 *     separately because paging puts it on an unpredictable page, and a client needs it to know
 *     whether saving adds a comment or revises one.
 */
public record StudyCommentsSummary(
    List<StudyComment> comments, Double averageRating, Integer total, StudyComment yourComment) {}
