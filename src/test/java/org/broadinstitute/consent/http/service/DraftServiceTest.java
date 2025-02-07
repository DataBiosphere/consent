package org.broadinstitute.consent.http.service;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.google.cloud.storage.BlobId;
import com.google.gson.Gson;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.StreamingOutput;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.broadinstitute.consent.http.cloudstore.GCSService;
import org.broadinstitute.consent.http.db.DraftDAO;
import org.broadinstitute.consent.http.enumeration.DraftType;
import org.broadinstitute.consent.http.models.DraftInterface;
import org.broadinstitute.consent.http.models.DraftStudyDataset;
import org.broadinstitute.consent.http.models.DraftSummary;
import org.broadinstitute.consent.http.models.FileStorageObject;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.dao.DraftServiceDAO;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DraftServiceTest {

  @Mock
  private DraftDAO draftDAO;

  @Mock
  private DraftServiceDAO draftServiceDAO;

  @Mock
  private GCSService gcsService;

  private DraftService draftService;

  private User user;

  private DraftInterface draft;

  @BeforeEach
  void beforeEach() {
    draftService = new DraftService(draftDAO, draftServiceDAO, gcsService);
    user = new User();
    user.setEmail("email@email.com");
    user.setUserId(1);
    draft = new DraftStudyDataset("{}", user);
  }

  @Test
  void testCreateDraft() throws SQLException {
    doThrow(new BadRequestException("Bad Request")).when(draftServiceDAO).insertDraft(any());
    assertThrows(BadRequestException.class, () -> draftService.insertDraft(null));
  }

  @Test
  void testGetAuthorizedDraftNotFound() {
    doThrow(new NotFoundException("Not Found")).when(draftServiceDAO)
        .getAuthorizedDraft(any(), any());
    assertThrows(NotFoundException.class, () -> draftService.getAuthorizedDraft(null, user));
  }

  @Test
  void testGetAuthorizedDraftsNotAuthorized() {
    UUID draftId = UUID.randomUUID();
    doThrow(new NotAuthorizedException("Not Authorized")).when(draftServiceDAO)
        .getAuthorizedDraft(draftId, user);
    assertThrows(NotAuthorizedException.class,
        () -> draftService.getAuthorizedDraft(draftId, user));
  }

  @Test
  void testGetAuthorizedDraft() {
    when(draftServiceDAO.getAuthorizedDraft(any(), any())).thenReturn(draft);
    assertEquals(draft, draftService.getAuthorizedDraft(draft.getUUID(), user));
  }

  @Test
  void testaddAttachments() throws SQLException, RuntimeException {
    Map<String, FormDataBodyPart> files = new HashMap<>();
    files.put("test", new FormDataBodyPart("test", "test"));
    FileStorageObject fileStorageObject = new FileStorageObject();
    fileStorageObject.setFileName("test");
    when(draftServiceDAO.addAttachments(draft, user, files)).thenReturn(List.of(fileStorageObject));
    assertEquals(fileStorageObject,
        draftService.addAttachments(draft, user, files).iterator().next());
  }

  @Test
  void testDeleteAttachment() throws SQLException {
    doNothing().when(draftServiceDAO).deleteDraftAttachment(any(), any(), anyInt());
    assertDoesNotThrow(() -> draftService.deleteDraftAttachment(null, null, 1));
  }

  @Test
  void testDeleteAttachmentThrows() throws SQLException {
    doThrow(new SQLException("Couldn't delete document")).when(draftServiceDAO)
        .deleteDraftAttachment(any(), any(), anyInt());
    assertThrows(SQLException.class, () -> draftService.deleteDraftAttachment(null, null, 1));
  }

  @Test
  void testFindDraftSummariesForUser() {
    DraftSummary draftSummary = new DraftSummary(UUID.randomUUID(), "test", new Date(), new Date(),
        DraftType.STUDY_DATASET_SUBMISSION_V1);
    when(draftDAO.findDraftSummariesByUserId(user.getUserId())).thenReturn(List.of(draftSummary));
    draftService.findDraftSummariesForUser(user);
    assertEquals(draftSummary, draftService.findDraftSummariesForUser(user).iterator().next());
  }

  @Test
  void testUpdateDraft() throws SQLException {
    DraftInterface updatedDraft = new DraftStudyDataset("{}", user);
    updatedDraft.setUpdateDate(new Date());
    when(draftServiceDAO.updateDraft(draft, user)).thenReturn(updatedDraft);
    DraftInterface draftUpdate = draftService.updateDraft(draft, user);
    assertNotEquals(draftUpdate, draft);
  }

  @Test
  void testDeleteDraft() throws SQLException {
    doNothing().when(draftServiceDAO).deleteDraft(draft, user);
    draftService.deleteDraft(draft, user);
  }

  @Test
  void testDeleteDraftThrows() throws SQLException {
    doThrow(new SQLException("delete failed.")).when(draftServiceDAO).deleteDraft(draft, user);
    assertThrows(SQLException.class, () -> draftService.deleteDraft(draft, user));
  }

  @Test
  void testStreamingOutput() throws Exception {
    StreamingOutput output = draftService.draftAsJson(draft);
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    output.write(byteArrayOutputStream);
    byteArrayOutputStream.close();
    Gson gson = GsonUtil.buildGson();
    StreamingDeserializer streamedData = gson.fromJson(byteArrayOutputStream.toString(),
        StreamingDeserializer.class);
    assertEquals(draft.getCreateDate().getTime(),
        streamedData.meta.getCreateDate().getTime());
    assertEquals("{}", streamedData.document.toString());
  }

  @Test
  void testGetDraftFileInputStream() throws IOException {
    FileStorageObject fileStorageObject = new FileStorageObject();
    fileStorageObject.setBlobId(BlobId.of(UUID.randomUUID().toString(), "test"));
    when(gcsService.getDocument(fileStorageObject.getBlobId())).thenAnswer(
        inputStream -> new ByteArrayInputStream("{}".getBytes(StandardCharsets.UTF_8)) {
        });
    InputStream fileContents = draftService.getDraftAttachmentStream(fileStorageObject);
    assertEquals("{}", new String(fileContents.readAllBytes(), StandardCharsets.UTF_8));
  }

  private static class StreamingDeserializer {

    private final Object document;
    private final DraftStudyDataset meta;

    public StreamingDeserializer(String document, DraftStudyDataset meta) {
      this.document = document;
      this.meta = meta;
    }
  }
}