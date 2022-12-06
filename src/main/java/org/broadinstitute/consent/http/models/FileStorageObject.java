package org.broadinstitute.consent.http.models;

import com.google.cloud.storage.BlobId;
import org.broadinstitute.consent.http.enumeration.FileCategory;

import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FileStorageObject {

    public static final String QUERY_FIELDS_WITH_FSO_PREFIX =
            """
            fso.file_storage_object_id AS fso_file_storage_object_id,
            fso.entity_id AS fso_entity_id,
            fso.file_name AS fso_file_name,
            fso.category AS fso_category,
            fso.gcs_file_uri AS fso_gcs_file_uri,
            fso.media_type AS fso_media_type,
            fso.create_date AS fso_create_date,
            fso.create_user_id AS fso_create_user_id,
            fso.update_date AS fso_update_date,
            fso.update_user_id AS fso_update_user_id,
            fso.deleted AS fso_deleted,
            fso.delete_user_id AS fso_delete_user_id
            """;

    private Integer fileStorageObjectId;
    private String entityId;
    private String fileName;
    private BlobId blobId;
    private FileCategory category;
    private String mediaType;
    private Integer createUserId;
    private Instant createDate;
    private Boolean deleted;
    private Integer deleteUserId;
    private Instant deleteDate;
    private Integer updateUserId;
    private Instant updateDate;
    // only populated when using `fetch` methods in service class
    private InputStream uploadedFile;

    public Integer getFileStorageObjectId() {
        return fileStorageObjectId;
    }

    public void setFileStorageObjectId(Integer fileStorageObjectId) {
        this.fileStorageObjectId = fileStorageObjectId;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public FileCategory getCategory() {
        return category;
    }

    public void setCategory(FileCategory category) {
        this.category = category;
    }

    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    public Integer getCreateUserId() {
        return createUserId;
    }

    public void setCreateUserId(Integer createUserId) {
        this.createUserId = createUserId;
    }

    public Instant getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Instant createDate) {
        this.createDate = createDate;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

    public InputStream getUploadedFile() {
        return uploadedFile;
    }

    public void setUploadedFile(InputStream uploadedFile) {
        this.uploadedFile = uploadedFile;
    }

    public Integer getDeleteUserId() {
        return deleteUserId;
    }

    public void setDeleteUserId(Integer deleteUserId) {
        this.deleteUserId = deleteUserId;
    }

    public Instant getDeleteDate() {
        return deleteDate;
    }

    public void setDeleteDate(Instant deleteDate) {
        this.deleteDate = deleteDate;
    }

    public Integer getUpdateUserId() {
        return updateUserId;
    }

    public void setUpdateUserId(Integer updateUserId) {
        this.updateUserId = updateUserId;
    }

    public Instant getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(Instant updateDate) {
        this.updateDate = updateDate;
    }

    public BlobId getBlobId() {
        return blobId;
    }

    public void setBlobId(BlobId blobId) {
        this.blobId = blobId;
    }

    /**
     * Computes the last time this file was changed, be it created, updated, or deleted.
     * @return The last time the file has been changed.
     */
    public Instant getLatestUpdateDate() {
        List<Instant> dates = new ArrayList<>();
        dates.add(this.getCreateDate());
        dates.add(this.getUpdateDate());
        dates.add(this.getDeleteDate());
        return dates.stream().filter(Objects::nonNull).max(Instant::compareTo).orElse(this.getCreateDate());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileStorageObject fileStorageObject = (FileStorageObject) o;
        return Objects.equals(fileStorageObjectId, fileStorageObject.fileStorageObjectId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileStorageObjectId, entityId, fileName, blobId, category, mediaType, createUserId, createDate, deleted);
    }
}
