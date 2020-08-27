package org.broadinstitute.consent.http.cloudstore;

import com.google.api.client.http.GenericUrl;
import com.google.api.gax.paging.Page;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.ReadChannel;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.charset.Charset;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.ws.rs.NotFoundException;
import org.apache.commons.io.IOUtils;
import org.broadinstitute.consent.http.configurations.StoreConfiguration;
import org.broadinstitute.consent.http.service.WhitelistService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GCSService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private StoreConfiguration config;
    private Storage storage;

    public GCSService() {
    }

    @Inject
    public GCSService(StoreConfiguration config) {
        this.config = config;
        try {
            ServiceAccountCredentials credentials = ServiceAccountCredentials.
                    fromStream(new FileInputStream(config.getPassword()));
            Storage storage = StorageOptions.newBuilder().
                    setProjectId(credentials.getProjectId()).
                    setCredentials(credentials).
                    build().
                    getService();
            this.setStorage(storage);
        } catch (Exception e) {
            logger.error("Exception initializing GCSService: " + e.getMessage());
        }
    }

    @VisibleForTesting
    public void setStorage(Storage storage) {
        this.storage = storage;
    }

    @VisibleForTesting
    public void setConfig(StoreConfiguration config) {
        this.config = config;
    }

    public GenericUrl postWhitelist(String content, String fileName) {
        BlobId blobId = BlobId.of(config.getBucket(), "whitelist/" + fileName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType("text/plain").build();
        Blob blob = storage.create(blobInfo, content.getBytes());
        return new GenericUrl(blob.getMediaLink());
    }

    public String getMostRecentWhitelist() throws Exception {
        try {
            Optional<Blob> whitelist = listWhitelistItems().stream().findFirst();
            if (whitelist.isPresent()) {
                return new String(whitelist.get().getContent());
            } else {
                logger.error("Most recent whitelist does not exist.");
                throw new Exception("Most recent whitelist does not exist.");
            }
        } catch (Exception e) {
            logger.error("Error getting most recent whitelist: " + e.getMessage());
            throw new Exception("Error getting most recent whitelist: " + e.getMessage());
        }
    }

    private List<Blob> listWhitelistItems() {
        Bucket bucket = storage.get(config.getBucket());
        Page<Blob> blobs = bucket.list();
        List<Blob> matchingBlobs = StreamSupport.
                stream(blobs.iterateAll().spliterator(), false).
                filter(b -> !b.isDirectory()).
                filter(b -> b.getName().contains(WhitelistService.WHITELIST_FILE_PREFIX)).
                collect(Collectors.toList());
        Comparator<Blob> comparator = Comparator.comparing(BlobInfo::getCreateTime);
        Comparator<Blob> reversed = comparator.reversed();
        matchingBlobs.sort(reversed);
        return matchingBlobs;
    }

    public GenericUrl storeDocument(InputStream content, String mediaType, String fileName)
        throws IOException {
        byte[] bytes = IOUtils.toByteArray(content);
        BlobId blobId = BlobId.of(config.getBucket(), fileName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType(mediaType).build();
        Blob blob = storage.create(blobInfo, bytes);
        return new GenericUrl(blob.getMediaLink());
    }

    public InputStream getDocument(GenericUrl url) throws NotFoundException {
        String fileName = url.getPathParts().stream().reduce((i, j) -> j).orElse("");
        Bucket bucket = storage.get(config.getBucket());
        Page<Blob> blobs = bucket.list();
        Optional<Blob> blobOptional = StreamSupport.
            stream(blobs.iterateAll().spliterator(), false).
            filter(b -> !b.isDirectory()).
            filter(b -> b.getName().contains(fileName)).
            findFirst();
        if (blobOptional.isPresent()) {
            return new ByteArrayInputStream(blobOptional.get().getContent());
        } else {
            throw new NotFoundException("URL Not Found: " + url.toString());
        }
    }
}
