package org.broadinstitute.consent.http.cloudstore;

import com.google.api.client.http.GenericUrl;
import com.google.api.gax.paging.Page;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import org.broadinstitute.consent.http.configurations.StoreConfiguration;
import org.broadinstitute.consent.http.service.WhitelistService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

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
        List<Blob> matchingBlobs = new ArrayList<>();
        for (Blob blob : blobs.iterateAll()) {
            if (!blob.isDirectory() && blob.getName().contains(WhitelistService.WHITELIST_FILE_PREFIX)) {
                matchingBlobs.add(blob);
            }
        }
        Comparator<Blob> comparator = Comparator.comparing(BlobInfo::getCreateTime);
        Comparator<Blob> reversed = comparator.reversed();
        matchingBlobs.sort(reversed);
        return matchingBlobs;
    }

}
