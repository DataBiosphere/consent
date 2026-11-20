package org.broadinstitute.consent.http.cloudstore;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.WriteChannel;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.CopyWriter;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ServerErrorException;
import jakarta.ws.rs.core.MediaType;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
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
import org.mockito.ArgumentCaptor;
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

  @Mock private WriteChannel writer;

  /** Bytes the writer received. */
  private final ByteArrayOutputStream written = new ByteArrayOutputStream();

  private void stubWriter() throws IOException {
    when(storage.writer(any(BlobInfo.class))).thenReturn(writer);
    lenient()
        .when(writer.write(any(ByteBuffer.class)))
        .thenAnswer(
            invocation -> {
              ByteBuffer bytes = invocation.getArgument(0);
              int n = bytes.remaining();
              byte[] chunk = new byte[n];
              bytes.get(chunk);
              written.write(chunk);
              return n;
            });
  }

  private BlobId store(String content) throws IOException {
    return service.storeDocument(
        IOUtils.toInputStream(content, Charset.defaultCharset()), MediaType.TEXT_PLAIN, id);
  }

  private final UUID id = UUID.randomUUID();

  // The upload streams in small chunks, and only a complete one is finalized (closed).
  @Test
  void testStoreDocument() throws Exception {
    stubWriter();
    initStore();

    BlobId stored = store("content");

    BlobId expected = BlobId.of(config.getBucket(), id.toString());
    assertEquals(expected, stored);
    ArgumentCaptor<BlobInfo> info = ArgumentCaptor.forClass(BlobInfo.class);
    verify(storage).writer(info.capture());
    assertEquals(expected, info.getValue().getBlobId());
    assertEquals(MediaType.TEXT_PLAIN, info.getValue().getContentType());
    verify(writer).setChunkSize(256 * 1024);
    assertEquals("content", written.toString(Charset.defaultCharset()));
    verify(writer).close();
    verify(storage, never()).delete(any(BlobId.class));
  }

  @Test
  void testStoreDocumentAtTheLimit() throws Exception {
    config.setMaxUploadBytes(7);
    stubWriter();
    initStore();

    store("content");

    verify(writer).close();
  }

  // Past the limit the upload is a 400 and is never finalized; anything stored is deleted.
  @Test
  void testStoreDocumentOverTheLimit() throws Exception {
    config.setMaxUploadBytes(6);
    stubWriter();
    initStore();

    UploadTooLargeException e = assertThrows(UploadTooLargeException.class, () -> store("content"));

    assertInstanceOf(BadRequestException.class, e);
    assertEquals("File size is invalid. Max size is: 0 MB", e.getMessage());
    verify(writer, never()).close();
    verify(storage).delete(BlobId.of(config.getBucket(), id.toString()));
  }

  // A storage failure under the limit isn't a size problem: the channel wraps it in an
  // IOException, and it's passed on as the storage failure. The upload is still cleaned up.
  @Test
  void testStoreDocumentRethrowsOtherStorageErrors() throws Exception {
    StorageException failure = new StorageException(503, "Unavailable");
    when(storage.writer(any(BlobInfo.class))).thenReturn(writer);
    when(writer.write(any(ByteBuffer.class))).thenThrow(new IOException(failure));
    initStore();

    assertSame(failure, assertThrows(StorageException.class, () -> store("content")));
    verify(writer, never()).close();
    verify(storage).delete(BlobId.of(config.getBucket(), id.toString()));
  }

  // Finalizing can fail after the server stored the document; it's deleted, and a failed delete
  // doesn't hide the original error.
  @Test
  void testStoreDocumentDeletesWhenFinalizingFails() throws Exception {
    stubWriter();
    IOException failure = new IOException("connection reset");
    doThrow(failure).when(writer).close();
    when(storage.delete(any(BlobId.class))).thenThrow(new StorageException(500, "delete failed"));
    initStore();

    assertSame(failure, assertThrows(IOException.class, () -> store("content")));
    verify(storage).delete(BlobId.of(config.getBucket(), id.toString()));
  }

  @Test
  void testDefaultUploadLimitIs25Megabytes() {
    assertEquals(25_000_000L, new StoreConfiguration().getMaxUploadBytes());
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
  void testCopyDocument() {
    initStore();
    BlobId copy = BlobId.of("bucket", "copy");
    Blob copied = mock(Blob.class);
    when(copied.getBlobId()).thenReturn(copy);
    CopyWriter writer = mock(CopyWriter.class);
    when(writer.getResult()).thenReturn(copied);
    when(storage.get(any(BlobId.class))).thenReturn(blob);
    when(storage.copy(any(Storage.CopyRequest.class))).thenReturn(writer);
    UUID id = UUID.randomUUID();

    assertEquals(copy, service.copyDocument("source", id));
    ArgumentCaptor<Storage.CopyRequest> request =
        ArgumentCaptor.forClass(Storage.CopyRequest.class);
    verify(storage).copy(request.capture());
    assertEquals(BlobId.of(config.getBucket(), "source"), request.getValue().getSource());
    assertEquals(config.getBucket(), request.getValue().getTarget().getBucket());
    assertEquals(id.toString(), request.getValue().getTarget().getName());
  }

  @Test
  void testCopyDocumentOfAMissingDocument() {
    initStore();
    when(storage.get(any(BlobId.class))).thenReturn(null);

    assertThrows(NotFoundException.class, () -> service.copyDocument("missing", UUID.randomUUID()));
    verify(storage, never()).copy(any(Storage.CopyRequest.class));
  }

  // Deleted between the existence check and the copy.
  @Test
  void testCopyDocumentDeletedDuringTheCopy() {
    initStore();
    when(storage.get(any(BlobId.class))).thenReturn(blob);
    when(storage.copy(any(Storage.CopyRequest.class)))
        .thenThrow(new StorageException(404, "Not Found"));

    assertThrows(NotFoundException.class, () -> service.copyDocument("gone", UUID.randomUUID()));
  }

  @Test
  void testCopyDocumentHidesOtherStorageErrors() {
    initStore();
    when(storage.get(any(BlobId.class))).thenReturn(blob);
    when(storage.copy(any(Storage.CopyRequest.class)))
        .thenThrow(new StorageException(503, "Unavailable: internal detail"));

    ServerErrorException e =
        assertThrows(
            ServerErrorException.class, () -> service.copyDocument("source", UUID.randomUUID()));
    assertEquals(500, e.getResponse().getStatus());
    assertEquals("Unable to copy the document", e.getMessage());
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
