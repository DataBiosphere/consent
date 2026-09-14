package org.broadinstitute.consent.http.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
import org.broadinstitute.consent.http.enumeration.UserRoles;
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
  @Mock private LibraryCardDAO libraryCardDAO;
  @Mock private DatasetService datasetService;

  private StudyCommentService service;

  @BeforeEach
  void setUp() {
    when(jdbi.onDemand(StudyCommentDAO.class)).thenReturn(commentDAO);
    when(jdbi.onDemand(LibraryCardDAO.class)).thenReturn(libraryCardDAO);
    service = new StudyCommentService(jdbi, datasetService);
  }

  @Test
  void testListStudyNotFound() {
    User user = researcher(10);
    when(datasetService.requireReadableStudy(1, user)).thenThrow(new NotFoundException());

    assertThrows(NotFoundException.class, () -> service.list(1, user, 25, 0));
  }

  @Test
  void testListNoCommentsHasNullAverage() {
    when(commentDAO.findByStudyId(1, 25, 0)).thenReturn(List.of());
    when(commentDAO.averageRatingByStudyId(1)).thenReturn(null);
    when(commentDAO.countByStudyId(1)).thenReturn(0);

    StudyCommentsSummary summary = service.list(1, researcher(10), 25, 0);

    assertEquals(List.of(), summary.comments());
    assertNull(summary.averageRating());
    assertEquals(0, summary.total());
  }

  /**
   * The average and the total describe the whole study, not the page. A caller paging through must
   * not see the average move under them, and needs the total to know more pages exist.
   */
  @Test
  void testListReportsTheAverageAndTotalAcrossEveryComment() {
    when(commentDAO.findByStudyId(1, 2, 0)).thenReturn(List.of(comment(1, 1, 4), comment(2, 2, 2)));
    when(commentDAO.averageRatingByStudyId(1)).thenReturn(3.0);
    when(commentDAO.countByStudyId(1)).thenReturn(7);

    StudyCommentsSummary summary = service.list(1, researcher(10), 2, 0);

    assertEquals(2, summary.comments().size());
    assertEquals(3.0, summary.averageRating());
    assertEquals(7, summary.total());
  }

  /** The page a caller asks for is the page the DAO is asked for. */
  @Test
  void testListPassesThePageThrough() {
    when(commentDAO.findByStudyId(1, 10, 20)).thenReturn(List.of());
    when(commentDAO.countByStudyId(1)).thenReturn(30);

    service.list(1, researcher(10), 10, 20);

    verify(commentDAO).findByStudyId(1, 10, 20);
  }

  /**
   * Paging puts a reader's own comment on an unpredictable page, so it is carried alongside the
   * page. Without it a client scanning only the returned page would offer to add a comment to
   * someone who already has one, and the save would silently revise instead.
   */
  @Test
  void testListCarriesTheCallersOwnCommentEvenWhenItIsNotOnThePage() {
    StudyComment own = comment(99, 10, 5);
    when(commentDAO.findByStudyId(1, 25, 0)).thenReturn(List.of(comment(1, 11, 3)));
    when(commentDAO.countByStudyId(1)).thenReturn(40);
    when(commentDAO.findByStudyIdAndUserId(1, 10)).thenReturn(own);

    StudyCommentsSummary summary = service.list(1, researcher(10), 25, 0);

    assertEquals(own, summary.yourComment());
    assertEquals(List.of(comment(1, 11, 3)), summary.comments());
  }

  @Test
  void testListLeavesYourCommentNullWhenTheCallerHasNone() {
    when(commentDAO.findByStudyId(1, 25, 0)).thenReturn(List.of());
    when(commentDAO.countByStudyId(1)).thenReturn(0);
    when(commentDAO.findByStudyIdAndUserId(1, 10)).thenReturn(null);

    assertNull(service.list(1, researcher(10), 25, 0).yourComment());
  }

  @Test
  void testPostStudyNotFound() {
    User user = researcher(10);
    when(datasetService.requireReadableStudy(1, user)).thenThrow(new NotFoundException());

    assertThrows(NotFoundException.class, () -> service.post(1, user, 5, "text"));
  }

  @Test
  void testPostRequiresResearcherRole() {

    User user = new User();
    user.setUserId(10);

    assertThrows(ForbiddenException.class, () -> service.post(1, user, 5, "text"));
  }

  @Test
  void testPostRequiresLibraryCard() {

    User user = researcher(10);
    when(libraryCardDAO.findLibraryCardIdByUserId(10)).thenReturn(null);

    assertThrows(ForbiddenException.class, () -> service.post(1, user, 5, "text"));
  }

  @Test
  void testPostRejectsInvalidRatings() {

    User user = researcher(10);
    when(libraryCardDAO.findLibraryCardIdByUserId(10)).thenReturn(100);

    assertThrows(BadRequestException.class, () -> service.post(1, user, null, "text"));
    assertThrows(BadRequestException.class, () -> service.post(1, user, 0, "text"));
    assertThrows(BadRequestException.class, () -> service.post(1, user, 6, "text"));
  }

  @Test
  void testPostUpsertsAndReturnsComment() {

    User user = researcher(10);
    when(libraryCardDAO.findLibraryCardIdByUserId(10)).thenReturn(100);
    when(commentDAO.upsert(1, 10, 5, "text")).thenReturn(7);
    StudyComment expected = comment(7, 10, 5);
    when(commentDAO.findById(1, 7)).thenReturn(expected);

    StudyComment posted = service.post(1, user, 5, "text");

    assertEquals(expected, posted);
  }

  /**
   * The list carries every comment on the study, so one caller cannot make it arbitrarily large.
   */
  @Test
  void testPostRejectsAnOverlongComment() {
    User user = researcher(10);
    when(libraryCardDAO.findLibraryCardIdByUserId(10)).thenReturn(100);
    String tooLong = "x".repeat(StudyCommentService.MAX_COMMENT_LENGTH + 1);

    assertThrows(BadRequestException.class, () -> service.post(1, user, 4, tooLong));
    verify(commentDAO, never()).upsert(any(), any(), any(), any());
  }

  @Test
  void testPostAcceptsACommentAtTheLimit() {
    User user = researcher(10);
    when(libraryCardDAO.findLibraryCardIdByUserId(10)).thenReturn(100);
    String atLimit = "x".repeat(StudyCommentService.MAX_COMMENT_LENGTH);
    when(commentDAO.upsert(1, 10, 4, atLimit)).thenReturn(7);
    when(commentDAO.findById(1, 7)).thenReturn(comment(7, 10, 4));

    service.post(1, user, 4, atLimit);

    verify(commentDAO).upsert(1, 10, 4, atLimit);
  }

  /** Deleted between the write and the read: absent, not a 500. */
  @Test
  void testPostReportsAVanishedCommentAsNotFound() {
    User user = researcher(10);
    when(libraryCardDAO.findLibraryCardIdByUserId(10)).thenReturn(100);
    when(commentDAO.upsert(1, 10, 4, "text")).thenReturn(7);
    when(commentDAO.findById(1, 7)).thenReturn(null);

    assertThrows(NotFoundException.class, () -> service.post(1, user, 4, "text"));
  }

  @Test
  void testDelete() {
    User user = researcher(10);
    when(commentDAO.deleteOwn(1, 7, 10)).thenReturn(1);

    service.delete(1, 7, user);

    verify(commentDAO).deleteOwn(1, 7, 10);
  }

  @Test
  void testDeleteNotFound() {
    User user = researcher(10);
    when(commentDAO.deleteOwn(1, 7, 10)).thenReturn(0);

    assertThrows(NotFoundException.class, () -> service.delete(1, 7, user));
  }

  /** The study id in the path is load-bearing: a comment is not reachable through another study. */
  @Test
  void testDeleteStudyNotFound() {
    User user = researcher(10);
    when(datasetService.requireReadableStudy(999, user)).thenThrow(new NotFoundException());

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
    when(datasetService.requireReadableStudy(any(), any()))
        .thenThrow(new NotFoundException("Study not found"));

    assertThrows(NotFoundException.class, () -> service.list(1, user, 25, 0));
    assertThrows(NotFoundException.class, () -> service.post(1, user, 5, "text"));
    assertThrows(NotFoundException.class, () -> service.delete(1, 7, user));
  }

  /**
   * Comments on a public study are readable by every authenticated user, so admins are the
   * moderation path: an admin removes anyone's comment, not only their own.
   */
  @Test
  void testAdminDeletesSomeoneElsesComment() {
    User admin = admin(99);
    when(commentDAO.deleteAny(1, 7)).thenReturn(1);

    service.delete(1, 7, admin);

    verify(commentDAO).deleteAny(1, 7);
    verify(commentDAO, never()).deleteOwn(any(), any(), any());
  }

  /** A moderation delete of a comment that is not there is still absent, not a server error. */
  @Test
  void testAdminDeleteNotFound() {
    User admin = admin(99);
    when(commentDAO.deleteAny(1, 7)).thenReturn(0);

    assertThrows(NotFoundException.class, () -> service.delete(1, 7, admin));
  }

  /**
   * A researcher stays scoped to their own comment. Without this the admin branch would be the only
   * thing standing between a researcher and someone else's rating.
   */
  @Test
  void testResearcherDeleteStaysScopedToTheirOwnComment() {
    User user = researcher(10);
    when(commentDAO.deleteOwn(1, 7, 10)).thenReturn(1);

    service.delete(1, 7, user);

    verify(commentDAO, never()).deleteAny(any(), any());
  }

  /**
   * A study's creator may rate it, given the Researcher role and a library card - the same bar as
   * anyone else. requireStudy already lets them read a study they own, and post applies no extra
   * distance test, so this pins the decision rather than leaving it to be read out of the gate.
   */
  @Test
  void testStudyCreatorMayRateTheirOwnStudy() {
    User creator = researcher(10);
    when(libraryCardDAO.findLibraryCardIdByUserId(10)).thenReturn(5);
    when(commentDAO.upsert(1, 10, 4, "text")).thenReturn(7);
    when(commentDAO.findById(1, 7)).thenReturn(comment(7, 10, 4));

    assertEquals(comment(7, 10, 4), service.post(1, creator, 4, "text"));
  }

  private User admin(Integer userId) {
    User user = new User();
    user.setUserId(userId);
    user.addRole(new UserRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName()));
    return user;
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
