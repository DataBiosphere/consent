package org.broadinstitute.consent.http.db;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.cloud.storage.BlobId;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.broadinstitute.consent.http.enumeration.FileCategory;
import org.broadinstitute.consent.http.models.Draft;
import org.broadinstitute.consent.http.models.DraftInterface;
import org.broadinstitute.consent.http.models.DraftSummary;
import org.broadinstitute.consent.http.models.FileStorageObject;
import org.broadinstitute.consent.http.models.User;
import org.jdbi.v3.core.statement.UnableToExecuteStatementException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
class DraftDAOTest extends DAOTestHelper {

  @Test
  public void testInsertOperation() {
    String jsonText = "{\"Name\": \"Greg\"}";
    User user1 = createUser();
    User user2 = createUser();
    Draft draft = new Draft(jsonText, user1);
    UUID uuid = draft.getUUID();
    Date createDate = draft.getCreateDate();
    Date updateDate = draft.getUpdateDate();
    String name = draft.getName();
    draftDAO.insert(draft.getName(), createDate.toInstant(), user1.getUserId(),
        draft.getJson(), draft.getUUID(), draft.getClass().getCanonicalName());
    Set<DraftInterface> user1submissions = draftDAO.findDraftsByUserId(
        user1.getUserId());
    assertThat(user1submissions, hasSize(1));
    DraftInterface returnedDraft = user1submissions.iterator().next();
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
    Set<DraftInterface> user2submissions = draftDAO.findDraftsByUserId(
        user2.getUserId());
    assertThat(user2submissions, hasSize(0));
  }

  @Test
  public void testUniqueUUIDConstraint() {
    String jsonText = "{\"Name\": \"Greg\"}";
    User user1 = createUser();
    Draft draft = new Draft(jsonText, user1);
    draftDAO.insert(draft.getName(), draft.getCreateDate().toInstant(), user1.getUserId(),
        draft.getJson(), draft.getUUID(), draft.getClass().getCanonicalName());
    assertThrows(UnableToExecuteStatementException.class,
        () -> draftDAO.insert(draft.getName(), draft.getCreateDate().toInstant(),
            user1.getUserId(), draft.getJson(), draft.getUUID(),
            draft.getClass().getCanonicalName()));
  }

  @Test
  public void testUpdateOperation() {
    String jsonText = "{\"Name\": \"Greg\"}";
    User user1 = createUser();
    Draft draft = new Draft(jsonText, user1);
    UUID uuid = draft.getUUID();
    Date createDate = draft.getCreateDate();
    draftDAO.insert(draft.getName(), createDate.toInstant(), user1.getUserId(),
        draft.getJson(), draft.getUUID(), draft.getClass().getCanonicalName());
    DraftInterface returnedDraft = draftDAO.findDraftById(uuid);
    assertFalse(Objects.isNull(returnedDraft));
    assertEquals(jsonText, returnedDraft.getJson());
    assertEquals(uuid, returnedDraft.getUUID());
    String revisedJson = "{\"Name\": \"Bob\"}";
    returnedDraft.setJson(revisedJson);
    returnedDraft.setUpdateDate(new Date());
    draftDAO.updateDraftByDraftUUID(returnedDraft.getName(),
        returnedDraft.getUpdateDate().toInstant(), user1.getUserId(), returnedDraft.getJson(),
        returnedDraft.getUUID(), returnedDraft.getClass().getCanonicalName());
    returnedDraft = draftDAO.findDraftById(uuid);
    assertFalse(Objects.isNull(returnedDraft));
    assertEquals(revisedJson, returnedDraft.getJson());
    assertEquals(uuid, returnedDraft.getUUID());
  }

