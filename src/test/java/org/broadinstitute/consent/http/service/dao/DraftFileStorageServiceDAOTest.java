package org.broadinstitute.consent.http.service.dao;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.storage.BlobId;
import jakarta.ws.rs.ServerErrorException;
import jakarta.ws.rs.core.MediaType;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;
import org.broadinstitute.consent.http.cloudstore.GCSService;
import org.broadinstitute.consent.http.db.DAOTestHelper;
import org.broadinstitute.consent.http.enumeration.FileCategory;
import org.broadinstitute.consent.http.models.FileStorageObject;
import org.broadinstitute.consent.http.models.User;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.transaction.TransactionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DraftFileStorageServiceDAOTest extends DAOTestHelper {
  @Mock private GCSService gcsService;

  private DraftFileStorageServiceDAO draftFileStorageServiceDAO;

  @BeforeEach
  void setUp() throws IOException {
    when(gcsService.storeDocument(any(), anyString(), any()))
        .thenReturn(BlobId.of(UUID.randomUUID().toString(), UUID.randomUUID().toString()));
    draftFileStorageServiceDAO = new DraftFileStorageServiceDAO(jdbi, gcsService);
  }

  @Test
  void testCreateDraftFile() {
    User user = createUser();
    UUID associatedUUID = UUID.randomUUID();
    Map<String, FormDataBodyPart> testFiles = getRandomFiles(4);
    List<FileStorageObject> storedFiles =
        draftFileStorageServiceDAO.storeDraftFiles(associatedUUID, user, testFiles);
    assertThat(testFiles.values(), hasSize(4));
    assertThat(storedFiles, hasSize(4));
    storedFiles.forEach(
        fileStorageObject -> {
          assertThat(testFiles, hasKey(fileStorageObject.getFileName()));
          assertFalse(fileStorageObject.getFileName().trim().isEmpty());
          assertEquals(fileStorageObject.getCreateUserId(), user.getUserId());
          assertEquals(fileStorageObject.getCategory(), FileCategory.DRAFT_UPLOADED_FILE);
        });
  }

  @Test
  void testDeleteDraftFiles() {
    User user = createUser();
    UUID associatedUUID = UUID.randomUUID();
    List<FileStorageObject> storedFiles =
        draftFileStorageServiceDAO.storeDraftFiles(associatedUUID, user, getRandomFiles(2));
    assertThat(storedFiles, hasSize(2));

    draftFileStorageServiceDAO.deleteFileRows(storedFiles, user);
    draftFileStorageServiceDAO.deleteDocuments(storedFiles, associatedUUID);

    storedFiles.forEach(
        file ->
            assertTrue(
                fileStorageObjectDAO.findFileById(file.getFileStorageObjectId()).getDeleted()));
    verify(gcsService, times(2)).deleteDocument(anyString());
  }

  @Test
  void testAFailureLeavingTheTransactionDeletesTheUploadedDocuments() {
    Jdbi failingCommit = spy(jdbi);
    doAnswer(
            invocation -> {
              invocation.callRealMethod();
              throw new TransactionException("commit failed");
            })
        .when(failingCommit)
        .useTransaction(any());
    DraftFileStorageServiceDAO dao = new DraftFileStorageServiceDAO(failingCommit, gcsService);
    User user = createUser();
    Map<String, FormDataBodyPart> files = getRandomFiles(2);

    assertThrows(
        TransactionException.class, () -> dao.storeDraftFiles(UUID.randomUUID(), user, files));

    verify(gcsService, times(2)).deleteDocument(anyString());
  }

  @Test
  void testAFailedFileStoresNothing() {
    User user = createUser();
    UUID associatedUUID = UUID.randomUUID();
    Map<String, FormDataBodyPart> testFiles = new LinkedHashMap<>(getRandomFiles(1));
    FormDataBodyPart failing = mock(FormDataBodyPart.class);
    when(failing.getValueAs(InputStream.class)).thenThrow(new IllegalStateException("unreadable"));
    testFiles.put("failing", failing);

    ServerErrorException error =
        assertThrows(
            ServerErrorException.class,
            () -> draftFileStorageServiceDAO.storeDraftFiles(associatedUUID, user, testFiles));
    assertEquals(500, error.getResponse().getStatus());

    assertTrue(fileStorageObjectDAO.findFilesByEntityId(associatedUUID.toString()).isEmpty());
    verify(gcsService).deleteDocument(anyString());
  }

  @Test
  void testAFailedRowInsertDeletesTheUploadedDocument() {
    User user = createUser();
    UUID associatedUUID = UUID.randomUUID();
    FormDataBodyPart failing = getFormDataBodyPartMock("failing");
    when(failing.getMediaType())
        .thenReturn(MediaType.MULTIPART_FORM_DATA_TYPE)
        .thenThrow(new IllegalStateException("no media type"));

    Map<String, FormDataBodyPart> files = Map.of("failing", failing);

    assertThrows(
        ServerErrorException.class,
        () -> draftFileStorageServiceDAO.storeDraftFiles(associatedUUID, user, files));

    assertTrue(fileStorageObjectDAO.findFilesByEntityId(associatedUUID.toString()).isEmpty());
    verify(gcsService).deleteDocument(anyString());
  }

  private Map<String, FormDataBodyPart> getRandomFiles(Integer count) {
    Map<String, FormDataBodyPart> mapOfFiles = new HashMap<>();
    IntStream.range(0, count)
        .forEach(
            index -> {
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
