package org.broadinstitute.consent.http.service;

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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

        when(userFileDAO.insertNewFile(
                fileName,
                category.getValue(),
                any(),
                any(),
                mediaType,
                entityId,
                createUserId,
                any())).thenReturn(10);

        UserFile newUserFile = new UserFile();
        newUserFile.setFileName(RandomStringUtils.randomAlphabetic(10));

        when(userFileDAO.findFileById(10)).thenReturn(newUserFile);

        spy(userFileDAO);
        spy(gcsService);

        initService();

        UserFile returned = service.uploadAndStoreUserFile(content, fileName, mediaType, category, entityId, createUserId);

        assertEquals(newUserFile, returned);

        verify(
                gcsService, times(1)
        ).storeDocument(content, mediaType, any());

        verify(
                userFileDAO, times(1)
        ).insertNewFile(
                fileName,
                category.getValue(),
                any(),
                any(),
                mediaType,
                entityId,
                createUserId,
                any());

    }
}
