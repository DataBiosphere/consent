package org.broadinstitute.consent.http.db;

import com.google.cloud.storage.BlobId;
import org.apache.commons.lang3.RandomStringUtils;
import org.broadinstitute.consent.http.enumeration.FileCategory;
import org.broadinstitute.consent.http.models.FileStorageObject;
import org.junit.Test;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class FileStorageObjectDAOTest extends DAOTestHelper {
    @Test
    public void testInsertFile() {
        String fileName = RandomStringUtils.randomAlphabetic(10);
        String category = FileCategory.getValues().get(new Random().nextInt(FileCategory.getValues().size()));
        String gcsFileUri = BlobId.of(
                RandomStringUtils.randomAlphabetic(10),
                RandomStringUtils.randomAlphabetic(10)).toGsUtilUri();
        String mediaType = RandomStringUtils.randomAlphabetic(10);
        String entityId = RandomStringUtils.randomAlphabetic(10);
        Integer createUserId = new Random().nextInt();
        Instant createDate = Instant.now();

        Integer newFileStorageObjectId = fileStorageObjectDAO.insertNewFile(
                fileName,
                category,
                gcsFileUri,
                mediaType,
                entityId,
                createUserId,
                createDate
        );

        FileStorageObject newFileStorageObject = fileStorageObjectDAO.findFileById(newFileStorageObjectId);

        assertNotNull(newFileStorageObject);
        assertNotNull(newFileStorageObject.getFileStorageObjectId());
        assertEquals(fileName, newFileStorageObject.getFileName());
        assertEquals(category, newFileStorageObject.getCategory().getValue());
        assertEquals(BlobId.fromGsUtilUri(gcsFileUri), newFileStorageObject.getBlobId());
        assertEquals(mediaType, newFileStorageObject.getMediaType());
        assertEquals(entityId, newFileStorageObject.getEntityId());
        assertEquals(createUserId, newFileStorageObject.getCreateUserId());
        assertEquals(
                createDate,
                newFileStorageObject.getCreateDate());
        assertFalse(newFileStorageObject.getDeleted());
        assertNull(newFileStorageObject.getUploadedFile());
    }

    @Test
    public void testDeleteFileById() {
        FileStorageObject origFile = createFileStorageObject();

        Integer deleteUserId = new Random().nextInt();
        Instant deleteDate = Instant.now();

        assertFalse(origFile.getDeleted());
        assertNull(origFile.getDeleteUserId());
        assertNull(origFile.getDeleteDate());

        fileStorageObjectDAO.deleteFileById(origFile.getFileStorageObjectId(), deleteUserId, deleteDate);

        FileStorageObject deletedFile = fileStorageObjectDAO.findFileById(origFile.getFileStorageObjectId());

        assertTrue(deletedFile.getDeleted());
        assertEquals(deleteUserId, deletedFile.getDeleteUserId());
        assertEquals(
                deleteDate,
                deletedFile.getDeleteDate());
    }

    @Test
    public void testDeleteFileByEntityId() {
        String entityId = RandomStringUtils.randomAlphabetic(10);
        String otherEntityId = RandomStringUtils.randomAlphabetic(8);

        Integer deleteUserId = new Random().nextInt();
        Instant deleteDate = Instant.now();

        FileStorageObject file1 = createFileStorageObject(entityId, FileCategory.IRB_COLLABORATION_LETTER);
        FileStorageObject file2 = createFileStorageObject(entityId, FileCategory.DATA_USE_LETTER);
        FileStorageObject file3 = createFileStorageObject(entityId, FileCategory.ALTERNATIVE_DATA_SHARING_PLAN);
        FileStorageObject file4 = createFileStorageObject(otherEntityId, FileCategory.IRB_COLLABORATION_LETTER);

        assertFalse(file1.getDeleted());
        assertNull(file1.getDeleteUserId());
        assertNull(file1.getDeleteDate());
        assertFalse(file2.getDeleted());
        assertNull(file2.getDeleteUserId());
        assertNull(file2.getDeleteDate());
        assertFalse(file3.getDeleted());
        assertNull(file3.getDeleteUserId());
        assertNull(file3.getDeleteDate());
        assertFalse(file4.getDeleted());
        assertNull(file4.getDeleteUserId());
        assertNull(file4.getDeleteDate());

        fileStorageObjectDAO.deleteFilesByEntityId(entityId, deleteUserId, deleteDate);

        file1 = fileStorageObjectDAO.findFileById(file1.getFileStorageObjectId());
        file2 = fileStorageObjectDAO.findFileById(file2.getFileStorageObjectId());
        file3 = fileStorageObjectDAO.findFileById(file3.getFileStorageObjectId());
        file4 = fileStorageObjectDAO.findFileById(file4.getFileStorageObjectId());

        assertTrue(file1.getDeleted());
        assertEquals(deleteUserId, file1.getDeleteUserId());
        assertEquals(
                deleteDate,
                file1.getDeleteDate());
        assertTrue(file2.getDeleted());
        assertEquals(deleteUserId, file2.getDeleteUserId());
        assertEquals(
                deleteDate,
                file2.getDeleteDate());
        assertTrue(file3.getDeleted());
        assertEquals(deleteUserId, file3.getDeleteUserId());
        assertEquals(
                deleteDate,
                file3.getDeleteDate());
        assertFalse(file4.getDeleted()); // should not be effected
        assertNull(file4.getDeleteUserId());
        assertNull(file4.getDeleteDate());
    }

    @Test
    public void testFindFilesByEntityId() {
        String entityId = RandomStringUtils.randomAlphabetic(10);

        createFileStorageObject();
        createFileStorageObject(); // random other files
        FileStorageObject file1 = createFileStorageObject(entityId, FileCategory.IRB_COLLABORATION_LETTER);
        FileStorageObject file2 = createFileStorageObject(entityId, FileCategory.DATA_USE_LETTER);
        FileStorageObject file3 = createFileStorageObject(entityId, FileCategory.ALTERNATIVE_DATA_SHARING_PLAN);

        List<FileStorageObject> filesFound = fileStorageObjectDAO.findFilesByEntityId(entityId);
        List<Integer> fileIdsfound = filesFound.stream().map(FileStorageObject::getFileStorageObjectId).toList();

        assertEquals(3, filesFound.size());
        assertTrue(fileIdsfound.contains(file1.getFileStorageObjectId()));
        assertTrue(fileIdsfound.contains(file2.getFileStorageObjectId()));
        assertTrue(fileIdsfound.contains(file3.getFileStorageObjectId()));
    }

    @Test
    public void testFindFilesByEntityIdAndCategory() {
        String entityId = RandomStringUtils.randomAlphabetic(10);

        // different entity id, same category, shouldn't be returned.
        createFileStorageObject("asdf", FileCategory.IRB_COLLABORATION_LETTER);
        createFileStorageObject();
        FileStorageObject file1 = createFileStorageObject(entityId, FileCategory.IRB_COLLABORATION_LETTER);
        FileStorageObject file2 = createFileStorageObject(entityId, FileCategory.IRB_COLLABORATION_LETTER);
        FileStorageObject file3 = createFileStorageObject(entityId, FileCategory.ALTERNATIVE_DATA_SHARING_PLAN);

        List<FileStorageObject> irbFiles = fileStorageObjectDAO.findFilesByEntityIdAndCategory(entityId, FileCategory.IRB_COLLABORATION_LETTER.getValue());
        List<FileStorageObject> altDataSharingFiles = fileStorageObjectDAO.findFilesByEntityIdAndCategory(entityId, FileCategory.ALTERNATIVE_DATA_SHARING_PLAN.getValue());

        assertEquals(2, irbFiles.size());
        assertTrue(irbFiles.contains(file1));
        assertTrue(irbFiles.contains(file2));

        assertEquals(1, altDataSharingFiles.size());
        assertTrue(altDataSharingFiles.contains(file3));
    }

    protected FileStorageObject createFileStorageObject() {
        FileCategory category = List.of(FileCategory.values()).get(new Random().nextInt(FileCategory.values().length));
        String entityId = RandomStringUtils.randomAlphabetic(10);

        return createFileStorageObject(entityId, category);
    }

    protected FileStorageObject createFileStorageObject(String entityId, FileCategory category) {
        String fileName = RandomStringUtils.randomAlphabetic(10);
        String bucketName = RandomStringUtils.randomAlphabetic(10);
        String gcsFileUri = RandomStringUtils.randomAlphabetic(10);
        Integer createUserId = new Random().nextInt();
        Instant createDate = Instant.now();

        Integer newFileStorageObjectId = fileStorageObjectDAO.insertNewFile(
                fileName,
                category.getValue(),
                bucketName,
                gcsFileUri,
                entityId,
                createUserId,
                createDate
        );
        return fileStorageObjectDAO.findFileById(newFileStorageObjectId);
    }

}
