package org.broadinstitute.consent.http.service;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import org.broadinstitute.consent.http.cloudstore.GCSService;
import org.broadinstitute.consent.http.db.FileStorageObjectDAO;
import org.broadinstitute.consent.http.db.mapper.RowMapperHelper;
import org.broadinstitute.consent.http.enumeration.FileCategory;
import org.broadinstitute.consent.http.models.FileStorageObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.NotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class FileStorageObjectService {

    GCSService gcsService;
    FileStorageObjectDAO fileStorageObjectDAO;

    Logger log = LoggerFactory.getLogger(FileStorageObjectService.class);


    public FileStorageObjectService(FileStorageObjectDAO fileStorageObjectDAO, GCSService gcsService) {
        this.fileStorageObjectDAO = fileStorageObjectDAO;
        this.gcsService = gcsService;
    }

    FileStorageObject uploadAndStoreUserFile(
            InputStream content,
            String fileName,
            String mediaType,
            FileCategory category,
            String entityId,
            Integer createUserId
    ) throws IOException {

        BlobId blobId;
        try {
            // upload to GCS
            blobId = gcsService.storeDocument(
                    content,
                    mediaType,
                    UUID.randomUUID());
        } catch (Exception e) {
            log.error("Failed to upload user file", e);
            throw e;
        }

        // insert file
        Integer userFileId = fileStorageObjectDAO.insertNewFile(
                fileName,
                category.getValue(),
                blobId.toGsUtilUri(),
                mediaType,
                entityId,
                createUserId,
                new Date()
        );

        return fileStorageObjectDAO.findFileById(userFileId);
    }

    // fetches file from GCS and adds it to the userFile
    private void fetchAndPopulateUploadedFile(FileStorageObject fileStorageObject) throws NotFoundException  {
        try {
            InputStream document = gcsService.getDocument(fileStorageObject.getBlobId());
            fileStorageObject.setUploadedFile(document);
        } catch (Exception e) {
            log.error("Could not get file from GCS", e);
            throw e;
        }
    }

    private void fetchAndPopulateMultipleUploadedFiles(List<FileStorageObject> fileStorageObjects) throws NotFoundException  {
        try {
            Map<BlobId, InputStream> documentMap = gcsService.getDocuments(
                    fileStorageObjects.stream().map(FileStorageObject::getBlobId).collect(Collectors.toList()));

            fileStorageObjects.forEach((fso) -> fso.setUploadedFile(documentMap.get(fso.getBlobId())));

        } catch (Exception e) {
            log.error("Could not get file from GCS", e);
            throw e;
        }
    }

    public FileStorageObject fetchById(
            Integer userFileId
    ) throws NotFoundException {
        FileStorageObject fileStorageObject = fileStorageObjectDAO.findFileById(userFileId);
        // download file from GCS
        fetchAndPopulateUploadedFile(fileStorageObject);
        return fileStorageObject;
    }

    public List<FileStorageObject> fetchAllByEntityId(
            String entityId
    ) throws NotFoundException {
        List<FileStorageObject> fileStorageObjects = fileStorageObjectDAO.findFilesByEntityId(entityId);
        // download all files from GCS
        fetchAndPopulateMultipleUploadedFiles(fileStorageObjects);
        return fileStorageObjects;
    }

    public List<FileStorageObject> fetchAllByEntityIdAndCategory(
            String entityId, FileCategory category
    ) throws NotFoundException {
        List<FileStorageObject> fileStorageObjects = fileStorageObjectDAO.findFilesByEntityIdAndCategory(entityId, category.getValue());
        // download all files from GCS
        fetchAndPopulateMultipleUploadedFiles(fileStorageObjects);
        return fileStorageObjects;
    }
}
