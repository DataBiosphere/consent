package org.broadinstitute.consent.http.cloudstore;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.io.IOUtils;
import org.broadinstitute.consent.http.AbstractTestHelper;
import org.broadinstitute.consent.http.configurations.StoreConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GCSServiceTest extends AbstractTestHelper {

  @Mock private Storage storage;

  @Mock private Blob blob;

  private StoreConfiguration config;

  private GCSService service;

  @BeforeEach
  void setUp() {
    config = new StoreConfiguration();
    config.setBucket("bucket");
    config.setEndpoint("http://localhost/");
    config.setPassword("password");
  }

  private void initStore() {
    service = new GCSService();
    service.setStorage(storage);
    service.setConfig(config);
  }

  @Test
  void testStoreDocument() throws Exception {
    UUID id = UUID.randomUUID();
    BlobId blobId = BlobId.of(config.getEndpoint(), id.toString());
    when(blob.getBlobId()).thenReturn(blobId);
    when(storage.create(any(BlobInfo.class), any(), new Storage.BlobTargetOption[0]))
        .thenReturn(blob);
    initStore();

    InputStream is = IOUtils.toInputStream("content", Charset.defaultCharset());
    BlobId storedBlobId = service.storeDocument(is, MediaType.TEXT_PLAIN, id);
    assertNotNull(storedBlobId);
  }

  @Test
  void testGetDocument() throws Exception {
    String fileName = randomAlphanumeric(10);
    String fileContent = randomAlphanumeric(10);
    String urlString = "http://localhost/bucket/" + fileName;
    when(blob.getContent()).thenReturn((fileContent).getBytes());
    when(storage.get(any(BlobId.class))).thenReturn(blob);

    initStore();
    InputStream is = service.getDocument(urlString);
    String content = IOUtils.toString(is, Charset.defaultCharset());
    assertNotNull(is);
    assertEquals(fileContent, content);
  }

  @Test
  void testGetDocument_ByBlobId() throws Exception {
    String fileContent = randomAlphanumeric(10);
    when(blob.getContent()).thenReturn((fileContent).getBytes());
    when(storage.get(any(BlobId.class))).thenReturn(blob);

    initStore();
    InputStream is = service.getDocument(BlobId.of("asdf", "ghjkl"));
    String content = IOUtils.toString(is, Charset.defaultCharset());
    assertNotNull(is);
    assertEquals(fileContent, content);
  }

  @Test
  void testGetDocuments() throws Exception {
    String fileName1 = randomAlphanumeric(10);
    String fileName2 = randomAlphanumeric(10);
    String fileContent1 = randomAlphanumeric(10);
    String fileContent2 = randomAlphanumeric(10);

    Blob blob1 = mock(Blob.class);
    BlobId blobId1 = BlobId.of("bucket", fileName1);
    when(blob1.getContent()).thenReturn((fileContent1).getBytes());
    when(blob1.getBlobId()).thenReturn(blobId1);

    Blob blob2 = mock(Blob.class);
    BlobId blobId2 = BlobId.of("bucket", fileName2);
    when(blob2.getContent()).thenReturn((fileContent2).getBytes());
    when(blob2.getBlobId()).thenReturn(blobId2);

    when(storage.get(List.of(blobId1, blobId2))).thenReturn(List.of(blob1, blob2));

    initStore();
    Map<BlobId, InputStream> out = service.getDocuments(List.of(blobId1, blobId2));
    assertNotNull(out);
    assertEquals(2, out.size());
    assertArrayEquals(fileContent1.getBytes(), out.get(blobId1).readAllBytes());
    assertArrayEquals(fileContent2.getBytes(), out.get(blobId2).readAllBytes());
  }

  @Test
  void testDeleteDocument() {
    String fileName = randomAlphabetic(10);
    BlobId blobId = BlobId.of("bucket", fileName);
    when(blob.getBlobId()).thenReturn(blobId);
    when(storage.get(any(BlobId.class))).thenReturn(blob);
    when(storage.delete(any(BlobId.class))).thenReturn(true);
    initStore();
    boolean deleted = service.deleteDocument(randomAlphabetic(10));
    assertTrue(deleted);
  }

  @Test
  void testHasBytes_true() {
    BlobId blobId = BlobId.of("bucket", "hasBytes");
    when(storage.get(blobId)).thenReturn(blob);
    when(blob.getSize()).thenReturn(5L);
    initStore();
    assertTrue(service.hasBytes(blobId));
  }

  @Test
  void testHasBytes_false() {
    BlobId blobId = BlobId.of("bucket", "hasNoBytes");
    when(storage.get(blobId)).thenReturn(blob);
    when(blob.getSize()).thenReturn(0L);
    initStore();
    assertFalse(service.hasBytes(blobId));
  }

  @Test
  void testHasBytes_throws() {
    BlobId blobId = BlobId.of("bucket", "deletedfromgcs");
    when(storage.get(blobId)).thenReturn(null);
    initStore();
    assertThrows(NotFoundException.class, () -> service.hasBytes(blobId));
  }

  @Test
  void testLoadCredentials_blankPasswordUsesApplicationDefault() throws Exception {
    GoogleCredentials adc = mock(GoogleCredentials.class);
    config.setPassword("   ");
    GCSService spyService = spy(new GCSService());
    spyService.setConfig(config);
    doReturn(adc).when(spyService).applicationDefaultCredentials();

    assertSame(adc, spyService.loadCredentials());
  }

  @Test
  void testLoadCredentials_nullPasswordUsesApplicationDefault() throws Exception {
    GoogleCredentials adc = mock(GoogleCredentials.class);
    config.setPassword(null);
    GCSService spyService = spy(new GCSService());
    spyService.setConfig(config);
    doReturn(adc).when(spyService).applicationDefaultCredentials();

    assertSame(adc, spyService.loadCredentials());
  }

  @Test
  void testLoadCredentials_keyFileWins(@TempDir Path tempDir) throws Exception {
    Path keyFile = tempDir.resolve("synthetic-sa.json");
    Files.writeString(keyFile, syntheticServiceAccountKeyJson("synthetic-project"));
    config.setPassword(keyFile.toString());
    GCSService spyService = spy(new GCSService());
    spyService.setConfig(config);

    GoogleCredentials credentials = spyService.loadCredentials();

    assertTrue(credentials instanceof ServiceAccountCredentials);
    assertEquals("synthetic-project", ((ServiceAccountCredentials) credentials).getProjectId());
    verify(spyService, never()).applicationDefaultCredentials();
  }

  @Test
  void testLoadCredentials_missingKeyFileDoesNotFallBack(@TempDir Path tempDir) throws Exception {
    config.setPassword(tempDir.resolve("does-not-exist.json").toString());
    GCSService spyService = spy(new GCSService());
    spyService.setConfig(config);

    assertThrows(IOException.class, spyService::loadCredentials);
    verify(spyService, never()).applicationDefaultCredentials();
  }

  /** A structurally valid service account key with a freshly generated, throwaway RSA key. */
  private static String syntheticServiceAccountKeyJson(String projectId) throws Exception {
    KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
    generator.initialize(2048);
    String pkcs8 =
        Base64.getMimeEncoder(64, "\n".getBytes())
            .encodeToString(generator.generateKeyPair().getPrivate().getEncoded());
    String pem = "-----BEGIN PRIVATE KEY-----\n" + pkcs8 + "\n-----END PRIVATE KEY-----\n";
    return """
        {
          "type": "service_account",
          "project_id": "%s",
          "private_key_id": "synthetic",
          "private_key": "%s",
          "client_email": "synthetic@%s.iam.gserviceaccount.com",
          "client_id": "0",
          "token_uri": "https://oauth2.googleapis.com/token"
        }
        """
        .formatted(projectId, pem.replace("\n", "\\n"), projectId);
  }
}
