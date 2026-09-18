package org.broadinstitute.consent.http.cloudstore;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.configurations.StoreConfiguration;
import org.broadinstitute.consent.http.util.ConsentLogger;

public class GCSService implements ConsentLogger {

  private StoreConfiguration config;
  private Storage storage;

  public GCSService() {}

  @Inject
  public GCSService(StoreConfiguration config) {
    this.config = config;
    try {
      GoogleCredentials credentials = loadCredentials();
      StorageOptions.Builder builder = StorageOptions.newBuilder().setCredentials(credentials);
      if (credentials instanceof ServiceAccountCredentials sa && sa.getProjectId() != null) {
        builder.setProjectId(sa.getProjectId());
      }
      this.setStorage(builder.build().getService());
    } catch (Exception e) {
      logException("Exception initializing GCSService: " + e.getMessage(), e);
    }
  }

  /**
   * Resolve the credentials used to talk to GCS.
   *
   * <p>Deployed environments mount a Yale-managed service account key file and name it in {@code
   * googleStore.password}. When that value is blank, fall back to Application Default Credentials
   * so local development needs no long-lived key: developers run {@code gcloud auth
   * application-default login}, optionally impersonating the environment's service account. See
   * DEVNOTES.md, "Google Cloud Storage credentials".
   *
   * @return GoogleCredentials for the Storage client
   * @throws IOException when the key file cannot be read or no default credentials are available
   */
  @VisibleForTesting
  GoogleCredentials loadCredentials() throws IOException {
    String keyPath = config.getPassword();
    if (StringUtils.isBlank(keyPath)) {
      logInfo("No googleStore.password configured; using Application Default Credentials for GCS");
      return applicationDefaultCredentials();
    }
    try (InputStream keyStream = new FileInputStream(keyPath)) {
      return ServiceAccountCredentials.fromStream(keyStream);
    }
  }

  /** Seam for tests; resolves credentials from the ambient environment. */
  @VisibleForTesting
  GoogleCredentials applicationDefaultCredentials() throws IOException {
    return GoogleCredentials.getApplicationDefault();
  }

  @VisibleForTesting
  protected void setStorage(Storage storage) {
    this.storage = storage;
  }

  @VisibleForTesting
  protected void setConfig(StoreConfiguration config) {
    this.config = config;
  }

  /**
   * Get the root bucket configured for this environment. Returns a Bucket with all possible
   * metadata values.
   *
   * @return Bucket
   */
  public Bucket getRootBucketWithMetadata() {
    return storage.get(
        config.getBucket(), Storage.BucketGetOption.fields(Storage.BucketField.values()));
  }

  /**
   * Store an input stream as a Blob
   *
   * @param content InputStream content
   * @param mediaType String media type
   * @param id String UUID of the file
   * @return BlobId of the stored document
   * @throws IOException Exception when storing document
   */
  public BlobId storeDocument(InputStream content, String mediaType, UUID id) throws IOException {
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
    return blobOptional.map(blob -> storage.delete(blob.getBlobId())).orElse(false);
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

  public boolean hasBytes(BlobId blobId) throws NotFoundException {
    Blob blob = storage.get(blobId);
    if (blob != null) {
      return blob.getSize() > 0;
    }
    throw new NotFoundException(String.format("Document not found: %s", blobId.toString()));
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
