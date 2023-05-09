package org.broadinstitute.consent.http.cloudstore;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.broadinstitute.consent.http.configurations.StoreConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.ws.rs.core.MediaType;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class GCSServiceTest {

    @Mock
    private Storage storage;

    @Mock
    private Blob blob;

    private StoreConfiguration config;

    private GCSService service;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
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
    public void testStoreDocument() throws Exception {
        UUID id = UUID.randomUUID();
        BlobId blobId = BlobId.of(config.getEndpoint(), id.toString());
        when(blob.getBlobId()).thenReturn(blobId);
        when(storage.create(any(BlobInfo.class), any())).thenReturn(blob);
        when(storage.create(any(BlobInfo.class), any(), new Storage.BlobTargetOption[0])).thenReturn(blob);
        initStore();

        InputStream is = IOUtils.toInputStream("content", Charset.defaultCharset());
        BlobId storedBlobId = service.storeDocument(is, MediaType.TEXT_PLAIN, id);
        assertNotNull(storedBlobId);
    }

    @Test
    public void testGetDocument() throws Exception {
        String fileName = RandomStringUtils.randomAlphanumeric(10);
        String fileContent = RandomStringUtils.randomAlphanumeric(10);
        String urlString = "http://localhost/bucket/" + fileName;
        Date now = new Date();
        Blob blob = mock(Blob.class);
        BlobId blobId = BlobId.of("bucket", fileName);
        when(blob.isDirectory()).thenReturn(false);
        when(blob.getMediaLink()).thenReturn(urlString);
        when(blob.getName()).thenReturn("bucket/" + fileName);
        when(blob.getCreateTime()).thenReturn(now.getTime());
        when(blob.getContent()).thenReturn((fileContent).getBytes());
        when(blob.getBlobId()).thenReturn(blobId);
        when(storage.get(any(BlobId.class))).thenReturn(blob);

        initStore();
        InputStream is = service.getDocument(urlString);
        String content = IOUtils.toString(is, Charset.defaultCharset());
        assertNotNull(is);
        assertEquals(fileContent, content);
    }

    @Test
    public void testGetDocument_ByBlobId() throws Exception {
        String fileName = RandomStringUtils.randomAlphanumeric(10);
        String fileContent = RandomStringUtils.randomAlphanumeric(10);
        String urlString = "http://localhost/bucket/" + fileName;
        Date now = new Date();
        Blob blob = mock(Blob.class);
        BlobId blobId = BlobId.of("asdf", "ghjkl");
        when(blob.isDirectory()).thenReturn(false);
        when(blob.getMediaLink()).thenReturn(urlString);
        when(blob.getName()).thenReturn("bucket/" + fileName);
        when(blob.getCreateTime()).thenReturn(now.getTime());
        when(blob.getContent()).thenReturn((fileContent).getBytes());
        when(blob.getBlobId()).thenReturn(blobId);
        when(storage.get(any(BlobId.class))).thenReturn(blob);

        initStore();
        InputStream is = service.getDocument(BlobId.of("asdf", "ghjkl"));
        String content = IOUtils.toString(is, Charset.defaultCharset());
        assertNotNull(is);
        assertEquals(fileContent, content);
    }

    @Test
    public void testGetDocuments() throws Exception {
        String fileName1 = RandomStringUtils.randomAlphanumeric(10);
        String fileName2 = RandomStringUtils.randomAlphanumeric(10);
        String fileContent1 = RandomStringUtils.randomAlphanumeric(10);
        String fileContent2 = RandomStringUtils.randomAlphanumeric(10);

        Date now = new Date();
        Blob blob1 = mock(Blob.class);
        BlobId blobId1 = BlobId.of("bucket", fileName1);
        when(blob1.isDirectory()).thenReturn(false);
        when(blob1.getMediaLink()).thenReturn("http://localhost/bucket/" + fileName1);
        when(blob1.getName()).thenReturn("bucket/" + fileName1);
        when(blob1.getCreateTime()).thenReturn(now.getTime());
        when(blob1.getContent()).thenReturn((fileContent1).getBytes());
        when(blob1.getBlobId()).thenReturn(blobId1);

        Blob blob2 = mock(Blob.class);
        BlobId blobId2 = BlobId.of("bucket", fileName2);
        when(blob2.isDirectory()).thenReturn(false);
        when(blob2.getMediaLink()).thenReturn("http://localhost/bucket/" + fileName2);
        when(blob2.getName()).thenReturn("bucket/" + fileName2);
        when(blob2.getCreateTime()).thenReturn(now.getTime());
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
    public void testDeleteDocument() {
        String fileName = RandomStringUtils.random(10, true, false);
        String fileContent = "content";
        Blob blob = mock(Blob.class);
        BlobId blobId = BlobId.of("bucket", fileName);
        when(blob.getContent()).thenReturn((fileContent).getBytes());
        when(blob.getBlobId()).thenReturn(blobId);
        when(storage.get(any(BlobId.class))).thenReturn(blob);
        when(storage.delete(any(BlobId.class))).thenReturn(true);
        initStore();
        boolean deleted = service.deleteDocument(RandomStringUtils.random(10));
        assertTrue(deleted);
    }

}
