package org.broadinstitute.consent.http.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.storage.BlobId;
import jakarta.ws.rs.NotFoundException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.broadinstitute.consent.http.cloudstore.GCSService;
import org.broadinstitute.consent.http.db.DAOTestHelper;
import org.broadinstitute.consent.http.enumeration.FileCategory;
import org.broadinstitute.consent.http.models.FileStorageObject;
import org.broadinstitute.consent.http.models.User;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DraftFileStorageServiceTest extends DAOTestHelper {

  private DraftFileStorageService draftFileStorageService;

  @BeforeEach
  public void setUp() throws IOException {
    GCSService gcsService = mock(GCSService.class);
    when(gcsService.storeDocument(any(), anyString(), any())).thenReturn(
        BlobId.of(UUID.randomUUID().toString(), UUID.randomUUID().toString()));
    when(gcsService.getDocument((BlobId) any())).thenAnswer(inputStream -> new ByteArrayInputStream("{}".getBytes(StandardCharsets.UTF_8)) {
    });
    draftFileStorageService = new DraftFileStorageService(jdbi, gcsService, fileStorageObjectDAO);
  }

  @Test
  public void testCreateDraftFile() throws SQLException {
    User user = createUser();
    UUID associatedUUID = UUID.randomUUID();
    Map<String, FormDataBodyPart> testFiles = getRandomFiles(4);
    List<FileStorageObject> storedFiles = draftFileStorageService.storeDraftFiles(associatedUUID,
        user, testFiles);
    assertThat(testFiles.values(), hasSize(4));
    assertThat(storedFiles, hasSize(4));
    storedFiles.forEach(fileStorageObject -> {
      assertThat(testFiles, hasKey(fileStorageObject.getFileName()));
      assertFalse(fileStorageObject.getFileName().trim().isEmpty());
      assertEquals(fileStorageObject.getCreateUserId(), user.getUserId());
      assertEquals(fileStorageObject.getCategory(), FileCategory.DRAFT_UPLOADED_FILE);
    });
  }

  @Test
  public void testDeleteDraftFiles() throws SQLException {
    User user = createUser();
    UUID associatedUUID = UUID.randomUUID();
    Map<String, FormDataBodyPart> testFiles = getRandomFiles(2);
    List<FileStorageObject> storedFiles = draftFileStorageService.storeDraftFiles(associatedUUID,
        user, testFiles);
    assertThat(testFiles.values(), hasSize(2));
    assertThat(storedFiles, hasSize(2));
    for (FileStorageObject fileStorageObject : storedFiles) {
      draftFileStorageService.deleteStoredFile(fileStorageObject, user);
    }
    assertThrows(NotFoundException.class,
        () -> draftFileStorageService.deleteStoredFile(new FileStorageObject(), user));
  }

  @Test
  public void testGetDraftFile() throws IOException, SQLException {
    User user = createUser();
    UUID associatedUUID = UUID.randomUUID();
    Map<String, FormDataBodyPart> testFiles = getRandomFiles(2);
    List<FileStorageObject> storedFiles = draftFileStorageService.storeDraftFiles(associatedUUID, user, testFiles);
    assertThat(testFiles.values(), hasSize(2));
    assertThat(storedFiles, hasSize(2));
    for (FileStorageObject fileStorageObject : storedFiles) {
      InputStream fileContents =  draftFileStorageService.get(fileStorageObject);
      assertEquals(EMPTY_JSON_DOCUMENT, new String(fileContents.readAllBytes(), StandardCharsets.UTF_8));
    }
  }
}
