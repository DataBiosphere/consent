package org.broadinstitute.consent.http.service;

import com.google.cloud.storage.BlobId;
import org.broadinstitute.consent.http.cloudstore.GCSService;
import org.broadinstitute.consent.http.db.FileStorageObjectDAO;
import org.broadinstitute.consent.http.enumeration.FileCategory;
import org.broadinstitute.consent.http.models.FileStorageObject;
import org.broadinstitute.consent.http.util.ConsentLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.NotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class FileStorageObjectService implements ConsentLogger {

    GCSService gcsService;
    FileStorageObjectDAO fileStorageObjectDAO;


    public FileStorageObjectService(FileStorageObjectDAO fileStorageObjectDAO, GCSService gcsService) {
        this.fileStorageObjectDAO = fileStorageObjectDAO;
        this.gcsService = gcsService;
    }

    FileStorageObject uploadAndStoreFile(
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
            logWarn("Failed to upload file for user id " + createUserId + ": " + e.getMessage());
            throw e;
        }

        // insert file
        Integer fileStorageObjectId = fileStorageObjectDAO.insertNewFile(
                fileName,
                category.getValue(),
                blobId.toGsUtilUri(),
                mediaType,
                entityId,
                createUserId,
                Instant.now()
        );

        return fileStorageObjectDAO.findFileById(fileStorageObjectId);
    }

    // fetches file from GCS and adds it to the fileStorageObject
    private void fetchAndPopulateUploadedFile(FileStorageObject fileStorageObject) throws NotFoundException  {
        try {
            InputStream document = gcsService.getDocument(fileStorageObject.getBlobId());
            fileStorageObject.setUploadedFile(document);
        } catch (NotFoundException e) {
            throw e; // pass along
        } catch (Exception e) {
            // all other exceptions
            logWarn("Failed to get document from GCS: " + e.getMessage());
            throw e;
        }
    }

    private void fetchAndPopulateMultipleUploadedFiles(List<FileStorageObject> fileStorageObjects) throws NotFoundException  {
        try {
            Map<BlobId, InputStream> documentMap = gcsService.getDocuments(
                    fileStorageObjects.stream().map(FileStorageObject::getBlobId).collect(Collectors.toList()));

            fileStorageObjects.forEach((fso) -> fso.setUploadedFile(documentMap.get(fso.getBlobId())));
        } catch (NotFoundException e) {
            throw e; // pass along
        } catch (Exception e) {
            // all other exceptions
            logWarn("Failed to get document from GCS: " + e.getMessage());
            throw e;
        }
    }

    public FileStorageObject fetchById(
            Integer fileStorageObjectId
    ) throws NotFoundException {
        FileStorageObject fileStorageObject = fileStorageObjectDAO.findFileById(fileStorageObjectId);
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
