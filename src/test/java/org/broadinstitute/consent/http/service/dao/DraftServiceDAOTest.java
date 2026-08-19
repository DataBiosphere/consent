package org.broadinstitute.consent.http.service.dao;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
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
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DraftServiceDAOTest extends DAOTestHelper {
  private static GCSService gcsService;
  private static DraftServiceDAO draftServiceDAO;

  @BeforeEach
  void beforeEachTestSetup() throws IOException {
    gcsService = Mockito.mock(GCSService.class);
    when(gcsService.storeDocument(any(), anyString(), any()))
        .thenReturn(BlobId.of(UUID.randomUUID().toString(), UUID.randomUUID().toString()));
    DraftFileStorageServiceDAO draftFileStorageServiceDAO =
        new DraftFileStorageServiceDAO(jdbi, gcsService);
    draftServiceDAO = new DraftServiceDAO(jdbi, draftFileStorageServiceDAO);
  }

  @Test
  void testCreateDraft() throws SQLException {
    User user = createUser();
    DraftInterface draft = createDraft(user, 3);
    assertThat(draftDAO.findDraftsByUserId(user.getUserId()), hasSize(1));
    Collection<DraftInterface> storedDrafts = draftDAO.findDraftsByUserId(user.getUserId());
    assertThat(storedDrafts, hasSize(1));
    DraftInterface storedDraft = storedDrafts.iterator().next();
    assertThat(storedDraft.getStoredFiles(), hasSize(3));
    assertEquals(storedDraft.getUUID(), draft.getUUID());
  }

  @Test
  void testCreateDraftWithInvalidJson() {
    Mockito.reset(gcsService);
    User user = createUser();
    DraftStudyDataset draft = new DraftStudyDataset("Hello world!", user);
    assertThrows(BadRequestException.class, () -> draftServiceDAO.insertDraft(draft));
  }

  @Test
  void testThinUserIsReturnedFromDraft() throws SQLException {
    User user = createUser();
    DraftInterface draft = createDraft(user, 3);
    assertThat(user.getRoles(), hasSize(greaterThan(0)));
    assertThinUser(draft.getCreateUser());
    assertThinUser(draft.getUpdateUser());
  }

  @Test
  void testGetAuthorizedDraft() throws SQLException {
    User goodUser = createUser();
    User badUser = createUser();
    User adminUser = createUser();
    adminUser.addRole(UserRoles.Admin());
    DraftInterface draft = createDraft(goodUser, 4);
    assertThat(draftDAO.findDraftsByUserId(goodUser.getUserId()), hasSize(1));
    assertThrows(
        NotFoundException.class,
        () -> draftServiceDAO.getAuthorizedDraft(UUID.randomUUID(), goodUser));
    assertThrows(
        NotAuthorizedException.class,
        () -> draftServiceDAO.getAuthorizedDraft(draft.getUUID(), badUser));
    assertThat(draftDAO.findDraftsByUserId(adminUser.getUserId()), hasSize(0));
    DraftInterface adminVisibleDraft =
        draftServiceDAO.getAuthorizedDraft(draft.getUUID(), adminUser);
    assertEquals(adminVisibleDraft.getUUID(), draft.getUUID());
    assertEquals(adminVisibleDraft.getName(), draft.getName());
    assertThat(adminVisibleDraft.getStoredFiles(), hasSize(4));
  }

  @Test
  void testChairpersonCannotReadAnotherUsersDraft() throws SQLException {
    // Reaching the draft endpoints does not make a chairperson an owner.
    User owner = createUser();
    User chairperson = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
    DraftInterface draft = createDraft(owner, 1);

    assertThrows(
        NotAuthorizedException.class,
        () -> draftServiceDAO.getAuthorizedDraft(draft.getUUID(), chairperson));
  }

  @Test
  void testChairpersonCanReadTheirOwnDraft() throws SQLException {
    User chairperson = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
    DraftInterface draft = createDraft(chairperson, 1);

    assertEquals(
        draft.getUUID(),
        draftServiceDAO.getAuthorizedDraft(draft.getUUID(), chairperson).getUUID());
  }

  @Test
  void testDeleteDraft() throws Exception {
    User user = createUser();
    createDraft(user, 3);
    Collection<DraftInterface> loadedDrafts = draftDAO.findDraftsByUserId(user.getUserId());
    assertThat(loadedDrafts, hasSize(1));
    draftServiceDAO.deleteDraft(loadedDrafts.iterator().next(), user);
    assertThat(draftDAO.findDraftsByUserId(user.getUserId()), hasSize(0));
  }

  @Test
  void testAddAttachmentToDraft() throws SQLException {
    User user = createUser();
    DraftInterface draft = createDraft(user, 3);
    Map<String, FormDataBodyPart> files = getRandomFiles(1);
    List<FileStorageObject> addedAttachments = draftServiceDAO.addAttachments(draft, user, files);
    assertThat(addedAttachments, hasSize(1));
    DraftInterface updatedDraft = draftServiceDAO.getAuthorizedDraft(draft.getUUID(), user);
    assertThat(updatedDraft.getStoredFiles(), hasSize(4));
  }

  @Test
  void testDeleteAttachmentFromDraft() throws SQLException {
    User user = createUser();
    DraftInterface draft = createDraft(user, 3);
    Set<FileStorageObject> storedFiles = draft.getStoredFiles();
    assertThat(storedFiles, hasSize(3));
    for (FileStorageObject file : storedFiles) {
      draftServiceDAO.deleteDraftAttachment(draft, user, file.getFileStorageObjectId());
    }
    assertThat(
        draftServiceDAO.getAuthorizedDraft(draft.getUUID(), user).getStoredFiles(), hasSize(0));
  }

  @Test
  void testDeleteAttachmentFromDraft_NotFound() throws SQLException {
    User user = createUser();
    DraftInterface draft = createDraft(user, 1);
    assertThrows(
        NotFoundException.class,
        () -> draftServiceDAO.deleteDraftAttachment(draft, user, Integer.MAX_VALUE));
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
  private DraftInterface createDraft(User user, int numberOfFiles) throws SQLException {
    DraftStudyDataset draft = new DraftStudyDataset("{}", user);
    draftServiceDAO.insertDraft(draft);
    Map<String, FormDataBodyPart> mapOfFiles = getRandomFiles(numberOfFiles);
    draftServiceDAO.addAttachments(draft, user, mapOfFiles);
    return draftServiceDAO.getAuthorizedDraft(draft.getUUID(), user);
  }

  private void assertThinUser(User user) {
    assertThat(user.getRoles(), nullValue());
    assertThat(user.getUserId(), notNullValue());
    assertThat(user.getInstitutionId(), nullValue());
    assertThat(user.getEraCommonsId(), nullValue());
  }

  private Map<String, FormDataBodyPart> getRandomFiles(Integer count) {
    return IntStream.range(0, count)
        .mapToObj("file%d"::formatted)
        .collect(Collectors.toMap(Function.identity(), this::getFormDataBodyPartMock));
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
