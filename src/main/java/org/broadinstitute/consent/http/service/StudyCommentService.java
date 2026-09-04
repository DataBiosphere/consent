package org.broadinstitute.consent.http.service;

import com.google.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import java.util.List;
import org.broadinstitute.consent.http.db.LibraryCardDAO;
import org.broadinstitute.consent.http.db.StudyCommentDAO;
import org.broadinstitute.consent.http.db.StudyDAO;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.Study;
import org.broadinstitute.consent.http.models.StudyComment;
import org.broadinstitute.consent.http.models.StudyCommentsSummary;
import org.broadinstitute.consent.http.models.User;
import org.jdbi.v3.core.Jdbi;

public class StudyCommentService {
  private final StudyCommentDAO commentDAO;
  private final StudyDAO studyDAO;
  private final LibraryCardDAO libraryCardDAO;
  private final DatasetService datasetService;

  @Inject
  public StudyCommentService(Jdbi jdbi, DatasetService datasetService) {
    commentDAO = jdbi.onDemand(StudyCommentDAO.class);
    studyDAO = jdbi.onDemand(StudyDAO.class);
    libraryCardDAO = jdbi.onDemand(LibraryCardDAO.class);
    this.datasetService = datasetService;
  }

  public StudyCommentsSummary list(Integer studyId, User user) {
    requireStudy(studyId, user);
    List<StudyComment> comments = commentDAO.findByStudyId(studyId);
    Double average =
        comments.isEmpty()
            ? null
            : comments.stream().mapToInt(StudyComment::rating).average().orElse(0);
    return new StudyCommentsSummary(comments, average);
  }

  public StudyComment post(Integer studyId, User user, Integer rating, String text) {
    requireStudy(studyId, user);
    if (!user.hasUserRole(UserRoles.RESEARCHER)
        || libraryCardDAO.findLibraryCardIdByUserId(user.getUserId()) == null) {
      throw new ForbiddenException(
          "Active Researcher Status is required to comment or rate this study.");
    }
    if (rating == null || rating < 1 || rating > 5)
      throw new BadRequestException("Rating must be between 1 and 5.");
    Integer id = commentDAO.upsert(studyId, user.getUserId(), rating, text);
    return commentDAO.findByStudyId(studyId).stream()
        .filter(comment -> comment.studyCommentId().equals(id))
        .findFirst()
        .orElseThrow();
  }

  public void delete(Integer studyId, Integer commentId, User user) {
    requireStudy(studyId, user);
    if (commentDAO.deleteOwn(studyId, commentId, user.getUserId()) == 0)
      throw new NotFoundException("Comment not found");
  }

  /**
   * Loads the study and enforces the same read access that StudyResource applies to the study
   * itself: a study that is not publicly visible is readable only by its creator, custodians, and
   * admins.
   */
  private void requireStudy(Integer studyId, User user) {
    Study study = studyDAO.findStudyById(studyId);
    if (study == null) throw new NotFoundException("Study not found");
    datasetService.verifyStudyVisibilityAccess(study, user);
  }
}
