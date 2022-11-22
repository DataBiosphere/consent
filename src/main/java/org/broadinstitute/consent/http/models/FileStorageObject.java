package org.broadinstitute.consent.http.models;

import com.google.cloud.storage.BlobId;
import org.broadinstitute.consent.http.enumeration.FileCategory;

import java.io.InputStream;
import java.time.Instant;
import java.util.Objects;

public class FileStorageObject {

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