  @Test
  public void testFindByUserOperations() {
    User user1 = createUser();
    User user2 = createUser();
    Draft draft = new Draft("{\"Name\": \"User1\"}", user1);
    Draft draft2 = new Draft("{\"Name\": \"User2\"}", user2);
    draftDAO.insert(draft.getName(), draft.getCreateDate().toInstant(), user1.getUserId(),
        draft.getJson(), draft.getUUID(), draft.getClass().getCanonicalName());
    draftDAO.insert(draft2.getName(), draft2.getCreateDate().toInstant(),
        user2.getUserId(),
        draft2.getJson(), draft2.getUUID(), draft2.getClass().getCanonicalName());
    Set<DraftInterface> user1submissions = draftDAO.findDraftsByUserId(
        user1.getUserId());
    Set<DraftSummary> user1summaries = draftDAO.findDraftSummariesByUserId(
        user1.getUserId());
    assertThat(user1submissions, hasSize(1));
    assertThat(user1summaries, hasSize(1));
    DraftSummary user1DraftSummary = user1summaries.iterator().next();
    DraftInterface user1DraftSubmission = user1submissions.iterator().next();
    summaryMatchesDetails(user1DraftSummary, user1DraftSubmission);
    Set<DraftInterface> user2submissions = draftDAO.findDraftsByUserId(
        user2.getUserId());
    Set<DraftSummary> user2summaries = draftDAO.findDraftSummariesByUserId(
        user2.getUserId());
    DraftSummary user2DraftSummary = user2summaries.iterator().next();
    DraftInterface user2DraftSubmission = user2submissions.iterator().next();
    assertThat(user2submissions, hasSize(1));
    summaryMatchesDetails(user2DraftSummary, user2DraftSubmission);
  }

  @Test
  public void testDraftsWithFiles() {
    User user1 = createUser();
    Draft draft = new Draft("{\"Name\": \"User1\"}", user1);
    FileStorageObject fso = new FileStorageObject();
    fso.setBlobId(BlobId.of(UUID.randomUUID().toString(), UUID.randomUUID().toString()));
    fso.setFileName("filename");
    fso.setMediaType("application/text");
    fso.setCategory(FileCategory.DRAFT_UPLOADED_FILE);
    fso.setCreateDate(draft.getCreateDate().toInstant());
    fso.setUpdateDate(draft.getUpdateDate().toInstant());
    fso.setCreateUserId(user1.getUserId());
    fso.setEntityId(draft.getUUID().toString());
    draftDAO.insert(draft.getName(), draft.getCreateDate().toInstant(),
        draft.getCreateUser().getUserId(), draft.getJson(), draft.getUUID(),
        draft.getClass().getCanonicalName());
    fileStorageObjectDAO.insertNewFile(fso.getFileName(), fso.getCategory().getValue(),
        fso.getBlobId().toGsUtilUri(), fso.getMediaType(), draft.getUUID().toString(),
        user1.getUserId(), fso.getCreateDate());
    Set<DraftInterface> draftSubmissions = draftDAO.findDraftsByUserId(
        user1.getUserId());
    assertThat(draftSubmissions, hasSize(1));
    Set<FileStorageObject> storedFiles = draftSubmissions.iterator().next().getStoredFiles();
    assertThat(storedFiles, hasSize(1));
    FileStorageObject storedFile = storedFiles.iterator().next();
    assertEquals(storedFile.getBlobId(), fso.getBlobId());
    assertEquals(storedFile.getFileName(), fso.getFileName());
    assertEquals(draft.getUUID().toString(), fso.getEntityId());
    assertEquals(draft.getCreateDate().toInstant(), fso.getCreateDate());
  }

  @Test
  public void testDeleteOperation() {
    User user1 = createUser();
    Draft draft = new Draft("{\"Name\": \"First\"}", user1);
    Draft draft2 = new Draft("{\"Name\": \"Second\"}", user1);
    draftDAO.insert(draft.getName(), draft.getCreateDate().toInstant(), user1.getUserId(),
        draft.getJson(), draft.getUUID(), draft.getClass().getCanonicalName());
    draftDAO.insert(draft2.getName(), draft2.getCreateDate().toInstant(),
        user1.getUserId(),
        draft2.getJson(), draft2.getUUID(), draft2.getClass().getCanonicalName());
    Set<DraftInterface> user1submissions = draftDAO.findDraftsByUserId(
        user1.getUserId());
    assertThat(user1submissions, hasSize(2));
    draftDAO.deleteDraftByUUIDList(List.of(draft2.getUUID()));
    user1submissions = draftDAO.findDraftsByUserId(user1.getUserId());
    assertThat(user1submissions, hasSize(1));

  }

  private void summaryMatchesDetails(DraftSummary draftSummary,
      DraftInterface draftInterface) {
    assertEquals(draftSummary.getId(), draftInterface.getUUID());
    assertEquals(draftSummary.getName(), draftInterface.getName());
    assertEquals(draftSummary.getCreateDate(), draftInterface.getCreateDate());
    assertEquals(draftSummary.getUpdateDate(), draftInterface.getUpdateDate());
  }
}
