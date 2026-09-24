package org.broadinstitute.consent.http.db;

import java.util.List;
import org.broadinstitute.consent.http.models.StudyComment;
import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

public interface StudyCommentDAO {
  @RegisterConstructorMapper(StudyComment.class)
  @SqlQuery(
      """
      SELECT sc.*, u.display_name, i.institution_name
      FROM study_comment sc
      INNER JOIN users u ON u.user_id = sc.user_id
      LEFT JOIN institution i ON i.institution_id = u.institution_id
      WHERE sc.study_id = :studyId
      ORDER BY sc.create_date DESC, sc.study_comment_id DESC
      LIMIT :limit OFFSET :offset
      """)
  List<StudyComment> findByStudyId(
      @Bind("studyId") Integer studyId, @Bind("limit") int limit, @Bind("offset") int offset);

  /** One comment by id, scoped to its study so it cannot be read through another study's route. */
  @RegisterConstructorMapper(StudyComment.class)
  @SqlQuery(
      """
      SELECT sc.*, u.display_name, i.institution_name
      FROM study_comment sc
      INNER JOIN users u ON u.user_id = sc.user_id
      LEFT JOIN institution i ON i.institution_id = u.institution_id
      WHERE sc.study_id = :studyId AND sc.study_comment_id = :commentId
      """)
  StudyComment findById(@Bind("studyId") Integer studyId, @Bind("commentId") Integer commentId);

  /**
   * The comment a user holds on a study, if any. study_comment is unique on (study_id, user_id), so
   * there is at most one.
   *
   * <p>Returned alongside a page so a reader's own comment is always available: paging means it may
   * sit on any page, and the composer has to know whether it exists to say whether saving adds or
   * revises.
   */
  @RegisterConstructorMapper(StudyComment.class)
  @SqlQuery(
      """
      SELECT sc.*, u.display_name, i.institution_name
      FROM study_comment sc
      INNER JOIN users u ON u.user_id = sc.user_id
      LEFT JOIN institution i ON i.institution_id = u.institution_id
      WHERE sc.study_id = :studyId AND sc.user_id = :userId
      """)
  StudyComment findByStudyIdAndUserId(
      @Bind("studyId") Integer studyId, @Bind("userId") Integer userId);

  @SqlQuery("SELECT count(*) FROM study_comment WHERE study_id = :studyId")
  int countByStudyId(@Bind("studyId") Integer studyId);

  /**
   * The mean rating over every comment on the study, not only the page being returned, so the
   * average a caller sees does not change as they page through. Null when there are none.
   */
  @SqlQuery("SELECT avg(rating) FROM study_comment WHERE study_id = :studyId")
  Double averageRatingByStudyId(@Bind("studyId") Integer studyId);

  @SqlUpdate(
      """
      INSERT INTO study_comment (study_id, user_id, rating, comment_text)
      VALUES (:studyId, :userId, :rating, :commentText)
      ON CONFLICT (study_id, user_id) DO UPDATE SET
        rating = EXCLUDED.rating, comment_text = EXCLUDED.comment_text, update_date = now()
      RETURNING study_comment_id
      """)
  @GetGeneratedKeys
  Integer upsert(
      @Bind("studyId") Integer studyId,
      @Bind("userId") Integer userId,
      @Bind("rating") Integer rating,
      @Bind("commentText") String commentText);

  @SqlUpdate(
      """
      DELETE FROM study_comment
      WHERE study_comment_id = :commentId AND study_id = :studyId AND user_id = :userId
      """)
  int deleteOwn(
      @Bind("studyId") Integer studyId,
      @Bind("commentId") Integer commentId,
      @Bind("userId") Integer userId);

  /**
   * Deletes a comment whoever wrote it. Separate from {@link #deleteOwn} rather than a nullable
   * user id, so a moderation delete has to be asked for explicitly and cannot be reached by passing
   * a null through the author path.
   */
  @SqlUpdate(
      """
      DELETE FROM study_comment
      WHERE study_comment_id = :commentId AND study_id = :studyId
      """)
  int deleteAny(@Bind("studyId") Integer studyId, @Bind("commentId") Integer commentId);
}
