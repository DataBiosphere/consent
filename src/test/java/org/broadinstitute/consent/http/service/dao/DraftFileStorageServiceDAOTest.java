package org.broadinstitute.consent.http.service.dao;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
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
import java.util.concurrent.Executors;
import java.util.stream.IntStream;
import org.broadinstitute.consent.http.cloudstore.GCSService;
import org.broadinstitute.consent.http.db.DAOTestHelper;
import org.broadinstitute.consent.http.enumeration.FileCategory;
import org.broadinstitute.consent.http.models.FileStorageObject;
import org.broadinstitute.consent.http.models.User;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
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
    draftFileStorageServiceDAO =
        new DraftFileStorageServiceDAO(
            jdbi, gcsService, Executors.newVirtualThreadPerTaskExecutor());
  }

  @Test
  void testCreateDraftFile() {
    User user = createUser();
    UUID associatedUUID = UUID.randomUUID();
    Map<String, FormDataBodyPart> testFiles = getRandomFiles(4);
    List<FileStorageObject> storedFiles = store(associatedUUID, user, testFiles);
    assertThat(storedFiles, hasSize(4));
    storedFiles.forEach(
        fileStorageObject -> {
          assertThat(testFiles, hasKey(fileStorageObject.getFileName()));
          assertEquals(fileStorageObject.getCreateUserId(), user.getUserId());
          assertEquals(fileStorageObject.getCategory(), FileCategory.DRAFT_UPLOADED_FILE);
        });
  }

  @Test
  void testDeleteDraftFiles() {
    User user = createUser();
    UUID associatedUUID = UUID.randomUUID();
    List<FileStorageObject> storedFiles = store(associatedUUID, user, getRandomFiles(2));
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
  void testAFailedUploadDeletesTheDocumentsAlreadyStored() {
    User user = createUser();
    UUID associatedUUID = UUID.randomUUID();
    Map<String, FormDataBodyPart> testFiles = new LinkedHashMap<>(getRandomFiles(1));
    FormDataBodyPart failing = mock(FormDataBodyPart.class);
    when(failing.getMediaType()).thenReturn(MediaType.MULTIPART_FORM_DATA_TYPE);
    when(failing.getValueAs(InputStream.class)).thenThrow(new IllegalStateException("unreadable"));
    testFiles.put("failing", failing);

    ServerErrorException error =
        assertThrows(
            ServerErrorException.class,
            () -> draftFileStorageServiceDAO.uploadDocuments(associatedUUID, user, testFiles));
    assertEquals(500, error.getResponse().getStatus());

    verify(gcsService).deleteDocument(anyString());
  }

  private List<FileStorageObject> store(
      UUID draftId, User user, Map<String, FormDataBodyPart> files) {
    return draftFileStorageServiceDAO.insertFileRows(
        draftFileStorageServiceDAO.uploadDocuments(draftId, user, files), draftId, user);
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
