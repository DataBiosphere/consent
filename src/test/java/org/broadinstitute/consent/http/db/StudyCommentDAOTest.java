package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
    List<StudyComment> comments = studyCommentDAO.findByStudyId(studyId);
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
    List<StudyComment> comments = studyCommentDAO.findByStudyId(studyId);
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

    List<StudyComment> comments = studyCommentDAO.findByStudyId(studyId);

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
    assertEquals(1, studyCommentDAO.findByStudyId(studyId).size());

    // Nor can the author reach it through a different study's id
    Integer otherStudyId = insertStudy();
    assertEquals(0, studyCommentDAO.deleteOwn(otherStudyId, commentId, user.getUserId()));
    assertEquals(1, studyCommentDAO.findByStudyId(studyId).size());

    // The author can
    assertEquals(1, studyCommentDAO.deleteOwn(studyId, commentId, user.getUserId()));
    assertTrue(studyCommentDAO.findByStudyId(studyId).isEmpty());
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
