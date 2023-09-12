package org.broadinstitute.consent.http.cloudstore;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import jakarta.ws.rs.core.MediaType;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.broadinstitute.consent.http.configurations.StoreConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
class GCSServiceTest {

  @Mock
  private Storage storage;

  @Mock
  private Blob blob;

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
    when(storage.create(any(BlobInfo.class), any(), new Storage.BlobTargetOption[0])).thenReturn(
        blob);
    initStore();

    InputStream is = IOUtils.toInputStream("content", Charset.defaultCharset());
    BlobId storedBlobId = service.storeDocument(is, MediaType.TEXT_PLAIN, id);
    assertNotNull(storedBlobId);
  }

  @Test
  void testGetDocument() throws Exception {
    String fileName = RandomStringUtils.randomAlphanumeric(10);
    String fileContent = RandomStringUtils.randomAlphanumeric(10);
    String urlString = "http://localhost/bucket/" + fileName;
    Blob blob = mock(Blob.class);
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
    String fileContent = RandomStringUtils.randomAlphanumeric(10);
    Blob blob = mock(Blob.class);
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
    String fileName1 = RandomStringUtils.randomAlphanumeric(10);
    String fileName2 = RandomStringUtils.randomAlphanumeric(10);
    String fileContent1 = RandomStringUtils.randomAlphanumeric(10);
    String fileContent2 = RandomStringUtils.randomAlphanumeric(10);

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
    String fileName = RandomStringUtils.random(10, true, false);
    Blob blob = mock(Blob.class);
    BlobId blobId = BlobId.of("bucket", fileName);
    when(blob.getBlobId()).thenReturn(blobId);
    when(storage.get(any(BlobId.class))).thenReturn(blob);
    when(storage.delete(any(BlobId.class))).thenReturn(true);
    initStore();
    boolean deleted = service.deleteDocument(RandomStringUtils.random(10));
    assertTrue(deleted);
  }

}
