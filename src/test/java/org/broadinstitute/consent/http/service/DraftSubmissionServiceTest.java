package org.broadinstitute.consent.http.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.storage.BlobId;
import com.google.gson.Gson;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.StreamingOutput;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.broadinstitute.consent.http.cloudstore.GCSService;
import org.broadinstitute.consent.http.db.DAOTestHelper;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.DraftSubmission;
import org.broadinstitute.consent.http.models.DraftSubmissionInterface;
import org.broadinstitute.consent.http.models.FileStorageObject;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DraftSubmissionServiceTest extends DAOTestHelper {

  private DraftSubmissionService draftSubmissionService;

  @BeforeEach
  public void setup() throws IOException {
    GCSService gcsService = mock(GCSService.class);
    lenient().when(gcsService.storeDocument(any(), anyString(), any())).thenReturn(
        BlobId.of(UUID.randomUUID().toString(), UUID.randomUUID().toString()));
    DraftFileStorageService draftFileStorageService = new DraftFileStorageService(jdbi, gcsService,
        fileStorageObjectDAO);
    this.draftSubmissionService = new DraftSubmissionService(jdbi, draftSubmissionDAO,
        draftFileStorageService);
  }

  @Test
  public void testCreateDraftSubmission() throws SQLException {
    User user = createUser();
    DraftSubmissionInterface draft = createDraftSubmission(user, 3);
    assertThat(draftSubmissionDAO.findDraftSubmissionsByUserId(user.getUserId()), hasSize(1));
    Set<DraftSubmissionInterface> storedDrafts = draftSubmissionDAO.findDraftSubmissionsByUserId(
        user.getUserId());
    assertThat(storedDrafts, hasSize(1));
    DraftSubmissionInterface storedDraft = storedDrafts.iterator().next();
    assertThat(storedDraft.getStoredFiles(), hasSize(3));
    assertEquals(storedDraft.getUUID(), draft.getUUID());
  }

  @Test
  public void testCreateDraftSubmissionWithInvalidJson() {
    User user = createUser();
    DraftSubmission draftSubmission = new DraftSubmission("Hello world!",user);
    assertThrows(BadRequestException.class, ()-> draftSubmissionService.insertDraftSubmission(draftSubmission));
  }

  @Test
  public void testGetAuthorizedDraft() throws SQLException {
    User goodUser = createUser();
    User badUser = createUser();
    User adminUser = createUser();
    adminUser.addRole(UserRoles.Admin());
    DraftSubmissionInterface draft = createDraftSubmission(goodUser, 4);
    assertThat(draftSubmissionDAO.findDraftSubmissionsByUserId(goodUser.getUserId()), hasSize(1));
    assertThrows(NotFoundException.class,
        () -> draftSubmissionService.getAuthorizedDraft(UUID.randomUUID(), goodUser));
    assertThrows(NotAuthorizedException.class,
        () -> draftSubmissionService.getAuthorizedDraft(draft.getUUID(), badUser));
    assertThat(draftSubmissionDAO.findDraftSubmissionsByUserId(adminUser.getUserId()), hasSize(0));
    DraftSubmissionInterface adminVisibleDraft = draftSubmissionService.getAuthorizedDraft(
        draft.getUUID(), adminUser);
    assertEquals(adminVisibleDraft.getUUID(), draft.getUUID());
    assertEquals(adminVisibleDraft.getName(), draft.getName());
    assertThat(adminVisibleDraft.getStoredFiles(), hasSize(4));
  }

  @Test
  public void testDeleteDraftSubmission() throws Exception {
    User user = createUser();
    createDraftSubmission(user, 3);
    Set<DraftSubmissionInterface> loadedDrafts = draftSubmissionDAO.findDraftSubmissionsByUserId(
        user.getUserId());
    assertThat(loadedDrafts, hasSize(1));
    draftSubmissionService.deleteDraftSubmission(loadedDrafts.iterator().next(), user);
    assertThat(draftSubmissionDAO.findDraftSubmissionsByUserId(user.getUserId()), hasSize(0));
  }

  @Test
  public void testDeleteDraftsForUser() throws SQLException {
    User user = createUser();
    User user2 = createUser();
    createDraftSubmission(user, 3);
    createDraftSubmission(user2, 1);
    createDraftSubmission(user2, 4);
    assertThat(draftSubmissionService.findDraftSubmissionsForUser(user2), hasSize(2));
    assertThat(draftSubmissionService.findDraftSubmissionsForUser(user), hasSize(1));
    draftSubmissionService.deleteDraftsByUser(user2);
    assertThat(draftSubmissionService.findDraftSubmissionsForUser(user), hasSize(1));
    assertThat(draftSubmissionService.findDraftSubmissionsForUser(user2), hasSize(0));
    draftSubmissionService.deleteDraftsByUser(user2);
  }

  @Test
  public void testDeleteAttachmentFromDraft() throws SQLException {
    User user = createUser();
    DraftSubmissionInterface draft = createDraftSubmission(user, 3);
    Set<FileStorageObject> storedFiles = draft.getStoredFiles();
    assertThat(storedFiles, hasSize(3));
    for (FileStorageObject file : storedFiles) {
      draftSubmissionService.deleteDraftAttachment(draft, user, file.getFileStorageObjectId());
    }
    assertThat(draftSubmissionService.getAuthorizedDraft(draft.getUUID(), user).getStoredFiles(),
        hasSize(0));
  }

  @Test
  public void testStreamingOutput() throws SQLException, IOException {
    User user = createUser();
    DraftSubmissionInterface draft = createDraftSubmission(user, 1);
    StreamingOutput output = draftSubmissionService.draftAsJson(draft);
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    output.write(byteArrayOutputStream);
    byteArrayOutputStream.close();
    Gson gson = GsonUtil.buildGson();
    StreamingDeserializer streamedData = gson.fromJson(byteArrayOutputStream.toString(),
        StreamingDeserializer.class);
    assertEquals(draft.getCreateDate().getTime(), streamedData.meta.getCreateDate().getTime());
    assertEquals("{}", streamedData.document.toString());
  }

  @Test
  public void testUpdateDraftSubmission() throws SQLException {
    User user = createUser();
    DraftSubmissionInterface draft = createDraftSubmission(user, 1);
    String updatedJson = "{\"study\": \"My example study\"}";
    String newDraftName = "My favorite draft";
    String originalDocumentJson = draft.getJson();
    Date originalDocumentDate = draft.getUpdateDate();
    assertEquals(originalDocumentJson, draft.getJson());
    assertEquals(originalDocumentDate, draft.getUpdateDate());
    assertNotEquals(draft.getName(), newDraftName);
    draft.setName(newDraftName);
    draft.setJson(updatedJson);
    draftSubmissionService.updateDraftSubmission(draft, user);
    DraftSubmissionInterface updatedDraft = draftSubmissionService.getAuthorizedDraft(
        draft.getUUID(), user);
    assertEquals(draft.getUUID(), updatedDraft.getUUID());
    assertEquals(newDraftName, updatedDraft.getName());
    assertEquals(updatedJson, updatedDraft.getJson());
  }

  @NotNull
  private DraftSubmissionInterface createDraftSubmission(User user, Integer numberOfFiles)
      throws SQLException {
    DraftSubmission draft = new DraftSubmission("{}", user);
    draftSubmissionService.insertDraftSubmission(draft);
    Map<String, FormDataBodyPart> mapOfFiles = getRandomFiles(numberOfFiles);
    return draftSubmissionService.addAttachments(draft, user, mapOfFiles);
  }

  private static class StreamingDeserializer {

    private final Object document;
    private final DraftSubmission meta;

    public StreamingDeserializer(String document, DraftSubmission meta) {
      this.document = document;
      this.meta = meta;
    }
  }

}
