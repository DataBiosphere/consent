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
      """)
  List<StudyComment> findByStudyId(@Bind("studyId") Integer studyId);

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
}
