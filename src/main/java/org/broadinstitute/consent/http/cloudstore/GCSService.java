package org.broadinstitute.consent.http.cloudstore;

import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import org.apache.commons.io.IOUtils;
import org.broadinstitute.consent.http.configurations.StoreConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.NotFoundException;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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

    /**
     * Get the root bucket configured for this environment. Returns a Bucket with all possible
     * metadata values.
     *
     * @return Bucket
     */
    public Bucket getRootBucketWithMetadata() {
        return storage.get(config.getBucket(), Storage.BucketGetOption.fields(Storage.BucketField.values()));
    }

    /**
     * Store an input stream as a Blob
     *
     * @param content   InputStream content
     * @param mediaType String media type
     * @param id        String UUID of the file
     * @return BlobId of the stored document
     * @throws IOException Exception when storing document
     */
    public BlobId storeDocument(InputStream content, String mediaType, UUID id)
            throws IOException {
        byte[] bytes = IOUtils.toByteArray(content);
        BlobId blobId = BlobId.of(config.getBucket(), id.toString());
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType(mediaType).build();
        Blob blob = storage.create(blobInfo, bytes);
        return blob.getBlobId();
    }


    /**
     * Delete a document by Blob Id Name
     *
     * @param blobIdName String value of the document blob id name
     * @return True if document was deleted, false otherwise.
     */
    public boolean deleteDocument(String blobIdName) {
        Optional<Blob> blobOptional = getBlobFromUrl(blobIdName);
        return blobOptional
                .map(blob -> storage.delete(blob.getBlobId()))
                .orElse(false);
    }

    /**
     * Retrieve a document by Blob Id Name
     *
     * @param blobIdName String value of the document blob id name
     * @return InputStream of the document
     * @throws NotFoundException Returned when no document found
     */
    public InputStream getDocument(String blobIdName) throws NotFoundException {
        Optional<Blob> blobOptional = getBlobFromUrl(blobIdName);
        if (blobOptional.isPresent()) {
            return new ByteArrayInputStream(blobOptional.get().getContent());
        } else {
            throw new NotFoundException("Document Not Found: " + blobIdName);
        }
    }

    public InputStream getDocument(BlobId blobId) throws NotFoundException {
        Optional<Blob> blobOptional = getBlobFromBlobId(blobId);
        if (blobOptional.isPresent()) {
            return new ByteArrayInputStream(blobOptional.get().getContent());
        } else {
            throw new NotFoundException("Document Not Found: " + blobId.toString());
        }
    }

    public Map<BlobId, InputStream> getDocuments(List<BlobId> blobIds) throws NotFoundException {
        Optional<List<Blob>> blobOptional = getBlobsFromBlobIds(blobIds);
        if (blobOptional.isPresent()) {
            List<Blob> blobs = blobOptional.get();
            Map<BlobId, InputStream> output = new HashMap<>();
            blobs.forEach((b) -> output.put(b.getBlobId(), new ByteArrayInputStream(b.getContent())));
            return output;
        } else {
            throw new NotFoundException("Document Not Found: " + blobIds.toString());
        }
    }

    /**
     * Find a blob in the current storage bucket.
     *
     * @param blobIdName String value of the document blob id name
     * @return Optional<Blob>
     */
    private Optional<Blob> getBlobFromUrl(String blobIdName) {
        Blob blob = storage.get(BlobId.of(config.getBucket(), blobIdName));
        return Optional.of(blob);
    }

    /**
     * Find a blob in the current storage bucket.
     *
     * @param blobId Bucket and blob id
     * @return Optional<Blob>
     */
    private Optional<Blob> getBlobFromBlobId(BlobId blobId) {
        Blob blob = storage.get(blobId);
        return Optional.of(blob);
    }

    private Optional<List<Blob>> getBlobsFromBlobIds(List<BlobId> blobIds) {
        List<Blob> blobs = storage.get(blobIds);
        return Optional.of(blobs);
    }
}
