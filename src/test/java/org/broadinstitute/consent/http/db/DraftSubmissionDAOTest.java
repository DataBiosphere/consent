package org.broadinstitute.consent.http.db;

import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import org.broadinstitute.consent.http.models.DraftSubmission;
import org.broadinstitute.consent.http.models.DraftSubmissionInterface;
import org.broadinstitute.consent.http.models.User;
import org.jdbi.v3.core.statement.UnableToExecuteStatementException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
class DraftSubmissionDAOTest extends DAOTestHelper {

  @Test
  public void testInsertOperation() {
    String jsonText = "{\"Name\": \"Greg\"}";
    User user1 = createUser();
    User user2 = createUser();
    DraftSubmission draft = new DraftSubmission(jsonText, null, user1);
    UUID uuid = draft.getUUID();
    Date createDate = draft.getCreateDate();
    Date updateDate = draft.getUpdateDate();
    String name = draft.getName();
    draftSubmissionDAO.insert(draft.getName(), createDate.toInstant(), user1.getUserId(),
        draft.getJson(), draft.getUUID(), draft.getClass().getCanonicalName());
    Set<DraftSubmission> user1submissions = draftSubmissionDAO.findDraftSubmissionsByUserId(
        user1.getUserId());
    assertThat(user1submissions, hasSize(1));
    DraftSubmissionInterface returnedDraft = user1submissions.iterator().next();
    assertEquals(jsonText, returnedDraft.getJson());
    assertEquals(uuid, returnedDraft.getUUID());
    assertEquals(createDate.getTime(), returnedDraft.getCreateDate().getTime());
    assertEquals(updateDate.getTime(), returnedDraft.getUpdateDate().getTime());
    assertEquals(name, returnedDraft.getName());
    assertEquals(returnedDraft.getCreateUser().getUserId(), user1.getUserId());
    assertEquals(returnedDraft.getCreateUser().getEmail(), user1.getEmail());
    assertEquals(returnedDraft.getCreateUser().getInstitutionId(), user1.getInstitutionId());
    assertEquals(returnedDraft.getCreateUser().getEraCommonsId(), user1.getEraCommonsId());
    assertEquals(returnedDraft.getUpdateUser().getUserId(), user1.getUserId());
    assertEquals(returnedDraft.getUpdateUser().getEmail(), user1.getEmail());
    assertEquals(returnedDraft.getUpdateUser().getInstitutionId(), user1.getInstitutionId());
    assertEquals(returnedDraft.getUpdateUser().getEraCommonsId(), user1.getEraCommonsId());
    Set<DraftSubmission> user2submissions = draftSubmissionDAO.findDraftSubmissionsByUserId(
        user2.getUserId());
    assertThat(user2submissions, hasSize(0));
  }

  @Test
  public void testUniqueUUIDConstraint() {
    String jsonText = "{\"Name\": \"Greg\"}";
    User user1 = createUser();
    DraftSubmission draft = new DraftSubmission(jsonText, null, user1);
    draftSubmissionDAO.insert(draft.getName(), draft.getCreateDate().toInstant(), user1.getUserId(),
        draft.getJson(), draft.getUUID(), draft.getClass().getCanonicalName());
    assertThrows(UnableToExecuteStatementException.class,
        () -> draftSubmissionDAO.insert(draft.getName(), draft.getCreateDate().toInstant(),
            user1.getUserId(), draft.getJson(), draft.getUUID(),
            draft.getClass().getCanonicalName()));
  }

  @Test
  public void testUpdateOperation() {
    String jsonText = "{\"Name\": \"Greg\"}";
    User user1 = createUser();
    DraftSubmission draft = new DraftSubmission(jsonText, null, user1);
    UUID uuid = draft.getUUID();
    Date createDate = draft.getCreateDate();
    draftSubmissionDAO.insert(draft.getName(), createDate.toInstant(), user1.getUserId(),
        draft.getJson(), draft.getUUID(), draft.getClass().getCanonicalName());
    Set<DraftSubmission> draftSubmissions = draftSubmissionDAO.findDraftSubmissionsById(uuid);
    assertThat(draftSubmissions, hasSize(1));
    DraftSubmission returnedDraft = draftSubmissions.iterator().next();
    assertEquals(jsonText, returnedDraft.getJson());
    assertEquals(uuid, returnedDraft.getUUID());
    String revisedJson = "{\"Name\": \"Bob\"}";
    returnedDraft.setJson(revisedJson);
    returnedDraft.setUpdateDate(new Date());
    draftSubmissionDAO.updateDraftSubmissionByDraftSubmissionUUID(returnedDraft.getName(),
        returnedDraft.getUpdateDate().toInstant(), user1.getUserId(), returnedDraft.getJson(),
        returnedDraft.getUUID(), returnedDraft.getClass().getCanonicalName());
    draftSubmissions = draftSubmissionDAO.findDraftSubmissionsById(uuid);
    assertThat(draftSubmissions, hasSize(1));
    returnedDraft = draftSubmissions.iterator().next();
    assertEquals(revisedJson, returnedDraft.getJson());
    assertEquals(uuid, returnedDraft.getUUID());
  }

  @Test
  public void testFindByUserOperation() {
    User user1 = createUser();
    User user2 = createUser();
    DraftSubmission draft = new DraftSubmission("{\"Name\": \"User1\"}", null, user1);
    DraftSubmission draft2  =  new DraftSubmission("{\"Name\": \"User2\"}", null, user2);
    draftSubmissionDAO.insert(draft.getName(), draft.getCreateDate().toInstant(), user1.getUserId(),
        draft.getJson(), draft.getUUID(), draft.getClass().getCanonicalName());
    draftSubmissionDAO.insert(draft2.getName(), draft2.getCreateDate().toInstant(), user2.getUserId(),
        draft2.getJson(), draft2.getUUID(), draft2.getClass().getCanonicalName());
    Set<DraftSubmission> user1submissions = draftSubmissionDAO.findDraftSubmissionsByUserId(
        user1.getUserId());
    assertThat(user1submissions, hasSize(1));
    Set<DraftSubmission> user2submissions = draftSubmissionDAO.findDraftSubmissionsByUserId(
        user2.getUserId());
    assertThat(user2submissions, hasSize(1));
  }

  @Test
  public void testDeleteOperation() {
    User user1 = createUser();
    DraftSubmission draft = new DraftSubmission("{\"Name\": \"First\"}", null, user1);
    DraftSubmission draft2 = new DraftSubmission("{\"Name\": \"Second\"}", null, user1);
    draftSubmissionDAO.insert(draft.getName(), draft.getCreateDate().toInstant(), user1.getUserId(),
        draft.getJson(), draft.getUUID(), draft.getClass().getCanonicalName());
    draftSubmissionDAO.insert(draft2.getName(), draft2.getCreateDate().toInstant(), user1.getUserId(),
        draft2.getJson(), draft2.getUUID(), draft2.getClass().getCanonicalName());
    Set<DraftSubmission> user1submissions = draftSubmissionDAO.findDraftSubmissionsByUserId(
        user1.getUserId());
    assertThat(user1submissions, hasSize(2));
    draftSubmissionDAO.deleteDraftByUUIDList(List.of(draft2.getUUID()));
    user1submissions = draftSubmissionDAO.findDraftSubmissionsByUserId(user1.getUserId());
    assertThat(user1submissions, hasSize(1));

  }

}
