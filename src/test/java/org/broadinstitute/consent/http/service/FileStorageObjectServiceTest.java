package org.broadinstitute.consent.http.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.storage.BlobId;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.apache.commons.lang3.RandomStringUtils;
import org.broadinstitute.consent.http.cloudstore.GCSService;
import org.broadinstitute.consent.http.db.FileStorageObjectDAO;
import org.broadinstitute.consent.http.enumeration.FileCategory;
import org.broadinstitute.consent.http.models.FileStorageObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FileStorageObjectServiceTest {

  @Mock
  private FileStorageObjectDAO fileStorageObjectDAO;

  @Mock
  private GCSService gcsService;

  private FileStorageObjectService service;

  private void initService() {
    service = new FileStorageObjectService(fileStorageObjectDAO, gcsService);
  }

  @Test
  void testUploadAndStoreFile() throws IOException {
    InputStream content = new ByteArrayInputStream(RandomStringUtils.random(20).getBytes());
    String fileName = RandomStringUtils.randomAlphabetic(10);
    String mediaType = RandomStringUtils.randomAlphabetic(10);
    FileCategory category = List.of(FileCategory.values())
        .get(new Random().nextInt(FileCategory.values().length));
    String entityId = RandomStringUtils.randomAlphabetic(10);
    Integer createUserId = new Random().nextInt();

    String bucket = RandomStringUtils.randomAlphabetic(10);
    String blob = RandomStringUtils.randomAlphabetic(10);

    when(fileStorageObjectDAO.insertNewFile(
        eq(fileName),
        eq(category.getValue()),
        eq(BlobId.of(bucket, blob).toGsUtilUri()),
        eq(mediaType),
        eq(entityId),
        eq(createUserId),
        any())).thenReturn(10);

    FileStorageObject newFileStorageObject = new FileStorageObject();
    newFileStorageObject.setFileName(RandomStringUtils.randomAlphabetic(10));

    when(fileStorageObjectDAO.findFileById(10)).thenReturn(newFileStorageObject);
    when(
        gcsService.storeDocument(eq(content), eq(mediaType), any())
    ).thenReturn(BlobId.of(bucket, blob));

    initService();

    FileStorageObject returned = service.uploadAndStoreFile(content, fileName, mediaType, category,
        entityId, createUserId);

    assertEquals(newFileStorageObject, returned);

    verify(
        gcsService, times(1)
    ).storeDocument(eq(content), eq(mediaType), any());

    verify(
        fileStorageObjectDAO, times(1)
    ).insertNewFile(
        eq(fileName),
        eq(category.getValue()),
        eq(BlobId.of(bucket, blob).toGsUtilUri()),
        eq(mediaType),
        eq(entityId),
        eq(createUserId),
        any());

  }

  @Test
  void testFetchById() throws IOException {
    String bucket = RandomStringUtils.randomAlphabetic(10);
    String blob = RandomStringUtils.randomAlphabetic(10);

    FileStorageObject file = new FileStorageObject();
    file.setBlobId(BlobId.of(bucket, blob));

    String content = RandomStringUtils.random(100);

    when(
        gcsService.getDocument(BlobId.of(bucket, blob))
    ).thenReturn(new ByteArrayInputStream(content.getBytes()));

    when(
        fileStorageObjectDAO.findFileById(10)
    ).thenReturn(file);

    initService();

    FileStorageObject returned = service.fetchById(10);

    assertEquals(file, returned);

    assertArrayEquals(content.getBytes(), returned.getUploadedFile().readAllBytes());
  }

  @Test
  void testFetchAllByEntityId() throws IOException {
    String bucket1Name = RandomStringUtils.randomAlphabetic(10);
    String blob1Name = RandomStringUtils.randomAlphabetic(10);
    String bucket2Name = RandomStringUtils.randomAlphabetic(10);
    String blob2Name = RandomStringUtils.randomAlphabetic(10);
    String bucket3Name = RandomStringUtils.randomAlphabetic(10);
    String blob3Name = RandomStringUtils.randomAlphabetic(10);

    FileStorageObject file1 = new FileStorageObject();
    file1.setBlobId(BlobId.of(bucket1Name, blob1Name));

    FileStorageObject file2 = new FileStorageObject();
    file2.setBlobId(BlobId.of(bucket2Name, blob2Name));

    FileStorageObject file3 = new FileStorageObject();
    file3.setBlobId(BlobId.of(bucket3Name, blob3Name));

    String content1 = RandomStringUtils.randomAlphabetic(10);
    String content2 = RandomStringUtils.randomAlphabetic(10);
    String content3 = RandomStringUtils.randomAlphabetic(10);

    when(
        gcsService.getDocuments(List.of(file1.getBlobId(), file2.getBlobId(), file3.getBlobId()))
    ).thenReturn(Map.of(
        file1.getBlobId(), new ByteArrayInputStream(content1.getBytes()),
        file2.getBlobId(), new ByteArrayInputStream(content2.getBytes()),
        file3.getBlobId(), new ByteArrayInputStream(content3.getBytes())
    ));

    String entityId = RandomStringUtils.randomAlphabetic(10);

    when(
        fileStorageObjectDAO.findFilesByEntityId(entityId)
    ).thenReturn(List.of(file1, file2, file3));

    initService();

    List<FileStorageObject> returned = service.fetchAllByEntityId(entityId);

    assertEquals(3, returned.size());

    assertEquals(file1, returned.get(0));
    assertEquals(file2, returned.get(1));
    assertEquals(file3, returned.get(2));

    assertArrayEquals(content1.getBytes(),
        returned.get(0).getUploadedFile().readAllBytes());
    assertArrayEquals(content2.getBytes(),
        returned.get(1).getUploadedFile().readAllBytes());
    assertArrayEquals(content3.getBytes(),
        returned.get(2).getUploadedFile().readAllBytes());
  }

  @Test
  void testFetchAllByEntityIdAndCategory() throws IOException {
    String bucket1Name = RandomStringUtils.randomAlphabetic(10);
    String blob1Name = RandomStringUtils.randomAlphabetic(10);
    String bucket2Name = RandomStringUtils.randomAlphabetic(10);
    String blob2Name = RandomStringUtils.randomAlphabetic(10);
    String bucket3Name = RandomStringUtils.randomAlphabetic(10);
    String blob3Name = RandomStringUtils.randomAlphabetic(10);

    FileStorageObject file1 = new FileStorageObject();
    file1.setBlobId(BlobId.of(bucket1Name, blob1Name));

    FileStorageObject file2 = new FileStorageObject();
    file2.setBlobId(BlobId.of(bucket2Name, blob2Name));

    FileStorageObject file3 = new FileStorageObject();
    file3.setBlobId(BlobId.of(bucket3Name, blob3Name));

    String content1 = RandomStringUtils.randomAlphabetic(10);
    String content2 = RandomStringUtils.randomAlphabetic(10);
    String content3 = RandomStringUtils.randomAlphabetic(10);

    when(
        gcsService.getDocuments(List.of(file1.getBlobId(), file2.getBlobId(), file3.getBlobId()))
    ).thenReturn(Map.of(
        file1.getBlobId(), new ByteArrayInputStream(content1.getBytes()),
        file2.getBlobId(), new ByteArrayInputStream(content2.getBytes()),
        file3.getBlobId(), new ByteArrayInputStream(content3.getBytes())
    ));

    String entityId = RandomStringUtils.randomAlphabetic(10);
    FileCategory category = List.of(FileCategory.values())
        .get(new Random().nextInt(FileCategory.values().length));

    when(
        fileStorageObjectDAO.findFilesByEntityIdAndCategory(entityId, category.getValue())
    ).thenReturn(List.of(file1, file2, file3));

    initService();

    List<FileStorageObject> returned = service.fetchAllByEntityIdAndCategory(entityId, category);

    assertEquals(3, returned.size());

    assertEquals(file1, returned.get(0));
    assertEquals(file2, returned.get(1));
    assertEquals(file3, returned.get(2));

    assertArrayEquals(content1.getBytes(),
        returned.get(0).getUploadedFile().readAllBytes());
    assertArrayEquals(content2.getBytes(),
        returned.get(1).getUploadedFile().readAllBytes());
    assertArrayEquals(content3.getBytes(),
        returned.get(2).getUploadedFile().readAllBytes());
  }

}
