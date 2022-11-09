package org.broadinstitute.consent.http.models;

import org.broadinstitute.consent.http.enumeration.UserFileCategory;

import java.util.Date;

public class UserFile {

    public static final String USER_FILE_QUERY_WITH_UF_PREFIX =
            "uf.user_file_id as uf_use_file_id, "
            + " uf.entity_id as uf_entity_id, "
            + " uf.file_name as uf_file_name, "
            + " uf.bucket_name as uf_file_name, "
            + " uf.blob_id as uf_file_name, "
            + " uf.category as uf_file_name, "
            + " uf.media_type as uf_file_name, "
            + " uf.create_user_id as uf_file_name, "
            + " uf.create_date as uf_create_date, "
            + " uf.deleted as uf_deleted ";

    private Integer userFileId;
    private String entityId;
    private String fileName;
    private String BucketName;
    private String blobId;
    private UserFileCategory category;
    private String mediaType;
    private Integer createUserId;
    private Date createDate;
    private Boolean deleted;

    public Integer getUserFileId() {
        return userFileId;
    }

    public void setUserFileId(Integer userFileId) {
        this.userFileId = userFileId;
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

    public String getBucketName() {
        return BucketName;
    }

    public void setBucketName(String bucketName) {
        BucketName = bucketName;
    }

    public String getBlobId() {
        return blobId;
    }

    public void setBlobId(String blobId) {
        this.blobId = blobId;
    }

    public UserFileCategory getCategory() {
        return category;
    }

    public void setCategory(UserFileCategory category) {
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
}
