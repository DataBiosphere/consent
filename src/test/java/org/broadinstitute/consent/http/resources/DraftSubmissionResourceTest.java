package org.broadinstitute.consent.http.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.api.client.http.HttpStatusCodes;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.DraftSubmission;
import org.broadinstitute.consent.http.models.DraftSubmissionInterface;
import org.broadinstitute.consent.http.models.DraftSubmissionSummary;
import org.broadinstitute.consent.http.models.FileStorageObject;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.DraftSubmissionService;
import org.broadinstitute.consent.http.service.UserService;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DraftSubmissionResourceTest {

  @Mock
  private DraftSubmissionService draftSubmissionService;

  @Mock
  private AuthUser authUser;

  @Mock
  private User user;

  @Mock
  private User user2;

  @Mock
  private UserService userService;

  @Mock
  private UriInfo uriInfo;

  @Mock
  private UriBuilder uriBuilder;

  private DraftSubmissionResource resource;

  private void initResource() {
    resource = new DraftSubmissionResource(userService, draftSubmissionService);
  }

  @Test
  public void testGetDraftSubmissionsWhenNoneExistForUser() {
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(draftSubmissionService.findDraftSummeriesForUser(any())).thenReturn(
        Collections.emptySet());
    initResource();
    Response response = resource.getDraftSubmissions(authUser);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    assertEquals("[]", response.getEntity().toString());
  }

  @Test
  public void testGetDraftSubmissionsWhenOneExistForUser() {
    Set<DraftSubmissionSummary> draftSummaries = new HashSet<>();
    draftSummaries.add(
        new DraftSubmissionSummary(UUID.randomUUID(), "test", new Date(), new Date()));
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(draftSubmissionService.findDraftSummeriesForUser(any())).thenReturn(draftSummaries);
    initResource();
    Response response = resource.getDraftSubmissions(authUser);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    assertEquals(draftSummaries, response.getEntity());
  }

  @Test
  public void tesCreateDraftRegistration() {
    String draft = "{}";
    initResource();
    Response response = resource.createDraftRegistration(authUser, draft);
    assertEquals(HttpStatusCodes.STATUS_CODE_CREATED, response.getStatus());
    assertEquals(draft, response.getEntity().toString());
  }

  @Test
  public void tesCreateDraftRegistrationWithoutJSON() throws SQLException {
    doThrow(new BadRequestException("Error submitting draft")).when(draftSubmissionService)
        .insertDraftSubmission(any());
    String draft = "";
    initResource();
    Response response = resource.createDraftRegistration(authUser, draft);
    assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
  }

  @Test
  public void testGetDraftDocumentNotFound() {
    when(draftSubmissionService.getAuthorizedDraft(any(), any())).thenThrow(
        new NotFoundException("Not found exception."));
    initResource();
    Response response = resource.getDraftDocument(authUser, UUID.randomUUID().toString());
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  public void testGetDraftDocumentSuccess() throws IOException {
    DraftSubmissionInterface draft = new DraftSubmission("{}", user);
    when(draftSubmissionService.getAuthorizedDraft(any(), any())).thenReturn(draft);
    StreamingOutput stream = out -> out.write("{}".getBytes());
    when(draftSubmissionService.draftAsJson(any())).thenReturn(stream);
    initResource();
    Response response = resource.getDraftDocument(authUser, UUID.randomUUID().toString());
    StreamingOutput entity = (StreamingOutput) response.getEntity();
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    entity.write(baos);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    assertEquals(draft.getJson(), baos.toString());
  }

  @Test
  public void testGetDraftDocumentNotAuthorized() {
    when(draftSubmissionService.getAuthorizedDraft(any(), any())).thenThrow(
        new NotAuthorizedException("Not authorized."));
    initResource();
    Response response = resource.getDraftDocument(authUser, UUID.randomUUID().toString());
    assertEquals(HttpStatusCodes.STATUS_CODE_UNAUTHORIZED, response.getStatus());
  }

  @Test
  public void testPutDraftDocumentUnauthorized() {
    when(draftSubmissionService.getAuthorizedDraft(any(), any())).thenThrow(
        new NotAuthorizedException("Not authorized."));
    initResource();
    Response response = resource.updateDraft(authUser, UUID.randomUUID().toString(), "{}");
    assertEquals(HttpStatusCodes.STATUS_CODE_UNAUTHORIZED, response.getStatus());
  }

  @Test
  public void testPutDraftDocumentNotFound() {
    when(draftSubmissionService.getAuthorizedDraft(any(), any())).thenThrow(
        new NotFoundException("Not found exception."));
    initResource();
    Response response = resource.updateDraft(authUser, UUID.randomUUID().toString(), "{}");
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  public void testPutDraftDocumentSuccess() throws IOException {
    String updatedJson = "{\"hello\":\"world!\"}";
    DraftSubmissionInterface draft = new DraftSubmission("{}", user);
    when(draftSubmissionService.getAuthorizedDraft(any(), any())).thenReturn(draft);
    StreamingOutput stream = out -> out.write(updatedJson.getBytes());
    when(draftSubmissionService.draftAsJson(any())).thenReturn(stream);
    initResource();
    Response response = resource.updateDraft(authUser, UUID.randomUUID().toString(), updatedJson);
    StreamingOutput entity = (StreamingOutput) response.getEntity();
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    entity.write(baos);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    assertEquals(updatedJson, baos.toString());
  }

  @Test
  public void testDeleteDraftDocumentNotFound() {
    when(draftSubmissionService.getAuthorizedDraft(any(), any())).thenThrow(
        NotFoundException.class);
    initResource();
    Response response = resource.deleteDraft(authUser, UUID.randomUUID().toString());
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  public void testDeleteDraftDocumentNotAuthorized() {
    when(draftSubmissionService.getAuthorizedDraft(any(), any())).thenThrow(
        NotAuthorizedException.class);
    initResource();
    Response response = resource.deleteDraft(authUser, UUID.randomUUID().toString());
    assertEquals(HttpStatusCodes.STATUS_CODE_UNAUTHORIZED, response.getStatus());
  }

  @Test
  public void testDeleteDraftDocumentSuccess() {
    when(draftSubmissionService.getAuthorizedDraft(any(), any())).thenReturn(
        new DraftSubmission("{}", user));
    initResource();
    Response response = resource.deleteDraft(authUser, UUID.randomUUID().toString());
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  public void testGetDraftAttachmentsNotFound() {
    when(draftSubmissionService.getAuthorizedDraft(any(), any())).thenThrow(NotFoundException.class);
    initResource();
    Response response = resource.getAttachments(authUser, UUID.randomUUID().toString());
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  public void testGetDraftAttachmentsNotAuthorized() {
    when(draftSubmissionService.getAuthorizedDraft(any(), any())).thenThrow(NotAuthorizedException.class);
    initResource();
    Response response = resource.getAttachments(authUser, UUID.randomUUID().toString());
    assertEquals(HttpStatusCodes.STATUS_CODE_UNAUTHORIZED, response.getStatus());
  }

  @Test
  public void testGetDraftAttachmentsSuccessNoAttachments() {
    DraftSubmissionInterface draft = mock(DraftSubmission.class);
    when(draftSubmissionService.getAuthorizedDraft(any(), any())).thenReturn(draft);
    when(draft.getStoredFiles()).thenReturn(new HashSet<>());
    initResource();
    Response response = resource.getAttachments(authUser, UUID.randomUUID().toString());
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    assertEquals("[]", response.getEntity().toString());
  }

  @Test
  public void testGetDraftAttachmentsSuccessWithAttachments() {
    DraftSubmissionInterface draft = mock(DraftSubmission.class);
    when(draftSubmissionService.getAuthorizedDraft(any(), any())).thenReturn(draft);
    FileStorageObject fileStorageObject1 = mock(FileStorageObject.class);
    FileStorageObject fileStorageObject2 = mock(FileStorageObject.class);
    when(draft.getStoredFiles()).thenReturn(Set.of(fileStorageObject1, fileStorageObject2));
    initResource();
    Response response = resource.getAttachments(authUser, UUID.randomUUID().toString());
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    assertTrue(response.getEntity().toString().contains("Mock for FileStorageObject"));
  }

  @Test
  public void testUploadAttachmentNotFound() {
    when(draftSubmissionService.getAuthorizedDraft(any(), any())).thenThrow(NotFoundException.class);
    initResource();
    Response response = resource.addAttachments(authUser, UUID.randomUUID().toString(), mock(
        FormDataMultiPart.class));
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  public void testUploadAttachmentNotAuthorized() {
    when(draftSubmissionService.getAuthorizedDraft(any(), any())).thenThrow(NotAuthorizedException.class);
    initResource();
    Response response = resource.addAttachments(authUser, UUID.randomUUID().toString(), mock(
        FormDataMultiPart.class));
    assertEquals(HttpStatusCodes.STATUS_CODE_UNAUTHORIZED, response.getStatus());
  }


  @Test
  public void testUploadAttachmentSuccess() throws SQLException {
    when(draftSubmissionService.getAuthorizedDraft(any(), any())).thenReturn(new DraftSubmission("{}", user));
    DraftSubmissionInterface draftWithAttachment = new DraftSubmission("{}", user);
    draftWithAttachment.addStoredFile(mock(FileStorageObject.class));
    when(draftSubmissionService.addAttachments(any(), any(), any())).thenReturn(draftWithAttachment);
    initResource();
    Response response = resource.addAttachments(authUser, UUID.randomUUID().toString(), mock(
        FormDataMultiPart.class));
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    assertEquals(draftWithAttachment.getStoredFiles(), response.getEntity());
  }

  @Test
  public void testGetAttachmentNotFound() {
    when(draftSubmissionService.getAuthorizedDraft(any(), any())).thenThrow(NotFoundException.class);
    initResource();
    Response response = resource.getAttachment(authUser, UUID.randomUUID().toString(), 1);
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  public void testGetAttachmentNotAuthorized() {
    when(draftSubmissionService.getAuthorizedDraft(any(), any())).thenThrow(NotAuthorizedException.class);
    initResource();
    Response response = resource.getAttachment(authUser, UUID.randomUUID().toString(), 1);
    assertEquals(HttpStatusCodes.STATUS_CODE_UNAUTHORIZED, response.getStatus());
  }

  @Test
  public void testGetAttachmentSuccess() throws SQLException {
    DraftSubmissionInterface draftWithAttachment = new DraftSubmission("{}", user);
    FileStorageObject fileStorageObject1 = mock(FileStorageObject.class);
    when(fileStorageObject1.getFileName()).thenReturn("fileName1.txt");
    when(fileStorageObject1.getFileStorageObjectId()).thenReturn(1);
    draftWithAttachment.addStoredFile(fileStorageObject1);
    when(draftSubmissionService.getAuthorizedDraft(any(), any())).thenReturn(draftWithAttachment);
    when(draftSubmissionService.getDraftAttachmentStream(fileStorageObject1)).thenReturn(mock(
        InputStream.class));
    initResource();
    Response response = resource.getAttachment(authUser, UUID.randomUUID().toString(), 1);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    assertTrue(response.getHeaderString("Content-Disposition").contains("attachment; filename=\"fileName1.txt\""));
  }

  @Test
  public void testGetAttachmentMissingFile() throws SQLException {
    DraftSubmissionInterface draftWithAttachment = new DraftSubmission("{}", user);
    FileStorageObject fileStorageObject1 = mock(FileStorageObject.class);
    when(fileStorageObject1.getFileStorageObjectId()).thenReturn(1);
    draftWithAttachment.addStoredFile(fileStorageObject1);
    FileStorageObject fileStorageObject3 = mock(FileStorageObject.class);
    when(fileStorageObject1.getFileStorageObjectId()).thenReturn(3);
    draftWithAttachment.addStoredFile(fileStorageObject3);
    when(draftSubmissionService.getAuthorizedDraft(any(), any())).thenReturn(draftWithAttachment);
    initResource();
    Response response = resource.getAttachment(authUser, UUID.randomUUID().toString(), 2);
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  public void testDeleteFileAttachmentNotFound() {
    when(draftSubmissionService.getAuthorizedDraft(any(), any())).thenThrow(NotFoundException.class);
    initResource();
    Response response = resource.deleteDraftAttachment(authUser, UUID.randomUUID().toString(), 1);
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  public void testDeleteFileAttachmentNotAuthorized() {
    when(draftSubmissionService.getAuthorizedDraft(any(), any())).thenThrow(NotAuthorizedException.class);
    initResource();
    Response response = resource.deleteDraftAttachment(authUser, UUID.randomUUID().toString(), 1);
    assertEquals(HttpStatusCodes.STATUS_CODE_UNAUTHORIZED, response.getStatus());
  }

  @Test
  public void testDeleteFileAttachmentSuccess() throws SQLException {
    when(draftSubmissionService.getAuthorizedDraft(any(), any())).thenReturn(new DraftSubmission("{}", user));
    doNothing().when(draftSubmissionService).deleteDraftAttachment(any(), any(), any());
    initResource();
    Response response = resource.deleteDraftAttachment(authUser, UUID.randomUUID().toString(), 1);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }
}
