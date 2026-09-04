package org.broadinstitute.consent.http.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import java.util.List;
import org.broadinstitute.consent.http.AbstractTestHelper;
import org.broadinstitute.consent.http.db.LibraryCardDAO;
import org.broadinstitute.consent.http.db.StudyCommentDAO;
import org.broadinstitute.consent.http.db.StudyDAO;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.Study;
import org.broadinstitute.consent.http.models.StudyComment;
import org.broadinstitute.consent.http.models.StudyCommentsSummary;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StudyCommentServiceTest extends AbstractTestHelper {

  @Mock private Jdbi jdbi;
  @Mock private StudyCommentDAO commentDAO;
  @Mock private StudyDAO studyDAO;
  @Mock private LibraryCardDAO libraryCardDAO;
  @Mock private DatasetService datasetService;

  private StudyCommentService service;

  @BeforeEach
  void setUp() {
    when(jdbi.onDemand(StudyCommentDAO.class)).thenReturn(commentDAO);
    when(jdbi.onDemand(StudyDAO.class)).thenReturn(studyDAO);
    when(jdbi.onDemand(LibraryCardDAO.class)).thenReturn(libraryCardDAO);
    service = new StudyCommentService(jdbi, datasetService);
  }

  @Test
  void testListStudyNotFound() {
    when(studyDAO.findStudyById(1)).thenReturn(null);

    assertThrows(NotFoundException.class, () -> service.list(1, researcher(10)));
  }

  @Test
  void testListNoCommentsHasNullAverage() {
    when(studyDAO.findStudyById(1)).thenReturn(new Study());
    when(commentDAO.findByStudyId(1)).thenReturn(List.of());

    StudyCommentsSummary summary = service.list(1, researcher(10));

    assertEquals(List.of(), summary.comments());
    assertNull(summary.averageRating());
  }

  @Test
  void testListComputesAverageRating() {
    when(studyDAO.findStudyById(1)).thenReturn(new Study());
    when(commentDAO.findByStudyId(1))
        .thenReturn(List.of(comment(1, 1, 4), comment(2, 2, 2), comment(3, 3, 3)));

    StudyCommentsSummary summary = service.list(1, researcher(10));

    assertEquals(3, summary.comments().size());
    assertEquals(3.0, summary.averageRating());
  }

  @Test
  void testPostStudyNotFound() {
    when(studyDAO.findStudyById(1)).thenReturn(null);

    assertThrows(NotFoundException.class, () -> service.post(1, researcher(10), 5, "text"));
  }

  @Test
  void testPostRequiresResearcherRole() {
    when(studyDAO.findStudyById(1)).thenReturn(new Study());
    User user = new User();
    user.setUserId(10);

    assertThrows(ForbiddenException.class, () -> service.post(1, user, 5, "text"));
  }

  @Test
  void testPostRequiresLibraryCard() {
    when(studyDAO.findStudyById(1)).thenReturn(new Study());
    User user = researcher(10);
    when(libraryCardDAO.findLibraryCardIdByUserId(10)).thenReturn(null);

    assertThrows(ForbiddenException.class, () -> service.post(1, user, 5, "text"));
  }

  @Test
  void testPostRejectsInvalidRatings() {
    when(studyDAO.findStudyById(1)).thenReturn(new Study());
    User user = researcher(10);
    when(libraryCardDAO.findLibraryCardIdByUserId(10)).thenReturn(100);

    assertThrows(BadRequestException.class, () -> service.post(1, user, null, "text"));
    assertThrows(BadRequestException.class, () -> service.post(1, user, 0, "text"));
    assertThrows(BadRequestException.class, () -> service.post(1, user, 6, "text"));
  }

  @Test
  void testPostUpsertsAndReturnsComment() {
    when(studyDAO.findStudyById(1)).thenReturn(new Study());
    User user = researcher(10);
    when(libraryCardDAO.findLibraryCardIdByUserId(10)).thenReturn(100);
    when(commentDAO.upsert(1, 10, 5, "text")).thenReturn(7);
    StudyComment expected = comment(7, 10, 5);
    when(commentDAO.findByStudyId(1)).thenReturn(List.of(comment(6, 11, 3), expected));

    StudyComment posted = service.post(1, user, 5, "text");

    assertEquals(expected, posted);
  }

  @Test
  void testDelete() {
    User user = researcher(10);
    when(studyDAO.findStudyById(1)).thenReturn(new Study());
    when(commentDAO.deleteOwn(1, 7, 10)).thenReturn(1);

    service.delete(1, 7, user);

    verify(commentDAO).deleteOwn(1, 7, 10);
  }

  @Test
  void testDeleteNotFound() {
    User user = researcher(10);
    when(studyDAO.findStudyById(1)).thenReturn(new Study());
    when(commentDAO.deleteOwn(1, 7, 10)).thenReturn(0);

    assertThrows(NotFoundException.class, () -> service.delete(1, 7, user));
  }

  /** The study id in the path is load-bearing: a comment is not reachable through another study. */
  @Test
  void testDeleteStudyNotFound() {
    User user = researcher(10);
    when(studyDAO.findStudyById(999)).thenReturn(null);

    assertThrows(NotFoundException.class, () -> service.delete(999, 7, user));
    verify(commentDAO, never()).deleteOwn(any(), any(), any());
  }

  /**
   * A study the user may not read must not leak its comments, the same way StudyResource hides the
   * study itself.
   */
  @Test
  void testStudyNotVisibleToUser() {
    User user = researcher(10);
    Study study = new Study();
    study.setPublicVisibility(false);
    when(studyDAO.findStudyById(1)).thenReturn(study);
    doThrow(new NotFoundException("Study not found"))
        .when(datasetService)
        .verifyStudyVisibilityAccess(any(), any());

    assertThrows(NotFoundException.class, () -> service.list(1, user));
    assertThrows(NotFoundException.class, () -> service.post(1, user, 5, "text"));
    assertThrows(NotFoundException.class, () -> service.delete(1, 7, user));
  }

  private User researcher(Integer userId) {
    User user = new User();
    user.setUserId(userId);
    user.addRole(
        new UserRole(UserRoles.RESEARCHER.getRoleId(), UserRoles.RESEARCHER.getRoleName()));
    return user;
  }

  private StudyComment comment(Integer commentId, Integer userId, Integer rating) {
    return new StudyComment(
        commentId, 1, userId, rating, "text", null, null, "Name", "Institution");
  }
}
