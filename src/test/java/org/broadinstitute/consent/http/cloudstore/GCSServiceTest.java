package org.broadinstitute.consent.http.cloudstore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Date;
import javax.ws.rs.core.MediaType;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.broadinstitute.consent.http.configurations.StoreConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;


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
        String filename = "filename.txt";
        BlobId blobId = BlobId.of(config.getEndpoint(), filename);
        when(blob.getBlobId()).thenReturn(blobId);
        when(storage.create(any(BlobInfo.class), any())).thenReturn(blob);
        when(storage.create(any(BlobInfo.class), any(), new Storage.BlobTargetOption[0])).thenReturn(blob);
        initStore();

        InputStream is = IOUtils.toInputStream("content", Charset.defaultCharset());
        BlobId storedBlobId = service.storeDocument(is, MediaType.TEXT_PLAIN, filename);
        assertNotNull(storedBlobId);
    }

    @Test
    public void testGetDocument() throws Exception {
        String fileName = RandomStringUtils.random(10, true, false);
        String fileContent = "content";
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
