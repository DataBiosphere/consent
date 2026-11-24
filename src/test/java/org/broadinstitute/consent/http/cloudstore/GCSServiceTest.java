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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.broadinstitute.consent.http.AbstractTestHelper;
import org.broadinstitute.consent.http.configurations.StoreConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
    when(writer.write(any(ByteBuffer.class)))
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
    verify(writer).setChunkSize(2 * 1024 * 1024);
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
    // The first read passes the limit, so nothing is written.
    when(storage.writer(any(BlobInfo.class))).thenReturn(writer);
    initStore();

    UploadTooLargeException e = assertThrows(UploadTooLargeException.class, () -> store("content"));

    assertInstanceOf(BadRequestException.class, e);
    assertEquals("File size is invalid. Max size is: 0.000006 MB", e.getMessage());
    verify(writer, never()).close();
    verify(storage).delete(BlobId.of(config.getBucket(), id.toString()));
  }

  // A storage failure under the limit is the server's, whether the channel wraps a storage
  // exception or not: a 500 with no storage details. The upload is still cleaned up.
  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void testStoreDocumentStorageFailureIsAServerError(boolean wrapsAStorageException)
      throws Exception {
    IOException failure =
        wrapsAStorageException
            ? new IOException(new StorageException(503, "Unavailable"))
            : new IOException("connection reset");
    when(storage.writer(any(BlobInfo.class))).thenReturn(writer);
    when(writer.write(any(ByteBuffer.class))).thenThrow(failure);
    initStore();

    ServerErrorException e = assertThrows(ServerErrorException.class, () -> store("content"));
    assertEquals(500, e.getResponse().getStatus());
    assertEquals("Unable to store the document", e.getMessage());
    verify(writer, never()).close();
    verify(storage).delete(BlobId.of(config.getBucket(), id.toString()));
  }

  // Finalizing can fail after the server stored the document; it's deleted, and a failed delete
  // doesn't hide the error.
  @Test
  void testStoreDocumentDeletesWhenFinalizingFails() throws Exception {
    stubWriter();
    doThrow(new IOException("connection reset")).when(writer).close();
    when(storage.delete(any(BlobId.class))).thenThrow(new StorageException(500, "delete failed"));
    initStore();

    assertThrows(ServerErrorException.class, () -> store("content"));
    verify(storage).delete(BlobId.of(config.getBucket(), id.toString()));
  }

  // A failure reading the client's upload, even after part of it was stored, isn't a storage
  // failure; it's passed on as before.
  @Test
  void testStoreDocumentPassesOnAFailedRead() throws Exception {
    stubWriter();
    IOException failure = new IOException("client went away");
    InputStream broken =
        new InputStream() {
          private boolean sent;

          @Override
          public int read() {
            return -1;
          }

          @Override
          public int read(byte[] buffer, int offset, int length) throws IOException {
            if (sent) {
              throw failure;
            }
            sent = true;
            buffer[offset] = 'x';
            return 1;
          }
        };
    initStore();

    assertSame(
        failure,
        assertThrows(
            IOException.class, () -> service.storeDocument(broken, MediaType.TEXT_PLAIN, id)));
    verify(writer, never()).close();
  }

  // A payload of many copy buffers and chunks, to a writer that takes part of each buffer, arrives
  // whole.
  @Test
  void testStoreDocumentSpanningManyBuffers() throws Exception {
    byte[] payload =
        RandomStringUtils.secure()
            .nextAlphanumeric(5 * 1024 * 1024)
            .getBytes(StandardCharsets.UTF_8);
    when(storage.writer(any(BlobInfo.class))).thenReturn(writer);
    when(writer.write(any(ByteBuffer.class)))
        .thenAnswer(
            invocation -> {
              ByteBuffer bytes = invocation.getArgument(0);
              int n = Math.min(bytes.remaining(), 1000);
              byte[] part = new byte[n];
              bytes.get(part);
              written.write(part);
              return n;
            });
    initStore();

    service.storeDocument(new ByteArrayInputStream(payload), MediaType.TEXT_PLAIN, id);

    assertArrayEquals(payload, written.toByteArray());
    verify(writer).close();
  }

  // The limit is cumulative: a rejection after more than an upload chunk (2 MiB) reached the writer
  // still isn't finalized, and the writer got no more than the limit.
  @Test
  void testStoreDocumentOverTheLimitAfterAChunk() throws Exception {
    config.setMaxUploadBytes(3 * 1024 * 1024);
    stubWriter();
    initStore();
    byte[] payload =
        RandomStringUtils.secure()
            .nextAlphanumeric(4 * 1024 * 1024)
            .getBytes(StandardCharsets.UTF_8);

    ByteArrayInputStream body = new ByteArrayInputStream(payload);
    assertThrows(
        UploadTooLargeException.class, () -> service.storeDocument(body, MediaType.TEXT_PLAIN, id));

    assertTrue(written.size() > 2 * 1024 * 1024);
    assertTrue(written.size() <= 3 * 1024 * 1024);
    verify(writer, never()).close();
    verify(storage).delete(BlobId.of(config.getBucket(), id.toString()));
  }

  // The message gives the limit exactly.
  @Test
  void testUploadTooLargeMessage() {
    assertEquals(
        "File size is invalid. Max size is: 25 MB",
        new UploadTooLargeException(25_000_000).getMessage());
    assertEquals(
        "File size is invalid. Max size is: 25.5 MB",
        new UploadTooLargeException(25_500_000).getMessage());
  }

  @Test
  void testDefaultUploadLimitIs25Megabytes() {
    assertEquals(25_000_000L, new StoreConfiguration().getMaxUploadBytes());
  }

  @Test
  void testGetDocument() throws Exception {
    String fileName = RandomStringUtils.secure().nextAlphanumeric(10);
    String fileContent = RandomStringUtils.secure().nextAlphanumeric(10);
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
    String fileContent = RandomStringUtils.secure().nextAlphanumeric(10);
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
    String fileName1 = RandomStringUtils.secure().nextAlphanumeric(10);
    String fileName2 = RandomStringUtils.secure().nextAlphanumeric(10);
    String fileContent1 = RandomStringUtils.secure().nextAlphanumeric(10);
    String fileContent2 = RandomStringUtils.secure().nextAlphanumeric(10);

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
    CopyWriter copyWriter = mock(CopyWriter.class);
    when(copyWriter.getResult()).thenReturn(copied);
    when(storage.get(any(BlobId.class))).thenReturn(blob);
    when(storage.copy(any(Storage.CopyRequest.class))).thenReturn(copyWriter);
    UUID copyId = UUID.randomUUID();

    assertEquals(copy, service.copyDocument("source", copyId));
    ArgumentCaptor<Storage.CopyRequest> request =
        ArgumentCaptor.forClass(Storage.CopyRequest.class);
    verify(storage).copy(request.capture());
    assertEquals(BlobId.of(config.getBucket(), "source"), request.getValue().getSource());
    assertEquals(config.getBucket(), request.getValue().getTarget().getBucket());
    assertEquals(copyId.toString(), request.getValue().getTarget().getName());
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
