package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.broadinstitute.consent.http.models.StudyComment;
import org.broadinstitute.consent.http.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StudyCommentDAOTest extends DAOTestHelper {

  private StudyCommentDAO studyCommentDAO;

  @BeforeEach
  void setUpDao() {
    studyCommentDAO = jdbi.onDemand(StudyCommentDAO.class);
  }

  @Test
  void testUpsertInsertsAndReturnsId() {
    Integer studyId = insertStudy();
    User user = createUserWithInstitution();

    Integer commentId = studyCommentDAO.upsert(studyId, user.getUserId(), 4, "Great study");

    assertNotNull(commentId);
    List<StudyComment> comments = studyCommentDAO.findByStudyId(studyId, 100, 0);
    assertEquals(1, comments.size());
    StudyComment comment = comments.getFirst();
    assertEquals(commentId, comment.studyCommentId());
    assertEquals(studyId, comment.studyId());
    assertEquals(user.getUserId(), comment.userId());
    assertEquals(4, comment.rating());
    assertEquals("Great study", comment.commentText());
    assertEquals(user.getDisplayName(), comment.displayName());
    assertNotNull(comment.institutionName());
    assertNotNull(comment.createDate());
    assertNotNull(comment.updateDate());
  }

  @Test
  void testUpsertOnConflictUpdatesExistingCommentAndReturnsSameId() {
    Integer studyId = insertStudy();
    User user = createUserWithInstitution();

    Integer firstId = studyCommentDAO.upsert(studyId, user.getUserId(), 2, "First take");
    Integer secondId = studyCommentDAO.upsert(studyId, user.getUserId(), 5, "Changed my mind");

    assertEquals(firstId, secondId);
    List<StudyComment> comments = studyCommentDAO.findByStudyId(studyId, 100, 0);
    assertEquals(1, comments.size());
    assertEquals(5, comments.getFirst().rating());
    assertEquals("Changed my mind", comments.getFirst().commentText());
  }

  @Test
  void testFindByStudyIdScopedToStudy() {
    Integer studyId = insertStudy();
    Integer otherStudyId = insertStudy();
    User user = createUserWithInstitution();
    User otherUser = createUser();

    studyCommentDAO.upsert(studyId, user.getUserId(), 3, "On the study");
    studyCommentDAO.upsert(otherStudyId, user.getUserId(), 1, "On another study");
    studyCommentDAO.upsert(studyId, otherUser.getUserId(), 5, null);

    List<StudyComment> comments = studyCommentDAO.findByStudyId(studyId, 100, 0);

    assertEquals(2, comments.size());
    assertTrue(comments.stream().allMatch(c -> studyId.equals(c.studyId())));
    // A commenter without an institution still lists, with a null institution name
    StudyComment noInstitution =
        comments.stream()
            .filter(c -> otherUser.getUserId().equals(c.userId()))
            .findFirst()
            .orElseThrow();
    assertNull(noInstitution.institutionName());
    assertNull(noInstitution.commentText());
  }

  @Test
  void testDeleteOwn() {
    Integer studyId = insertStudy();
    User user = createUserWithInstitution();
    User otherUser = createUser();
    Integer commentId = studyCommentDAO.upsert(studyId, user.getUserId(), 4, "text");

    // Another user cannot delete the comment
    assertEquals(0, studyCommentDAO.deleteOwn(studyId, commentId, otherUser.getUserId()));
    assertEquals(1, studyCommentDAO.findByStudyId(studyId, 100, 0).size());

    // Nor can the author reach it through a different study's id
    Integer otherStudyId = insertStudy();
    assertEquals(0, studyCommentDAO.deleteOwn(otherStudyId, commentId, user.getUserId()));
    assertEquals(1, studyCommentDAO.findByStudyId(studyId, 100, 0).size());

    // The author can
    assertEquals(1, studyCommentDAO.deleteOwn(studyId, commentId, user.getUserId()));
    assertTrue(studyCommentDAO.findByStudyId(studyId, 100, 0).isEmpty());
  }

  /**
   * The moderation delete is not scoped to the author, but it is still scoped to the study in the
   * path - the same way deleteOwn is - so a comment cannot be reached through another study's id.
   */
  @Test
  void testDeleteAny() {
    Integer studyId = insertStudy();
    User author = createUserWithInstitution();
    Integer commentId = studyCommentDAO.upsert(studyId, author.getUserId(), 4, "text");

    Integer otherStudyId = insertStudy();
    assertEquals(0, studyCommentDAO.deleteAny(otherStudyId, commentId));
    assertEquals(1, studyCommentDAO.findByStudyId(studyId, 100, 0).size());

    // Deletes it without being the author
    assertEquals(1, studyCommentDAO.deleteAny(studyId, commentId));
    assertTrue(studyCommentDAO.findByStudyId(studyId, 100, 0).isEmpty());
  }

  /**
   * ck_study_comment_rating is what actually holds the 1-5 range. The service checks it too, but a
   * second writer bypassing the service must not be able to store a rating outside it.
   */
  @Test
  void testRatingRangeIsEnforcedByTheDatabase() {
    Integer studyId = insertStudy();
    User user = createUserWithInstitution();

    for (int rating : new int[] {0, 6}) {
      Exception thrown =
          assertThrows(
              Exception.class,
              () -> studyCommentDAO.upsert(studyId, user.getUserId(), rating, "text"));
      assertTrue(thrown.getMessage().contains("ck_study_comment_rating"));
    }
  }

  /**
   * Revising a comment keeps its place in the list. Ordering is by create_date, so the upsert must
   * leave that alone and move only update_date - otherwise an edit would jump a comment to the top.
   */
  @Test
  void testUpsertPreservesCreateDateAndAdvancesUpdateDate() throws Exception {
    Integer studyId = insertStudy();
    User user = createUserWithInstitution();
    studyCommentDAO.upsert(studyId, user.getUserId(), 3, "first");
    StudyComment before = studyCommentDAO.findByStudyId(studyId, 100, 0).getFirst();

    Thread.sleep(10);
    studyCommentDAO.upsert(studyId, user.getUserId(), 5, "revised");
    StudyComment after = studyCommentDAO.findByStudyId(studyId, 100, 0).getFirst();

    assertEquals(before.createDate(), after.createDate());
    assertTrue(after.updateDate().after(before.updateDate()));
    assertEquals(5, after.rating());
  }

  /** Newest first by creation, so an edited comment does not jump the queue. */
  @Test
  void testFindByStudyIdOrdersNewestFirstByCreation() throws Exception {
    Integer studyId = insertStudy();
    User first = createUserWithInstitution();
    User second = createUserWithInstitution();
    Integer firstId = studyCommentDAO.upsert(studyId, first.getUserId(), 3, "older");
    Thread.sleep(10);
    Integer secondId = studyCommentDAO.upsert(studyId, second.getUserId(), 4, "newer");

    assertEquals(
        List.of(secondId, firstId),
        studyCommentDAO.findByStudyId(studyId, 100, 0).stream()
            .map(StudyComment::studyCommentId)
            .toList());

    // Editing the older one does not move it, because the order is by create_date
    Thread.sleep(10);
    studyCommentDAO.upsert(studyId, first.getUserId(), 5, "older, revised");

    assertEquals(
        List.of(secondId, firstId),
        studyCommentDAO.findByStudyId(studyId, 100, 0).stream()
            .map(StudyComment::studyCommentId)
            .toList());
  }

  @Test
  void testFindByStudyIdPagesThroughTheList() {
    Integer studyId = insertStudy();
    for (int i = 0; i < 3; i++) {
      studyCommentDAO.upsert(studyId, createUserWithInstitution().getUserId(), 3, "c" + i);
    }

    assertEquals(2, studyCommentDAO.findByStudyId(studyId, 2, 0).size());
    assertEquals(1, studyCommentDAO.findByStudyId(studyId, 2, 2).size());
    assertEquals(3, studyCommentDAO.countByStudyId(studyId));
  }

  /** The average covers every comment, so paging cannot change it. */
  @Test
  void testAverageRatingCoversEveryCommentAndIsNullWhenThereAreNone() {
    Integer studyId = insertStudy();
    assertNull(studyCommentDAO.averageRatingByStudyId(studyId));

    studyCommentDAO.upsert(studyId, createUserWithInstitution().getUserId(), 2, "a");
    studyCommentDAO.upsert(studyId, createUserWithInstitution().getUserId(), 4, "b");

    assertEquals(3.0, studyCommentDAO.averageRatingByStudyId(studyId));
  }

  /** A comment is not reachable through another study's id. */
  @Test
  void testFindByIdIsScopedToTheStudy() {
    Integer studyId = insertStudy();
    Integer otherStudyId = insertStudy();
    User user = createUserWithInstitution();
    Integer commentId = studyCommentDAO.upsert(studyId, user.getUserId(), 4, "text");

    assertNotNull(studyCommentDAO.findById(studyId, commentId));
    assertNull(studyCommentDAO.findById(otherStudyId, commentId));
  }

  private Integer insertStudy() {
    User user = createUser();
    return studyDAO.insertStudy(
        randomAlphabetic(20),
        randomAlphabetic(20),
        randomAlphabetic(20),
        null,
        List.of(randomAlphabetic(10)),
        true,
        user.getUserId(),
        Instant.now(),
        UUID.randomUUID());
  }
}
