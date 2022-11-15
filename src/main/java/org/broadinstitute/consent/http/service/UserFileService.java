package org.broadinstitute.consent.http.service;

import com.google.cloud.storage.BlobId;
import org.broadinstitute.consent.http.cloudstore.GCSService;
import org.broadinstitute.consent.http.db.UserFileDAO;
import org.broadinstitute.consent.http.enumeration.UserFileCategory;
import org.broadinstitute.consent.http.models.UserFile;

import javax.ws.rs.NotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class UserFileService {

    GCSService gcsService;
    UserFileDAO userFileDAO;

    public UserFileService(UserFileDAO userFileDAO, GCSService gcsService) {

    }

    UserFile uploadAndStoreUserFile(
            InputStream content,
            String fileName,
            String mediaType,
            UserFileCategory category,
            String entityId,
            Integer createUserId
    ) throws IOException {
        // upload to GCS
        BlobId blobId = gcsService.storeDocument(
                content,
                mediaType,
                UUID.randomUUID().toString());

        // insert file
        Integer userFileId = userFileDAO.insertNewFile(
                fileName,
                category.getValue(),
                blobId.getBucket(),
                blobId.getName(),
                mediaType,
                entityId,
                createUserId,
                new Date()
        );

        return userFileDAO.findFileById(userFileId);
    }

    // fetches file from GCS and adds it to the userFile
    private void fetchAndPopulateUploadedFile(UserFile userFile) throws NotFoundException  {
        InputStream document = gcsService.getDocument(
                BlobId.of(
                        userFile.getBucketName(),
                        userFile.getBlobName()));

        userFile.setUploadedFile(document);
    }

    public UserFile fetchById(
            Integer userFileId
    ) throws NotFoundException {
        UserFile userFile = userFileDAO.findFileById(userFileId);
        // download file from GCS
        fetchAndPopulateUploadedFile(userFile);
        return userFile;
    }

    public List<UserFile> fetchAllByEntityId(
            String entityId
    ) throws NotFoundException {
        List<UserFile> userFiles = userFileDAO.findFilesByEntityId(entityId);
        // download all files from GCS
        userFiles.forEach(this::fetchAndPopulateUploadedFile);
        return userFiles;
    }

    public List<UserFile> fetchAllByEntityIdAndCategory(
            String entityId, UserFileCategory category
    ) throws NotFoundException {
        List<UserFile> userFiles = userFileDAO.findFilesByEntityIdAndCategory(entityId, category.getValue());
        // download all files from GCS
        userFiles.forEach(this::fetchAndPopulateUploadedFile);
        return userFiles;
    }
}
