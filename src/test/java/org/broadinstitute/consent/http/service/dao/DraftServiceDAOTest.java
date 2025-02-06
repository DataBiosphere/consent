package org.broadinstitute.consent.http.service.dao;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.storage.BlobId;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.MediaType;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;
import org.broadinstitute.consent.http.cloudstore.GCSService;
import org.broadinstitute.consent.http.db.DAOTestHelper;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.DraftInterface;
import org.broadinstitute.consent.http.models.DraftStudyDataset;
import org.broadinstitute.consent.http.models.FileStorageObject;
import org.broadinstitute.consent.http.models.User;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DraftServiceDAOTest extends DAOTestHelper {

  @Mock
  GCSService gcsService;

  private DraftServiceDAO draftServiceDAO;

  @BeforeEach
  void setup() throws IOException {
    DraftFileStorageServiceDAO draftFileStorageServiceDAO = new DraftFileStorageServiceDAO(jdbi,
        gcsService,
        fileStorageObjectDAO);
    this.draftServiceDAO = new DraftServiceDAO(jdbi, draftDAO,
        draftFileStorageServiceDAO);
  }

  @Test
  void testCreateDraft() throws SQLException, IOException {
    when(gcsService.storeDocument(any(), anyString(), any())).thenReturn(
        BlobId.of(UUID.randomUUID().toString(), UUID.randomUUID().toString()));
    User user = createUser();
    DraftInterface draft = createDraft(user, 3);
    assertThat(draftDAO.findDraftsByUserId(user.getUserId()), hasSize(1));
    Collection<DraftInterface> storedDrafts = draftDAO.findDraftsByUserId(
        user.getUserId());
    assertThat(storedDrafts, hasSize(1));
    DraftInterface storedDraft = storedDrafts.iterator().next();
    assertThat(storedDraft.getStoredFiles(), hasSize(3));
    assertEquals(storedDraft.getUUID(), draft.getUUID());
  }

  @Test
  void testCreateDraftWithInvalidJson() {
    User user = createUser();
    DraftStudyDataset draft = new DraftStudyDataset("Hello world!", user);
    assertThrows(BadRequestException.class, () -> draftServiceDAO.insertDraft(draft));
  }

  @Test
  void testThinUserIsReturnedFromDraft() throws SQLException, IOException {
    when(gcsService.storeDocument(any(), anyString(), any())).thenReturn(
        BlobId.of(UUID.randomUUID().toString(), UUID.randomUUID().toString()));
    User user = createUser();
    DraftInterface draft = createDraft(user, 3);
    assertThat(user.getRoles(), hasSize(greaterThan(0)));
    assertThinUser(draft.getCreateUser());
    assertThinUser(draft.getUpdateUser());
  }

  @Test
  void testGetAuthorizedDraft() throws SQLException, IOException {
    when(gcsService.storeDocument(any(), anyString(), any())).thenReturn(
        BlobId.of(UUID.randomUUID().toString(), UUID.randomUUID().toString()));
    User goodUser = createUser();
    User badUser = createUser();
    User adminUser = createUser();
    adminUser.addRole(UserRoles.Admin());
    DraftInterface draft = createDraft(goodUser, 4);
    assertThat(draftDAO.findDraftsByUserId(goodUser.getUserId()), hasSize(1));
    assertThrows(NotFoundException.class,
        () -> draftServiceDAO.getAuthorizedDraft(UUID.randomUUID(), goodUser));
    assertThrows(NotAuthorizedException.class,
        () -> draftServiceDAO.getAuthorizedDraft(draft.getUUID(), badUser));
    assertThat(draftDAO.findDraftsByUserId(adminUser.getUserId()), hasSize(0));
    DraftInterface adminVisibleDraft = draftServiceDAO.getAuthorizedDraft(
        draft.getUUID(), adminUser);
    assertEquals(adminVisibleDraft.getUUID(), draft.getUUID());
    assertEquals(adminVisibleDraft.getName(), draft.getName());
    assertThat(adminVisibleDraft.getStoredFiles(), hasSize(4));
  }

  @Test
  void testDeleteDraft() throws Exception {
    when(gcsService.storeDocument(any(), anyString(), any())).thenReturn(
        BlobId.of(UUID.randomUUID().toString(), UUID.randomUUID().toString()));
    User user = createUser();
    createDraft(user, 3);
    Collection<DraftInterface> loadedDrafts = draftDAO.findDraftsByUserId(
        user.getUserId());
    assertThat(loadedDrafts, hasSize(1));
    draftServiceDAO.deleteDraft(loadedDrafts.iterator().next(), user);
    assertThat(draftDAO.findDraftsByUserId(user.getUserId()), hasSize(0));
  }

  @Test
  void testDeleteDraftsForUser() throws SQLException, IOException {
    when(gcsService.storeDocument(any(), anyString(), any())).thenReturn(
        BlobId.of(UUID.randomUUID().toString(), UUID.randomUUID().toString()));
    User user = createUser();
    User user2 = createUser();
    createDraft(user, 3);
    createDraft(user2, 1);
    createDraft(user2, 4);
    assertThat(draftServiceDAO.findDraftsForUser(user2), hasSize(2));
    assertThat(draftServiceDAO.findDraftsForUser(user), hasSize(1));
    draftServiceDAO.deleteDraftsByUser(user2);
    assertThat(draftServiceDAO.findDraftsForUser(user), hasSize(1));
    assertThat(draftServiceDAO.findDraftsForUser(user2), hasSize(0));
    draftServiceDAO.deleteDraftsByUser(user2);
  }

  @Test
  void testAddAttachmentToDraft() throws SQLException, IOException {
    when(gcsService.storeDocument(any(), anyString(), any())).thenReturn(
        BlobId.of(UUID.randomUUID().toString(), UUID.randomUUID().toString()));
    User user = createUser();
    DraftInterface draft = createDraft(user, 3);
    Map<String, FormDataBodyPart> files = getRandomFiles(1);
    List<FileStorageObject> addedAttachments = draftServiceDAO.addAttachments(draft, user, files);
    assertThat(addedAttachments, hasSize(1));
    DraftInterface updatedDraft = draftServiceDAO.getAuthorizedDraft(draft.getUUID(), user);
    assertThat(updatedDraft.getStoredFiles(), hasSize(4));
  }

  @Test
  void testDeleteAttachmentFromDraft() throws SQLException, IOException {
    when(gcsService.storeDocument(any(), anyString(), any())).thenReturn(
        BlobId.of(UUID.randomUUID().toString(), UUID.randomUUID().toString()));
    User user = createUser();
    DraftInterface draft = createDraft(user, 3);
    Set<FileStorageObject> storedFiles = draft.getStoredFiles();
    assertThat(storedFiles, hasSize(3));
    for (FileStorageObject file : storedFiles) {
      draftServiceDAO.deleteDraftAttachment(draft, user, file.getFileStorageObjectId());
    }
    assertThat(draftServiceDAO.getAuthorizedDraft(draft.getUUID(), user).getStoredFiles(),
        hasSize(0));
  }

  @Test
  void testUpdateDraft() throws SQLException {
    User user = createUser();
    DraftInterface draft = createDraft(user, 1);
    String updatedJson = "{\"study\": \"My example study\"}";
    String newDraftName = "My favorite draft";
    String originalDocumentJson = draft.getJson();
    Date originalDocumentDate = draft.getUpdateDate();
    assertEquals(originalDocumentJson, draft.getJson());
    assertEquals(originalDocumentDate, draft.getUpdateDate());
    assertNotEquals(newDraftName, draft.getName());
    draft.setName(newDraftName);
    draft.setJson(updatedJson);
    DraftInterface updatedDraft = draftServiceDAO.updateDraft(draft, user);
    assertEquals(draft.getUUID(), updatedDraft.getUUID());
    assertEquals(newDraftName, updatedDraft.getName());
    assertEquals(updatedJson, updatedDraft.getJson());
    assertThinUser(updatedDraft.getCreateUser());
    assertThinUser(updatedDraft.getUpdateUser());
  }

  @NotNull
  private DraftInterface createDraft(User user, Integer numberOfFiles)
      throws SQLException {
    DraftStudyDataset draft = new DraftStudyDataset("{}", user);
    draftServiceDAO.insertDraft(draft);
    Map<String, FormDataBodyPart> mapOfFiles = getRandomFiles(numberOfFiles);
    draftServiceDAO.addAttachments(draft, user, mapOfFiles);
    return draftServiceDAO.getAuthorizedDraft(draft.getUUID(), user);
  }

  private void assertThinUser(User user) {
    assertThat(user.getRoles(), is(nullValue()));
    assertThat(user.getUserId(), is(notNullValue()));
    assertThat(user.getInstitutionId(), is(nullValue()));
    assertThat(user.getEraCommonsId(), is(nullValue()));
  }

  private Map<String, FormDataBodyPart> getRandomFiles(Integer count) {
    Map<String, FormDataBodyPart> mapOfFiles = new HashMap<>();
    IntStream.range(0, count)
        .forEach(index -> {
          String name = String.format("file%d", index);
          mapOfFiles.put(name, getFormDataBodyPartMock(name));
        });
    return mapOfFiles;
  }

  private FormDataBodyPart getFormDataBodyPartMock(String name) {
    FormDataBodyPart part = mock(FormDataBodyPart.class);
    when(part.getName()).thenReturn(name);
    when(part.getMediaType()).thenReturn(MediaType.MULTIPART_FORM_DATA_TYPE);
    when(part.getValueAs(InputStream.class))
        .thenReturn(new ByteArrayInputStream(EMPTY_JSON_DOCUMENT.getBytes()));
    return part;
  }


}
