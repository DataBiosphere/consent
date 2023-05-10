package org.broadinstitute.consent.http.db;

import com.google.cloud.storage.BlobId;
import java.time.Instant;
import java.util.List;
import java.util.Random;
import org.apache.commons.lang3.RandomStringUtils;
import org.broadinstitute.consent.http.enumeration.FileCategory;
import org.broadinstitute.consent.http.models.FileStorageObject;
import org.broadinstitute.consent.http.models.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class FileStorageObjectDAOTest extends DAOTestHelper {
    @Test
    public void testInsertFile() {

        createFileStorageObject(); // add random other files to db
        createFileStorageObject();

        String fileName = RandomStringUtils.randomAlphabetic(10);
        String category = FileCategory.getValues().get(new Random().nextInt(FileCategory.getValues().size()));
        String gcsFileUri = BlobId.of(
                RandomStringUtils.randomAlphabetic(10),
                RandomStringUtils.randomAlphabetic(10)).toGsUtilUri();
        String mediaType = RandomStringUtils.randomAlphabetic(10);
        String entityId = RandomStringUtils.randomAlphabetic(10);
        User createUser = createUser();
        Instant createDate = Instant.now();

        Integer newFileStorageObjectId = fileStorageObjectDAO.insertNewFile(
                fileName,
                category,
                gcsFileUri,
                mediaType,
                entityId,
                createUser.getUserId(),
                createDate
        );

        createFileStorageObject();


        FileStorageObject newFileStorageObject = fileStorageObjectDAO.findFileById(newFileStorageObjectId);

        Assertions.assertNotNull(newFileStorageObject);
        Assertions.assertNotNull(newFileStorageObject.getFileStorageObjectId());
        Assertions.assertEquals(fileName, newFileStorageObject.getFileName());
        Assertions.assertEquals(category, newFileStorageObject.getCategory().getValue());
        Assertions.assertEquals(BlobId.fromGsUtilUri(gcsFileUri), newFileStorageObject.getBlobId());
        Assertions.assertEquals(mediaType, newFileStorageObject.getMediaType());
        Assertions.assertEquals(entityId, newFileStorageObject.getEntityId());
        Assertions.assertEquals(createUser.getUserId(), newFileStorageObject.getCreateUserId());
        Assertions.assertEquals(createDate.getEpochSecond(),
            newFileStorageObject.getCreateDate().getEpochSecond());
        Assertions.assertFalse(newFileStorageObject.getDeleted());
        Assertions.assertNull(newFileStorageObject.getUploadedFile());
    }

    @Test
    public void testDeleteFileById() {
        FileStorageObject origFile = createFileStorageObject();

        User deleteUser = createUser();
        Instant deleteDate = Instant.now();

        Assertions.assertFalse(origFile.getDeleted());
        Assertions.assertNull(origFile.getDeleteUserId());
        Assertions.assertNull(origFile.getDeleteDate());

        fileStorageObjectDAO.deleteFileById(origFile.getFileStorageObjectId(), deleteUser.getUserId(), deleteDate);

        FileStorageObject deletedFile = fileStorageObjectDAO.findFileById(origFile.getFileStorageObjectId());

        Assertions.assertTrue(deletedFile.getDeleted());
        Assertions.assertEquals(deleteUser.getUserId(), deletedFile.getDeleteUserId());
        Assertions.assertEquals(deleteDate.getEpochSecond(),
            deletedFile.getDeleteDate().getEpochSecond());
    }

    @Test
    public void testDeleteFileByEntityId() {
        String entityId = RandomStringUtils.randomAlphabetic(10);
        String otherEntityId = RandomStringUtils.randomAlphabetic(8);

        User deleteUser = createUser();
        Instant deleteDate = Instant.now();

        FileStorageObject file1 = createFileStorageObject(entityId, FileCategory.IRB_COLLABORATION_LETTER);
        FileStorageObject file2 = createFileStorageObject(entityId, FileCategory.DATA_USE_LETTER);
        FileStorageObject file3 = createFileStorageObject(entityId, FileCategory.ALTERNATIVE_DATA_SHARING_PLAN);
        FileStorageObject file4 = createFileStorageObject(otherEntityId, FileCategory.IRB_COLLABORATION_LETTER);

        Assertions.assertFalse(file1.getDeleted());
        Assertions.assertNull(file1.getDeleteUserId());
        Assertions.assertNull(file1.getDeleteDate());
        Assertions.assertFalse(file2.getDeleted());
        Assertions.assertNull(file2.getDeleteUserId());
        Assertions.assertNull(file2.getDeleteDate());
        Assertions.assertFalse(file3.getDeleted());
        Assertions.assertNull(file3.getDeleteUserId());
        Assertions.assertNull(file3.getDeleteDate());
        Assertions.assertFalse(file4.getDeleted());
        Assertions.assertNull(file4.getDeleteUserId());
        Assertions.assertNull(file4.getDeleteDate());

        fileStorageObjectDAO.deleteFilesByEntityId(entityId, deleteUser.getUserId(), deleteDate);

        file1 = fileStorageObjectDAO.findFileById(file1.getFileStorageObjectId());
        file2 = fileStorageObjectDAO.findFileById(file2.getFileStorageObjectId());
        file3 = fileStorageObjectDAO.findFileById(file3.getFileStorageObjectId());
        file4 = fileStorageObjectDAO.findFileById(file4.getFileStorageObjectId());

        Assertions.assertTrue(file1.getDeleted());
        Assertions.assertEquals(deleteUser.getUserId(), file1.getDeleteUserId());
        Assertions.assertEquals(deleteDate.getEpochSecond(),
            file1.getDeleteDate().getEpochSecond());
        Assertions.assertTrue(file2.getDeleted());
        Assertions.assertEquals(deleteUser.getUserId(), file2.getDeleteUserId());
        Assertions.assertEquals(deleteDate.getEpochSecond(),
            file2.getDeleteDate().getEpochSecond());
        Assertions.assertTrue(file3.getDeleted());
        Assertions.assertEquals(deleteUser.getUserId(), file3.getDeleteUserId());
        Assertions.assertEquals(deleteDate.getEpochSecond(),
            file3.getDeleteDate().getEpochSecond());
        Assertions.assertFalse(file4.getDeleted()); // should not be effected
        Assertions.assertNull(file4.getDeleteUserId());
        Assertions.assertNull(file4.getDeleteDate());
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

        Assertions.assertEquals(3, filesFound.size());
        Assertions.assertTrue(fileIdsfound.contains(file1.getFileStorageObjectId()));
        Assertions.assertTrue(fileIdsfound.contains(file2.getFileStorageObjectId()));
        Assertions.assertTrue(fileIdsfound.contains(file3.getFileStorageObjectId()));
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

        Assertions.assertEquals(2, irbFiles.size());
        Assertions.assertTrue(irbFiles.contains(file1));
        Assertions.assertTrue(irbFiles.contains(file2));

        Assertions.assertEquals(1, altDataSharingFiles.size());
        Assertions.assertTrue(altDataSharingFiles.contains(file3));
    }

    @Test
    public void testDeleteFileStorageObjectByUserId() {
        var file1 = createFileStorageObject();
        var fileId = file1.getFileStorageObjectId();
        var userId = file1.getCreateUserId();

        FileStorageObject file2 = fileStorageObjectDAO.findFileById(fileId);
        Assertions.assertNotNull(file2);

        fileStorageObjectDAO.deleteAllUserFiles(userId);

        FileStorageObject file3 = fileStorageObjectDAO.findFileById(fileId);
        Assertions.assertNull(file3);
    }

    private FileStorageObject createFileStorageObject() {
        FileCategory category = List.of(FileCategory.values()).get(new Random().nextInt(FileCategory.values().length));
        String entityId = RandomStringUtils.randomAlphabetic(10);

        return createFileStorageObject(entityId, category);
    }

    private FileStorageObject createFileStorageObject(String entityId, FileCategory category) {
        String fileName = RandomStringUtils.randomAlphabetic(10);
        String bucketName = RandomStringUtils.randomAlphabetic(10);
        String gcsFileUri = RandomStringUtils.randomAlphabetic(10);
        User createUser = createUser();
        Instant createDate = Instant.now();

        Integer newFileStorageObjectId = fileStorageObjectDAO.insertNewFile(
                fileName,
                category.getValue(),
                bucketName,
                gcsFileUri,
                entityId,
                createUser.getUserId(),
                createDate
        );
        return fileStorageObjectDAO.findFileById(newFileStorageObjectId);
    }

}
