package org.broadinstitute.consent.http.db;

import org.apache.commons.lang3.RandomStringUtils;
import org.broadinstitute.consent.http.enumeration.DatasetPropertyType;
import org.broadinstitute.consent.http.enumeration.UserFileCategory;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetProperty;
import org.broadinstitute.consent.http.models.Role;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserFile;
import org.broadinstitute.consent.http.models.UserRole;
import org.junit.Assert;
import org.junit.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class UserFileDAOTest extends DAOTestHelper {
    @Test
    public void testInsertFile() {
        String fileName = RandomStringUtils.randomAlphabetic(10);
        String category = UserFileCategory.getValues().get(new Random().nextInt(UserFileCategory.getValues().size()));
        String bucketName = RandomStringUtils.randomAlphabetic(10);
        String blobName = RandomStringUtils.randomAlphabetic(10);
        String mediaType = RandomStringUtils.randomAlphabetic(10);
        String entityId = RandomStringUtils.randomAlphabetic(10);
        Integer createUserId = new Random().nextInt();
        Date createDate = new Date();

        Integer newUserFileId = userFileDAO.insertNewFile(
                fileName,
                category,
                bucketName,
                blobName,
                mediaType,
                entityId,
                createUserId,
                createDate
        );

        UserFile newUserFile = userFileDAO.findFileById(newUserFileId);

        assertNotNull(newUserFile);
        assertNotNull(newUserFile.getUserFileId());
        assertEquals(fileName, newUserFile.getFileName());
        assertEquals(category, newUserFile.getCategory().getValue());
        assertEquals(bucketName, newUserFile.getBucketName());
        assertEquals(blobName, newUserFile.getBlobName());
        assertEquals(mediaType, newUserFile.getMediaType());
        assertEquals(entityId, newUserFile.getEntityId());
        assertEquals(createUserId, newUserFile.getCreateUserId());
        assertEquals(
                createDate.getTime(),
                newUserFile.getCreateDate().getTime(),
                86400000); // delta: within one day (86,400,000 ms)
        assertFalse(newUserFile.getDeleted());
        assertNull(newUserFile.getUploadedFile());
    }

    @Test
    public void testDeleteFileById() {
        UserFile origFile = createUserFile();

        assertFalse(origFile.getDeleted());

        userFileDAO.deleteFileById(origFile.getUserFileId());

        UserFile deletedFile = userFileDAO.findFileById(origFile.getUserFileId());

        assertTrue(deletedFile.getDeleted());
    }

    @Test
    public void testDeleteFileByEntityId() {
        String entityId = RandomStringUtils.randomAlphabetic(10);
        String otherEntityId = RandomStringUtils.randomAlphabetic(8);

        UserFile file1 = createUserFile(entityId, UserFileCategory.IRB_COLLABORATION_LETTER);
        UserFile file2 = createUserFile(entityId, UserFileCategory.DATA_USE_LETTER);
        UserFile file3 = createUserFile(entityId, UserFileCategory.ALTERNATIVE_DATA_SHARING_PLAN);
        UserFile file4 = createUserFile(otherEntityId, UserFileCategory.IRB_COLLABORATION_LETTER);

        assertFalse(file1.getDeleted());
        assertFalse(file2.getDeleted());
        assertFalse(file3.getDeleted());
        assertFalse(file4.getDeleted());

        userFileDAO.deleteFilesByEntityId(entityId);

        file1 = userFileDAO.findFileById(file1.getUserFileId());
        file2 = userFileDAO.findFileById(file2.getUserFileId());
        file3 = userFileDAO.findFileById(file3.getUserFileId());
        file4 = userFileDAO.findFileById(file4.getUserFileId());

        assertTrue(file1.getDeleted());
        assertTrue(file2.getDeleted());
        assertTrue(file3.getDeleted());
        assertFalse(file4.getDeleted()); // should not be effected
    }

    @Test
    public void testFindFilesByEntityId() {
        String entityId = RandomStringUtils.randomAlphabetic(10);

        createUserFile();
        createUserFile(); // random other files
        UserFile file1 = createUserFile(entityId, UserFileCategory.IRB_COLLABORATION_LETTER);
        UserFile file2 = createUserFile(entityId, UserFileCategory.DATA_USE_LETTER);
        UserFile file3 = createUserFile(entityId, UserFileCategory.ALTERNATIVE_DATA_SHARING_PLAN);

        List<UserFile> filesFound = userFileDAO.findFilesByEntityId(entityId);
        List<Integer> fileIdsfound = filesFound.stream().map(UserFile::getUserFileId).toList();

        assertEquals(3, filesFound.size());
        assertTrue(fileIdsfound.contains(file1.getUserFileId()));
        assertTrue(fileIdsfound.contains(file2.getUserFileId()));
        assertTrue(fileIdsfound.contains(file3.getUserFileId()));
    }

    @Test
    public void testFindFilesByEntityIdAndCategory() {
        String entityId = RandomStringUtils.randomAlphabetic(10);

        // different entity id, same category, shouldn't be returned.
        createUserFile("asdf", UserFileCategory.IRB_COLLABORATION_LETTER);
        createUserFile();
        UserFile file1 = createUserFile(entityId, UserFileCategory.IRB_COLLABORATION_LETTER);
        UserFile file2 = createUserFile(entityId, UserFileCategory.IRB_COLLABORATION_LETTER);
        UserFile file3 = createUserFile(entityId, UserFileCategory.ALTERNATIVE_DATA_SHARING_PLAN);

        List<UserFile> irbFiles = userFileDAO.findFilesByEntityIdAndCategory(entityId, UserFileCategory.IRB_COLLABORATION_LETTER.getValue());
        List<UserFile> altDataSharingFiles = userFileDAO.findFilesByEntityIdAndCategory(entityId, UserFileCategory.ALTERNATIVE_DATA_SHARING_PLAN.getValue());

        assertEquals(2, irbFiles.size());
        assertTrue(irbFiles.contains(file1));
        assertTrue(irbFiles.contains(file2));

        assertEquals(1, altDataSharingFiles.size());
        assertTrue(altDataSharingFiles.contains(file3));
    }


}
