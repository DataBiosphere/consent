package org.broadinstitute.consent.http.service;

import com.google.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import java.util.List;
import org.broadinstitute.consent.http.db.LibraryCardDAO;
import org.broadinstitute.consent.http.db.StudyCommentDAO;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.StudyComment;
import org.broadinstitute.consent.http.models.StudyCommentsSummary;
import org.broadinstitute.consent.http.models.User;
import org.jdbi.v3.core.Jdbi;

public class StudyCommentService {
  private final StudyCommentDAO commentDAO;
  private final LibraryCardDAO libraryCardDAO;
  private final DatasetService datasetService;

  @Inject
  public StudyCommentService(Jdbi jdbi, DatasetService datasetService) {
    commentDAO = jdbi.onDemand(StudyCommentDAO.class);
    libraryCardDAO = jdbi.onDemand(LibraryCardDAO.class);
    this.datasetService = datasetService;
  }

  /** The largest page a caller may request, so one request cannot pull an unbounded list. */
  public static final int MAX_PAGE_SIZE = 100;

  public static final int DEFAULT_PAGE_SIZE = 25;

  /**
   * The longest comment accepted. Enforced here rather than by the column, which stays TEXT: the
   * limit is a product decision that should be changeable without a migration.
   */
  public static final int MAX_COMMENT_LENGTH = 2000;

  public StudyCommentsSummary list(Integer studyId, User user, int limit, int offset) {
    requireStudy(studyId, user);
    List<StudyComment> comments = commentDAO.findByStudyId(studyId, limit, offset);
    return new StudyCommentsSummary(
        comments,
        commentDAO.averageRatingByStudyId(studyId),
        commentDAO.countByStudyId(studyId),
        commentDAO.findByStudyIdAndUserId(studyId, user.getUserId()));
  }

  /**
   * Records or revises the caller's rating and comment.
   *
   * <p>A study's creator and its custodians may rate it, as long as they hold the Researcher role
   * and an active library card - the same bar as anyone else. Their ratings count toward the
   * average like any other. This is deliberate: the gate is about being an active researcher, not
   * about distance from the study.
   */
  public StudyComment post(Integer studyId, User user, Integer rating, String text) {
    requireStudy(studyId, user);
    if (!user.hasUserRole(UserRoles.RESEARCHER)
        || libraryCardDAO.findLibraryCardIdByUserId(user.getUserId()) == null) {
      throw new ForbiddenException(
          "Active Researcher Status is required to comment or rate this study.");
    }
    if (rating == null || rating < 1 || rating > 5) {
      throw new BadRequestException("Rating must be between 1 and 5.");
    }
    if (text != null && text.length() > MAX_COMMENT_LENGTH) {
      throw new BadRequestException(
          "Comment must be %d characters or fewer.".formatted(MAX_COMMENT_LENGTH));
    }
    Integer id = commentDAO.upsert(studyId, user.getUserId(), rating, text);
    StudyComment saved = commentDAO.findById(studyId, id);
    if (saved == null) {
      // Deleted between the write and the read. Rare, but reading the whole list and calling
      // orElseThrow on it reported that as a 500.
      throw new NotFoundException("Comment not found");
    }
    return saved;
  }

  /**
   * Removes a comment.
   *
   * <p>An author deletes their own; an admin deletes anyone's, which is the only moderation path
   * over comments that every authenticated user can read. The Researcher role is required for the
   * author path at the resource, so a user who has lost that role can no longer delete - including
   * their own comment. An admin is not required to hold it.
   */
  public void delete(Integer studyId, Integer commentId, User user) {
    requireStudy(studyId, user);
    int deleted =
        user.hasUserRole(UserRoles.ADMIN)
            ? commentDAO.deleteAny(studyId, commentId)
            : commentDAO.deleteOwn(studyId, commentId, user.getUserId());
    if (deleted == 0) {
      // Someone else's comment is reported absent rather than forbidden, so the response does not
      // confirm that a comment the caller may not touch exists.
      throw new NotFoundException("Comment not found");
    }
  }

  /**
   * Enforces the same read access StudyResource applies to the study itself: a study that is not
   * publicly visible is readable only by its creator, custodians, and admins. The shared gate reads
   * only the study's own details, which is all the rule needs.
   */
  private void requireStudy(Integer studyId, User user) {
    datasetService.requireReadableStudy(studyId, user);
  }
}
