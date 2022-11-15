package org.broadinstitute.consent.http.service;

import com.google.cloud.storage.BlobId;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.cloudstore.GCSService;
import org.broadinstitute.consent.http.db.InstitutionDAO;
import org.broadinstitute.consent.http.db.LibraryCardDAO;
import org.broadinstitute.consent.http.db.SamDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.db.UserFileDAO;
import org.broadinstitute.consent.http.db.UserPropertyDAO;
import org.broadinstitute.consent.http.db.UserRoleDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.enumeration.UserFileCategory;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.LibraryCard;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserFile;
import org.broadinstitute.consent.http.models.UserProperty;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.models.UserUpdateFields;
import org.broadinstitute.consent.http.models.sam.UserStatus;
import org.broadinstitute.consent.http.models.sam.UserStatusInfo;
import org.broadinstitute.consent.http.service.UserService.SimplifiedUser;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class UserFileServiceTest {

    @Mock
    UserFileDAO userFileDAO;

    @Mock
    GCSService gcsService;

    UserFileService service;

    @Before
    public void setUp() {
        openMocks(this);
    }

    private void initService() {
        service = new UserFileService(userFileDAO, gcsService);
    }

    @Test
    public void testUploadAndStoreUserFile() throws IOException {
        InputStream content = new ByteArrayInputStream(new byte[]{});
        String fileName = RandomStringUtils.randomAlphabetic(10);
        String mediaType = RandomStringUtils.randomAlphabetic(10);
        UserFileCategory category = List.of(UserFileCategory.values()).get(new Random().nextInt(UserFileCategory.values().length));
        String entityId = RandomStringUtils.randomAlphabetic(10);
        Integer createUserId = new Random().nextInt();
        String bucketName = RandomStringUtils.randomAlphabetic(10);
        String blobName = RandomStringUtils.randomAlphabetic(10);


        when(userFileDAO.insertNewFile(
                eq(fileName),
                eq(category.getValue()),
                eq(bucketName),
                eq(blobName),
                eq(mediaType),
                eq(entityId),
                eq(createUserId),
                any())).thenReturn(10);

        UserFile newUserFile = new UserFile();
        newUserFile.setFileName(RandomStringUtils.randomAlphabetic(10));

        when(userFileDAO.findFileById(10)).thenReturn(newUserFile);
        when(
                gcsService.storeDocument(eq(content), eq(mediaType), any())
        ).thenReturn(BlobId.of(bucketName, blobName));

        spy(userFileDAO);
        spy(gcsService);

        initService();

        UserFile returned = service.uploadAndStoreUserFile(content, fileName, mediaType, category, entityId, createUserId);

        assertEquals(newUserFile, returned);

        verify(
                gcsService, times(1)
        ).storeDocument(eq(content), eq(mediaType), any());

        verify(
                userFileDAO, times(1)
        ).insertNewFile(
                eq(fileName),
                eq(category.getValue()),
                eq(bucketName),
                eq(blobName),
                eq(mediaType),
                eq(entityId),
                eq(createUserId),
                any());

    }

    @Test
    public void testFetchById() throws IOException {
        String bucketName = RandomStringUtils.randomAlphabetic(10);
        String blobName = RandomStringUtils.randomAlphabetic(10);

        UserFile file = new UserFile();
        file.setBucketName(bucketName);
        file.setBlobName(blobName);

        String content = RandomStringUtils.randomAlphabetic(10);

        when(
                gcsService.getDocument(BlobId.of(file.getBucketName(), file.getBlobName()))
        ).thenReturn(new ByteArrayInputStream(content.getBytes()));

        when(
                userFileDAO.findFileById(10)
        ).thenReturn(file);

        initService();

        UserFile returned = service.fetchById(10);

        assertEquals(file, returned);

        assertArrayEquals(content.getBytes(), returned.getUploadedFile().readAllBytes());
    }

    @Test
    public void testFetchAllByEntityId() throws IOException {
        String bucket1Name = RandomStringUtils.randomAlphabetic(10);
        String blob1Name = RandomStringUtils.randomAlphabetic(10);
        String bucket2Name = RandomStringUtils.randomAlphabetic(10);
        String blob2Name = RandomStringUtils.randomAlphabetic(10);
        String bucket3Name = RandomStringUtils.randomAlphabetic(10);
        String blob3Name = RandomStringUtils.randomAlphabetic(10);

        UserFile file1 = new UserFile();
        file1.setBucketName(bucket1Name);
        file1.setBlobName(blob1Name);

        UserFile file2 = new UserFile();
        file2.setBucketName(bucket2Name);
        file2.setBlobName(blob2Name);

        UserFile file3 = new UserFile();
        file3.setBucketName(bucket3Name);
        file3.setBlobName(blob3Name);

        String content1 = RandomStringUtils.randomAlphabetic(10);
        String content2 = RandomStringUtils.randomAlphabetic(10);
        String content3 = RandomStringUtils.randomAlphabetic(10);

        when(
                gcsService.getDocument(BlobId.of(file1.getBucketName(), file1.getBlobName()))
        ).thenReturn(new ByteArrayInputStream(content1.getBytes()));
        when(
                gcsService.getDocument(BlobId.of(file2.getBucketName(), file2.getBlobName()))
        ).thenReturn(new ByteArrayInputStream(content2.getBytes()));
        when(
                gcsService.getDocument(BlobId.of(file3.getBucketName(), file3.getBlobName()))
        ).thenReturn(new ByteArrayInputStream(content3.getBytes()));

        String entityId = RandomStringUtils.randomAlphabetic(10);

        when(
                userFileDAO.findFilesByEntityId(entityId)
        ).thenReturn(List.of(file1, file2, file3));

        initService();

        List<UserFile> returned = service.fetchAllByEntityId(entityId);

        assertEquals(3, returned.size());

        assertEquals(file1, returned.get(0));
        assertEquals(file2, returned.get(1));
        assertEquals(file3, returned.get(2));

        assertArrayEquals(content1.getBytes(), returned.get(0).getUploadedFile().readAllBytes());
        assertArrayEquals(content2.getBytes(), returned.get(1).getUploadedFile().readAllBytes());
        assertArrayEquals(content3.getBytes(), returned.get(2).getUploadedFile().readAllBytes());
    }

    @Test
    public void testFetchAllByEntityIdAndCategory() throws IOException {
        String bucket1Name = RandomStringUtils.randomAlphabetic(10);
        String blob1Name = RandomStringUtils.randomAlphabetic(10);
        String bucket2Name = RandomStringUtils.randomAlphabetic(10);
        String blob2Name = RandomStringUtils.randomAlphabetic(10);
        String bucket3Name = RandomStringUtils.randomAlphabetic(10);
        String blob3Name = RandomStringUtils.randomAlphabetic(10);

        UserFile file1 = new UserFile();
        file1.setBucketName(bucket1Name);
        file1.setBlobName(blob1Name);

        UserFile file2 = new UserFile();
        file2.setBucketName(bucket2Name);
        file2.setBlobName(blob2Name);

        UserFile file3 = new UserFile();
        file3.setBucketName(bucket3Name);
        file3.setBlobName(blob3Name);

        String content1 = RandomStringUtils.randomAlphabetic(10);
        String content2 = RandomStringUtils.randomAlphabetic(10);
        String content3 = RandomStringUtils.randomAlphabetic(10);

        when(
                gcsService.getDocument(BlobId.of(file1.getBucketName(), file1.getBlobName()))
        ).thenReturn(new ByteArrayInputStream(content1.getBytes()));
        when(
                gcsService.getDocument(BlobId.of(file2.getBucketName(), file2.getBlobName()))
        ).thenReturn(new ByteArrayInputStream(content2.getBytes()));
        when(
                gcsService.getDocument(BlobId.of(file3.getBucketName(), file3.getBlobName()))
        ).thenReturn(new ByteArrayInputStream(content3.getBytes()));

        String entityId = RandomStringUtils.randomAlphabetic(10);
        UserFileCategory category = List.of(UserFileCategory.values()).get(new Random().nextInt(UserFileCategory.values().length));

        when(
                userFileDAO.findFilesByEntityIdAndCategory(entityId, category.getValue())
        ).thenReturn(List.of(file1, file2, file3));

        initService();

        List<UserFile> returned = service.fetchAllByEntityIdAndCategory(entityId, category);

        assertEquals(3, returned.size());

        assertEquals(file1, returned.get(0));
        assertEquals(file2, returned.get(1));
        assertEquals(file3, returned.get(2));

        assertArrayEquals(content1.getBytes(), returned.get(0).getUploadedFile().readAllBytes());
        assertArrayEquals(content2.getBytes(), returned.get(1).getUploadedFile().readAllBytes());
        assertArrayEquals(content3.getBytes(), returned.get(2).getUploadedFile().readAllBytes());
    }

}
