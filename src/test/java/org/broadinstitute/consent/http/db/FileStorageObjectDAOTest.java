package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.cloud.storage.BlobId;
import java.time.Instant;
import java.util.List;
import java.util.Random;
import org.broadinstitute.consent.http.enumeration.FileCategory;
import org.broadinstitute.consent.http.models.FileStorageObject;
import org.broadinstitute.consent.http.models.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FileStorageObjectDAOTest extends DAOTestHelper {

  @Test
  void testInsertFile() {

    createFileStorageObject(); // add random other files to db
    createFileStorageObject();

    String fileName = randomAlphabetic(10);
    String category =
        FileCategory.getValues().get(new Random().nextInt(FileCategory.getValues().size()));
    String gcsFileUri = BlobId.of(randomAlphabetic(10), randomAlphabetic(10)).toGsUtilUri();
    String mediaType = randomAlphabetic(10);
    String entityId = randomAlphabetic(10);
    User createUser = createUser();
    Instant createDate = Instant.now();

    Integer newFileStorageObjectId =
        fileStorageObjectDAO.insertNewFile(
            fileName,
            category,
            gcsFileUri,
            mediaType,
            entityId,
            createUser.getUserId(),
            createDate);

    createFileStorageObject();

    FileStorageObject newFileStorageObject =
        fileStorageObjectDAO.findFileById(newFileStorageObjectId);

    assertNotNull(newFileStorageObject);
    assertNotNull(newFileStorageObject.getFileStorageObjectId());
    assertEquals(fileName, newFileStorageObject.getFileName());
    assertEquals(category, newFileStorageObject.getCategory().getValue());
    assertEquals(BlobId.fromGsUtilUri(gcsFileUri), newFileStorageObject.getBlobId());
    assertEquals(mediaType, newFileStorageObject.getMediaType());
    assertEquals(entityId, newFileStorageObject.getEntityId());
    assertEquals(createUser.getUserId(), newFileStorageObject.getCreateUserId());
    assertEquals(
        createDate.getEpochSecond(), newFileStorageObject.getCreateDate().getEpochSecond());
    assertFalse(newFileStorageObject.getDeleted());
    assertNull(newFileStorageObject.getUploadedFile());
  }

  @Test
  void testDeleteFileById() {
    FileStorageObject origFile = createFileStorageObject();

    User deleteUser = createUser();

    assertFalse(origFile.getDeleted());
    assertNull(origFile.getDeleteUserId());
    assertNull(origFile.getDeleteDate());

    fileStorageObjectDAO.deleteFileById(origFile.getFileStorageObjectId(), deleteUser.getUserId());

    FileStorageObject deletedFile =
        fileStorageObjectDAO.findFileById(origFile.getFileStorageObjectId());

    assertTrue(deletedFile.getDeleted());
    assertEquals(deleteUser.getUserId(), deletedFile.getDeleteUserId());
    assertTrue(deletedFile.getDeleteDate().getEpochSecond() > 0);
  }

  @Test
  void testDeleteFileByEntityId() {
    String entityId = randomAlphabetic(10);
    String otherEntityId = randomAlphabetic(8);

    User deleteUser = createUser();
    Instant deleteDate = Instant.now();

    FileStorageObject file1 =
        createFileStorageObject(entityId, FileCategory.IRB_COLLABORATION_LETTER);
    FileStorageObject file2 = createFileStorageObject(entityId, FileCategory.DATA_USE_LETTER);
    FileStorageObject file3 =
        createFileStorageObject(entityId, FileCategory.ALTERNATIVE_DATA_SHARING_PLAN);
    FileStorageObject file4 =
        createFileStorageObject(otherEntityId, FileCategory.IRB_COLLABORATION_LETTER);

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

    fileStorageObjectDAO.deleteFilesByEntityId(entityId, deleteUser.getUserId(), deleteDate);

    file1 = fileStorageObjectDAO.findFileById(file1.getFileStorageObjectId());
    file2 = fileStorageObjectDAO.findFileById(file2.getFileStorageObjectId());
    file3 = fileStorageObjectDAO.findFileById(file3.getFileStorageObjectId());
    file4 = fileStorageObjectDAO.findFileById(file4.getFileStorageObjectId());

    assertTrue(file1.getDeleted());
    assertEquals(deleteUser.getUserId(), file1.getDeleteUserId());
    assertEquals(deleteDate.getEpochSecond(), file1.getDeleteDate().getEpochSecond());
    assertTrue(file2.getDeleted());
    assertEquals(deleteUser.getUserId(), file2.getDeleteUserId());
    assertEquals(deleteDate.getEpochSecond(), file2.getDeleteDate().getEpochSecond());
    assertTrue(file3.getDeleted());
    assertEquals(deleteUser.getUserId(), file3.getDeleteUserId());
    assertEquals(deleteDate.getEpochSecond(), file3.getDeleteDate().getEpochSecond());
    assertFalse(file4.getDeleted()); // should not be effected
    assertNull(file4.getDeleteUserId());
    assertNull(file4.getDeleteDate());
  }

  @Test
  void testFindFilesByEntityId() {
    String entityId = randomAlphabetic(10);

    createFileStorageObject();
    createFileStorageObject(); // random other files
    FileStorageObject file1 =
        createFileStorageObject(entityId, FileCategory.IRB_COLLABORATION_LETTER);
    FileStorageObject file2 = createFileStorageObject(entityId, FileCategory.DATA_USE_LETTER);
    FileStorageObject file3 =
        createFileStorageObject(entityId, FileCategory.ALTERNATIVE_DATA_SHARING_PLAN);

    List<FileStorageObject> filesFound = fileStorageObjectDAO.findFilesByEntityId(entityId);
    List<Integer> fileIdsfound =
        filesFound.stream().map(FileStorageObject::getFileStorageObjectId).toList();

    assertEquals(3, filesFound.size());
    assertTrue(fileIdsfound.contains(file1.getFileStorageObjectId()));
    assertTrue(fileIdsfound.contains(file2.getFileStorageObjectId()));
    assertTrue(fileIdsfound.contains(file3.getFileStorageObjectId()));
  }

  @Test
  void testFindFilesByEntityIdAndCategory() {
    String entityId = randomAlphabetic(10);

    // different entity id, same category, shouldn't be returned.
    createFileStorageObject("asdf", FileCategory.IRB_COLLABORATION_LETTER);
    createFileStorageObject();
    FileStorageObject file1 =
        createFileStorageObject(entityId, FileCategory.IRB_COLLABORATION_LETTER);
    FileStorageObject file2 =
        createFileStorageObject(entityId, FileCategory.IRB_COLLABORATION_LETTER);
    FileStorageObject file3 =
        createFileStorageObject(entityId, FileCategory.ALTERNATIVE_DATA_SHARING_PLAN);

    List<FileStorageObject> irbFiles =
        fileStorageObjectDAO.findFilesByEntityIdAndCategory(
            entityId, FileCategory.IRB_COLLABORATION_LETTER.getValue());
    List<FileStorageObject> altDataSharingFiles =
        fileStorageObjectDAO.findFilesByEntityIdAndCategory(
            entityId, FileCategory.ALTERNATIVE_DATA_SHARING_PLAN.getValue());

    assertEquals(2, irbFiles.size());
    assertTrue(irbFiles.contains(file1));
    assertTrue(irbFiles.contains(file2));

    assertEquals(1, altDataSharingFiles.size());
    assertTrue(altDataSharingFiles.contains(file3));
  }

  @Test
  void testFindFileMetadataByEntityId() {
    String entityId = randomAlphabetic(10);
    String otherEntityId = randomAlphabetic(10);
    User createUser = createUser();
    Instant oldest = Instant.now().minusSeconds(300);
    Instant middle = Instant.now().minusSeconds(200);
    Instant newest = Instant.now().minusSeconds(100);

    Integer oldestId =
        fileStorageObjectDAO.insertNewFile(
            randomAlphabetic(10),
            FileCategory.IRB_COLLABORATION_LETTER.getValue(),
            BlobId.of(randomAlphabetic(10), randomAlphabetic(10)).toGsUtilUri(),
            randomAlphabetic(10),
            entityId,
            createUser.getUserId(),
            oldest);

    Integer middleId =
        fileStorageObjectDAO.insertNewFile(
            randomAlphabetic(10),
            FileCategory.DATA_USE_LETTER.getValue(),
            BlobId.of(randomAlphabetic(10), randomAlphabetic(10)).toGsUtilUri(),
            randomAlphabetic(10),
            entityId,
            createUser.getUserId(),
            middle);

    Integer newestId =
        fileStorageObjectDAO.insertNewFile(
            randomAlphabetic(10),
            FileCategory.ALTERNATIVE_DATA_SHARING_PLAN.getValue(),
            BlobId.of(randomAlphabetic(10), randomAlphabetic(10)).toGsUtilUri(),
            randomAlphabetic(10),
            entityId,
            createUser.getUserId(),
            newest);

    Integer deletedId =
        fileStorageObjectDAO.insertNewFile(
            randomAlphabetic(10),
            FileCategory.IRB_COLLABORATION_LETTER.getValue(),
            BlobId.of(randomAlphabetic(10), randomAlphabetic(10)).toGsUtilUri(),
            randomAlphabetic(10),
            entityId,
            createUser.getUserId(),
            Instant.now());
    fileStorageObjectDAO.deleteFileById(deletedId, createUser.getUserId());

    fileStorageObjectDAO.insertNewFile(
        randomAlphabetic(10),
        FileCategory.IRB_COLLABORATION_LETTER.getValue(),
        BlobId.of(randomAlphabetic(10), randomAlphabetic(10)).toGsUtilUri(),
        randomAlphabetic(10),
        otherEntityId,
        createUser.getUserId(),
        Instant.now());

    jdbi.useHandle(
        handle ->
            handle
                .createUpdate(
                    "UPDATE file_storage_object SET deleted = NULL WHERE file_storage_object_id = :id")
                .bind("id", middleId)
                .execute());

    List<FileStorageObject> filesFound = fileStorageObjectDAO.findFileMetadataByEntityId(entityId);
    List<Integer> foundIds =
        filesFound.stream().map(FileStorageObject::getFileStorageObjectId).toList();

    assertEquals(3, filesFound.size());
    assertEquals(newestId, filesFound.get(0).getFileStorageObjectId());
    assertEquals(middleId, filesFound.get(1).getFileStorageObjectId());
    assertEquals(oldestId, filesFound.get(2).getFileStorageObjectId());

    assertTrue(foundIds.contains(newestId));
    assertTrue(foundIds.contains(middleId));
    assertTrue(foundIds.contains(oldestId));
    assertFalse(foundIds.contains(deletedId));

    filesFound.forEach(file -> assertEquals(entityId, file.getEntityId()));
  }

  @Test
  void testFindActiveFileByIdAndEntityId() {
    String entityId = randomAlphabetic(10);
    FileStorageObject file = createFileStorageObject(entityId, FileCategory.DATA_USE_LETTER);

    FileStorageObject found =
        fileStorageObjectDAO.findActiveFileByIdAndEntityId(entityId, file.getFileStorageObjectId());

    assertNotNull(found);
    assertEquals(file.getFileStorageObjectId(), found.getFileStorageObjectId());
    assertEquals(entityId, found.getEntityId());
  }

  @Test
  void testFindActiveFileByIdAndEntityIdDeletedReturnsNull() {
    String entityId = randomAlphabetic(10);
    FileStorageObject file = createFileStorageObject(entityId, FileCategory.DATA_USE_LETTER);
    User deleteUser = createUser();
    fileStorageObjectDAO.deleteFileById(file.getFileStorageObjectId(), deleteUser.getUserId());

    FileStorageObject found =
        fileStorageObjectDAO.findActiveFileByIdAndEntityId(entityId, file.getFileStorageObjectId());

    assertNull(found);
  }

  @Test
  void testFindActiveFileByIdAndEntityIdWrongEntityReturnsNull() {
    String entityId = randomAlphabetic(10);
    FileStorageObject file = createFileStorageObject(entityId, FileCategory.DATA_USE_LETTER);

    FileStorageObject found =
        fileStorageObjectDAO.findActiveFileByIdAndEntityId(
            randomAlphabetic(8), file.getFileStorageObjectId());

    assertNull(found);
  }

  @Test
  void testCreateFile() {
    String fileName = randomAlphabetic(10);
    String category = FileCategory.DATA_USE_LETTER.getValue();
    String gcsFileUri = BlobId.of(randomAlphabetic(10), randomAlphabetic(10)).toGsUtilUri();
    String mediaType = randomAlphabetic(10);
    String entityId = randomAlphabetic(10);
    User createUser = createUser();
    Instant createDate = Instant.now();

    Integer newFileStorageObjectId =
        fileStorageObjectDAO.create(
            fileName,
            category,
            gcsFileUri,
            mediaType,
            entityId,
            createUser.getUserId(),
            createDate);

    FileStorageObject newFileStorageObject =
        fileStorageObjectDAO.findFileById(newFileStorageObjectId);

    assertNotNull(newFileStorageObject);
    assertEquals(fileName, newFileStorageObject.getFileName());
    assertEquals(category, newFileStorageObject.getCategory().getValue());
    assertEquals(entityId, newFileStorageObject.getEntityId());
    assertEquals(createUser.getUserId(), newFileStorageObject.getCreateUserId());
  }

  @Test
  void testSoftDelete() {
    String entityId = randomAlphabetic(10);
    String otherEntityId = randomAlphabetic(8);
    FileStorageObject target = createFileStorageObject(entityId, FileCategory.DATA_USE_LETTER);
    FileStorageObject other = createFileStorageObject(otherEntityId, FileCategory.DATA_USE_LETTER);
    User deleteUser = createUser();
    Instant deleteDate = Instant.now();

    fileStorageObjectDAO.softDelete(
        entityId, target.getFileStorageObjectId(), deleteUser.getUserId(), deleteDate);

    FileStorageObject deleted = fileStorageObjectDAO.findFileById(target.getFileStorageObjectId());
    FileStorageObject untouched = fileStorageObjectDAO.findFileById(other.getFileStorageObjectId());

    assertEquals(Boolean.TRUE, deleted.getDeleted());
    assertEquals(deleteUser.getUserId(), deleted.getDeleteUserId());
    assertEquals(deleteDate.getEpochSecond(), deleted.getDeleteDate().getEpochSecond());
    assertNotEquals(Boolean.TRUE, untouched.getDeleted());
    assertNull(untouched.getDeleteUserId());
    assertNull(untouched.getDeleteDate());
  }

  @Test
  void testUpdateCategory() {
    String entityId = randomAlphabetic(10);
    FileStorageObject original =
        createFileStorageObject(entityId, FileCategory.IRB_COLLABORATION_LETTER);
    User updateUser = createUser();
    Instant updateDate = Instant.now();

    fileStorageObjectDAO.updateCategory(
        original.getFileStorageObjectId(),
        FileCategory.DATA_USE_LETTER.getValue(),
        updateUser.getUserId(),
        updateDate);

    FileStorageObject updated =
        fileStorageObjectDAO.findFileById(original.getFileStorageObjectId());

    assertEquals(FileCategory.DATA_USE_LETTER.getValue(), updated.getCategory().getValue());
    assertEquals(updateUser.getUserId(), updated.getUpdateUserId());
    assertEquals(updateDate.getEpochSecond(), updated.getUpdateDate().getEpochSecond());
    assertEquals(original.getFileName(), updated.getFileName());
    assertEquals(original.getEntityId(), updated.getEntityId());
    assertEquals(original.getBlobId(), updated.getBlobId());
  }

  private FileStorageObject createFileStorageObject() {
    FileCategory category =
        List.of(FileCategory.values()).get(new Random().nextInt(FileCategory.values().length));
    String entityId = randomAlphabetic(10);

    return createFileStorageObject(entityId, category);
  }

  private FileStorageObject createFileStorageObject(String entityId, FileCategory category) {
    String fileName = randomAlphanumeric(10);
    String gcsFileUri = BlobId.of(randomAlphanumeric(10), randomAlphanumeric(10)).toGsUtilUri();
    String mediaType = randomAlphanumeric(10);
    User createUser = createUser();
    Instant createDate = Instant.now();

    Integer newFileStorageObjectId =
        fileStorageObjectDAO.insertNewFile(
            fileName,
            category.getValue(),
            gcsFileUri,
            mediaType,
            entityId,
            createUser.getUserId(),
            createDate);
    return fileStorageObjectDAO.findFileById(newFileStorageObjectId);
  }
}
