package org.broadinstitute.consent.http.cloudstore;

import com.google.api.client.http.GenericUrl;
import com.google.api.gax.paging.Page;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import org.apache.commons.lang3.RandomStringUtils;
import org.broadinstitute.consent.http.configurations.StoreConfiguration;
import org.broadinstitute.consent.http.service.WhitelistService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class GCSServiceTest {

    @Mock
    private Storage storage;

    @Mock
    private Blob blob;

    @Mock
    private Bucket bucket;

    @Mock
    private Page<Blob> blobs;

    @Mock
    private Iterable<Blob> iterable;

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
    public void testPostWhitelist() {
        String filename = "filename.txt";
        when(blob.getMediaLink()).thenReturn(config.getEndpoint() + filename);
        when(storage.create(any(BlobInfo.class), any())).thenReturn(blob);
        when(storage.create(any(BlobInfo.class), any(), new Storage.BlobTargetOption[0])).thenReturn(blob);
        initStore();

        GenericUrl url = service.postWhitelist("content", filename);
        assertNotNull(url);
    }

    @Test
    public void testGetMostRecentWhitelist() throws Exception {
        int count = 3; // Count value is used in Blob name, create time, and content
        Iterator<Blob> blobIterator = makeBlobIterator(count);
        when(iterable.iterator()).thenReturn(blobIterator);
        when(blobs.iterateAll()).thenReturn(iterable);
        when(bucket.list()).thenReturn(blobs);
        when(storage.get(any(String.class))).thenReturn(bucket);

        initStore();
        String whitelistData = service.getMostRecentWhitelist();
        assertNotNull(whitelistData);
        assertTrue(whitelistData.contains(String.valueOf(count)));
    }

    /**
     * Make an iterator of Blobs so we can mock out what Google will return for data.
     * @return Iterator<Blob>
     */
    @SuppressWarnings("SameParameterValue")
    private Iterator<Blob> makeBlobIterator(int i) {
        List<Blob> blobs = IntStream.range(1, i + 1).
                mapToObj(this::mockBlob).
                collect(Collectors.toList());
        return blobs.iterator();
    }

    private Blob mockBlob(int i) {
        Blob blob = mock(Blob.class);
        when(blob.isDirectory()).thenReturn(false);
        when(blob.getMediaLink()).thenReturn("http://localhost/bucket/blob" + i);
        when(blob.getName()).thenReturn("bucket/blob_" + i + "_" + WhitelistService.WHITELIST_FILE_PREFIX + RandomStringUtils.random(10, false, true));
        when(blob.getCreateTime()).thenReturn(Long.valueOf(i));
        when(blob.getContent()).thenReturn(("blob_" + i).getBytes());
        return blob;
    }

}
