package org.broadinstitute.consent.http.models;

import com.google.cloud.storage.BlobId;
import org.broadinstitute.consent.http.enumeration.FileCategory;

import java.io.InputStream;
import java.util.Date;
import java.util.Objects;

public class FileStorageObject {

    public static final String FILE_STORAGE_OBJECT_QUERY_WITH_FSO_PREFIX =
            "fso.file_storage_object_id as fso_use_file_id, "
            + " fso.entity_id as fso_entity_id, "
            + " fso.file_name as fso_file_name, "
            + " fso.bucket_name as fso_file_name, "
            + " fso.blob_id as fso_file_name, "
            + " fso.category as fso_file_name, "
            + " fso.media_type as fso_file_name, "
            + " fso.create_user_id as fso_file_name, "
            + " fso.create_date as fso_create_date, "
            + " fso.deleted as fso_deleted ";

    private Integer fileStorageObjectId;
    private String entityId;
    private String fileName;
    private BlobId blobId;
    private FileCategory category;
    private String mediaType;
    private Integer createUserId;
    private Date createDate;
    private Boolean deleted;
    private Integer deleteUserId;
    private Date deleteDate;
    private Integer updateUserId;
    private Date updateDate;
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

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileStorageObject fileStorageObject = (FileStorageObject) o;
        return Objects.equals(fileStorageObjectId, fileStorageObject.fileStorageObjectId) && Objects.equals(entityId, fileStorageObject.entityId) && Objects.equals(fileName, fileStorageObject.fileName) && Objects.equals(blobId, fileStorageObject.blobId) && category == fileStorageObject.category && Objects.equals(mediaType, fileStorageObject.mediaType) && Objects.equals(createUserId, fileStorageObject.createUserId) && Objects.equals(createDate, fileStorageObject.createDate) && Objects.equals(deleted, fileStorageObject.deleted);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileStorageObjectId, entityId, fileName, blobId, category, mediaType, createUserId, createDate, deleted);
    }

    public Integer getDeleteUserId() {
        return deleteUserId;
    }

    public void setDeleteUserId(Integer deleteUserId) {
        this.deleteUserId = deleteUserId;
    }

    public Date getDeleteDate() {
        return deleteDate;
    }

    public void setDeleteDate(Date deleteDate) {
        this.deleteDate = deleteDate;
    }

    public Integer getUpdateUserId() {
        return updateUserId;
    }

    public void setUpdateUserId(Integer updateUserId) {
        this.updateUserId = updateUserId;
    }

    public Date getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(Date updateDate) {
        this.updateDate = updateDate;
    }

    public BlobId getBlobId() {
        return blobId;
    }

    public void setBlobId(BlobId blobId) {
        this.blobId = blobId;
    }
}
