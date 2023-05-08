package org.broadinstitute.consent.http.resources;

import com.google.api.client.http.HttpStatusCodes;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.enumeration.DarStatus;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.DarCollection;
import org.broadinstitute.consent.http.models.DarCollectionSummary;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.PaginationResponse;
import org.broadinstitute.consent.http.models.PaginationToken;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.service.DarCollectionService;
import org.broadinstitute.consent.http.service.DataAccessRequestService;
import org.broadinstitute.consent.http.service.DatasetService;
import org.broadinstitute.consent.http.service.UserService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class DarCollectionResourceTest {
    private final AuthUser authUser = new AuthUser("test@test.com");
    private final List<UserRole> researcherRole = new ArrayList(List.of(
            new UserRole(UserRoles.RESEARCHER.getRoleId(), UserRoles.RESEARCHER.getRoleName())
    ));
    private final User researcher = new User(1, authUser.getEmail(), "Display Name", new Date(), researcherRole);
    private final List<UserRole> signingOfficialRole = List.of(
            new UserRole(UserRoles.SIGNINGOFFICIAL.getRoleId(), UserRoles.SIGNINGOFFICIAL.getRoleName()));
    private final User signingOfficial = new User(4, authUser.getEmail(), "Display Name", new Date(), signingOfficialRole);

    private DarCollectionResource resource;

    @Mock
    private DataAccessRequestService dataAccessRequestService;
    @Mock
    private DatasetService datasetService;
    @Mock
    private DarCollectionService darCollectionService;
    @Mock
    private UserService userService;

    private void initResource() {
        resource = new DarCollectionResource(dataAccessRequestService, darCollectionService, userService);
    }

    private DataAccessRequest mockDataAccessRequestWithDatasetIds() {
        DataAccessRequest dar = new DataAccessRequest();
        DataAccessRequestData data = new DataAccessRequestData();
        dar.addDatasetId(RandomUtils.nextInt(1, 100));
        dar.setData(data);
        return dar;
    }

    private DarCollection mockDarCollection() {
        DarCollection collection = new DarCollection();
        for (int i = 0; i < 3; i++) {
            collection.addDar(mockDataAccessRequestWithDatasetIds());
        }
        return collection;
    }

    private Set<Dataset> mockDatasetsForResearcherCollection() {
        Set<Dataset> datasets = new HashSet<>();
        for (int i = 1; i < 3; i++) {
            Dataset newDataset = new Dataset();
            newDataset.setDataSetId(i);
            datasets.add(newDataset);
        }
        return datasets;
    }

    @Before
    public void setUp() {
        openMocks(this);
    }

    @Test
    public void testGetCollectionsForResearcher() {
        List<DarCollection> mockCollectionsList = new ArrayList<>();
        mockCollectionsList.add(mockDarCollection());
        mockCollectionsList.add(mockDarCollection());
        when(userService.findUserByEmail(anyString())).thenReturn(researcher);
        when(darCollectionService.getCollectionsForUser(any(User.class))).thenReturn(mockCollectionsList);
        initResource();

        Response response = resource.getCollectionsForResearcher(authUser);
        assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }

    @Test
    public void testGetCollectionsForUserByRoleAdmin() {
        List<DarCollection> mockCollectionsList = List.of(mockDarCollection());
        UserRole adminRole = new UserRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName());
        User admin = new User(1, authUser.getEmail(), "Display Name", new Date(), List.of(adminRole));

        when(userService.findUserByEmail(anyString())).thenReturn(admin);
        when(darCollectionService.getAllCollections()).thenReturn(mockCollectionsList);
        initResource();

        Response response = resource.getCollectionsForUserByRole(authUser, UserRoles.ADMIN.getRoleName());
        assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }

    @Test
    public void testGetCollectionsForUserByRoleAdminWithoutProperRole() {
        // Test that a user who has access cannot access as a role they do not have.
        List<DarCollection> mockCollectionsList = List.of(mockDarCollection());
        UserRole adminRole = new UserRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName());
        User admin = new User(1, authUser.getEmail(), "Display Name", new Date(), List.of(adminRole));

        when(userService.findUserByEmail(anyString())).thenReturn(admin);
        when(darCollectionService.getAllCollections()).thenReturn(mockCollectionsList);
        initResource();

        Response response = resource.getCollectionsForUserByRole(authUser, UserRoles.SIGNINGOFFICIAL.getRoleName());
        assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }

    @Test
    public void testGetCollectionsForUserByRoleAdminWithSORole() {
        // Test that a user who has access can access as a different role they have.
        List<DarCollection> mockCollectionsList = List.of(mockDarCollection());
        UserRole adminRole = new UserRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName());
        UserRole soRole = new UserRole(UserRoles.SIGNINGOFFICIAL.getRoleId(), UserRoles.SIGNINGOFFICIAL.getRoleName());
        User admin = new User(1, authUser.getEmail(), "Display Name", new Date(), List.of(adminRole, soRole));

        when(userService.findUserByEmail(anyString())).thenReturn(admin);
        when(darCollectionService.getAllCollections()).thenReturn(mockCollectionsList);
        initResource();

        Response response = resource.getCollectionsForUserByRole(authUser, UserRoles.SIGNINGOFFICIAL.getRoleName());
        assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }

    @Test
    public void testGetCollectionByIdResearcher() {
        DarCollection collection = mockDarCollection();
        collection.setCreateUser(researcher);
        collection.setCreateUserId(researcher.getUserId());
        when(userService.findUserByEmail(anyString())).thenReturn(researcher);
        when(darCollectionService.getByCollectionId(any())).thenReturn(collection);
        initResource();

        Response response = resource.getCollectionById(authUser, 1);
        assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }

    @Test
    public void testGetCollectionByIdResearcherNotFound() {
        DarCollection collection = mockDarCollection();
        collection.setCreateUser(researcher);
        collection.setCreateUserId(researcher.getUserId() + 1);
        when(userService.findUserByEmail(anyString())).thenReturn(researcher);
        when(darCollectionService.getByCollectionId(any())).thenReturn(collection);
        initResource();

        Response response = resource.getCollectionById(authUser, 1);
        assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
    }

    @Test
    public void testGetCollectionByIdAdmin() {
        DarCollection collection = mockDarCollection();
        UserRole adminRole = new UserRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName());
        User admin = new User(1, authUser.getEmail(), "Display Name", new Date(), List.of(adminRole));
        collection.setCreateUser(researcher);
        collection.setCreateUserId(researcher.getUserId());

        when(darCollectionService.getByCollectionId(any())).thenReturn(collection);
        when(userService.findUserByEmail(anyString())).thenReturn(admin);
        initResource();

        Response response = resource.getCollectionById(authUser, collection.getDarCollectionId());
        assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }

    @Test
    public void testGetCollectionByIdSO() {
        DarCollection collection = mockDarCollection();
        signingOfficial.setInstitutionId(1);
        researcher.setInstitutionId(1);
        collection.setCreateUser(researcher);
        collection.setCreateUserId(researcher.getUserId());

        when(darCollectionService.getByCollectionId(any())).thenReturn(collection);
        when(userService.findUserByEmail(anyString())).thenReturn(signingOfficial);
        initResource();

        Response response = resource.getCollectionById(authUser, collection.getDarCollectionId());
        assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }

    @Test
    public void testGetCollectionByIdSOWrongInstitution() {
        DarCollection collection = mockDarCollection();
        signingOfficial.setInstitutionId(2);
        researcher.setInstitutionId(1);
        collection.setCreateUser(researcher);
        collection.setCreateUserId(researcher.getUserId());

        when(darCollectionService.getByCollectionId(any())).thenReturn(collection);
        when(userService.findUserByEmail(anyString())).thenReturn(signingOfficial);
        initResource();

        Response response = resource.getCollectionById(authUser, collection.getDarCollectionId());
        assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
    }

    @Test
    public void testGetCollectionByIdNoInstitution() {
        DarCollection collection = mockDarCollection();
        researcher.setInstitutionId(1);
        collection.setCreateUser(researcher);
        collection.setCreateUserId(researcher.getUserId());

        when(darCollectionService.getByCollectionId(any())).thenReturn(collection);
        when(userService.findUserByEmail(anyString())).thenReturn(signingOfficial);
        initResource();

        Response response = resource.getCollectionById(authUser, collection.getDarCollectionId());
        assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
    }

    @Test
    public void testGetCollectionByIdSONoInstitution() {
        DarCollection collection = mockDarCollection();
        signingOfficial.setInstitutionId(1);
        collection.setCreateUser(researcher);
        collection.setCreateUserId(researcher.getUserId());

        when(darCollectionService.getByCollectionId(any())).thenReturn(collection);
        when(userService.findUserByEmail(anyString())).thenReturn(signingOfficial);
        initResource();

        Response response = resource.getCollectionById(authUser, collection.getDarCollectionId());
        assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
    }

    @Test
    public void testGetCollectionByIdChair() {
        List<UserRole> chairRole = List.of(
                new UserRole(UserRoles.CHAIRPERSON.getRoleId(), UserRoles.CHAIRPERSON.getRoleName()));
        User chair = new User(3, authUser.getEmail(), "Display Name", new Date(), chairRole);
        DarCollection collection = mockDarCollection();
        collection.setCreateUser(researcher);
        collection.setCreateUserId(researcher.getUserId());

        Dataset dataSet = new Dataset();
        dataSet.setDataSetId(2);
        collection.addDataset(dataSet);


        when(darCollectionService.getByCollectionId(any())).thenReturn(collection);
        when(userService.findUserByEmail(anyString())).thenReturn(chair);
        when(darCollectionService.findDatasetIdsByUser(any())).thenReturn(Arrays.asList(1, 2));
        initResource();

        Response response = resource.getCollectionById(authUser, collection.getDarCollectionId());
        assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }

    @Test
    public void testGetCollectionByIdDacMember() {
        List<UserRole> chairRole = List.of(
                new UserRole(UserRoles.MEMBER.getRoleId(), UserRoles.MEMBER.getRoleName()));
        User chair = new User(3, authUser.getEmail(), "Display Name", new Date(), chairRole);
        DarCollection collection = mockDarCollection();
        collection.setCreateUser(researcher);
        collection.setCreateUserId(researcher.getUserId());

        Dataset dataSet = new Dataset();
        dataSet.setDataSetId(2);
        collection.addDataset(dataSet);


        when(darCollectionService.getByCollectionId(any())).thenReturn(collection);
        when(userService.findUserByEmail(anyString())).thenReturn(chair);
        when(darCollectionService.findDatasetIdsByUser(any())).thenReturn(Arrays.asList(1, 2));
        initResource();

        Response response = resource.getCollectionById(authUser, collection.getDarCollectionId());
        assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }

    @Test
    public void testGetCollectionByIdDacMemberNoDatasetIdMatch() {
        List<UserRole> chairRole = List.of(
                new UserRole(UserRoles.CHAIRPERSON.getRoleId(), UserRoles.CHAIRPERSON.getRoleName()));
        User chair = new User(3, authUser.getEmail(), "Display Name", new Date(), chairRole);
        DarCollection collection = mockDarCollection();
        collection.setCreateUser(researcher);
        collection.setCreateUserId(researcher.getUserId());

        Dataset dataSet = new Dataset();
        dataSet.setDataSetId(3);
        collection.addDataset(dataSet);


        when(darCollectionService.getByCollectionId(any())).thenReturn(collection);
        when(userService.findUserByEmail(anyString())).thenReturn(chair);
        when(darCollectionService.findDatasetIdsByUser(any())).thenReturn(Arrays.asList(1, 2));
        initResource();

        Response response = resource.getCollectionById(authUser, collection.getDarCollectionId());
        assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
    }

    @Test
    public void testGetCollectionByIdMultipleRoles() {
        UserRole chairRole = new UserRole(UserRoles.CHAIRPERSON.getRoleId(), UserRoles.CHAIRPERSON.getRoleName());
        researcher.addRole(chairRole);
        DarCollection collection = mockDarCollection();
        collection.setCreateUser(researcher);
        collection.setCreateUserId(researcher.getUserId());

        Dataset dataSet = new Dataset();
        dataSet.setDataSetId(3);
        collection.addDataset(dataSet);


        when(darCollectionService.getByCollectionId(any())).thenReturn(collection);
        when(userService.findUserByEmail(anyString())).thenReturn(researcher);
        when(darCollectionService.findDatasetIdsByUser(any())).thenReturn(Arrays.asList(1, 2));
        initResource();

        Response response = resource.getCollectionById(authUser, collection.getDarCollectionId());
        assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }


    @Test
    public void testGetCollectionByReferenceId() {
        DarCollection collection = mockDarCollection();
        collection.setCreateUserId(researcher.getUserId());
        when(userService.findUserByEmail(anyString())).thenReturn(researcher);
        when(darCollectionService.getByReferenceId(any())).thenReturn(collection);
        initResource();

        Response response = resource.getCollectionByReferenceId(authUser, "1");
        assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }

    @Test
    public void testGetCollectionByReferenceIdNotFound() {
        DarCollection collection = mockDarCollection();
        collection.setCreateUserId(researcher.getUserId() + 1);
        when(userService.findUserByEmail(anyString())).thenReturn(researcher);
        when(darCollectionService.getByReferenceId(any())).thenReturn(collection);
        initResource();

        Response response = resource.getCollectionByReferenceId(authUser, "1");
        assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
    }

    @Test
    public void testCancelDarCollection_OKStatus() {
        DarCollection collection = mockDarCollection();
        collection.setCreateUserId(researcher.getUserId());
        when(userService.findUserByEmail(anyString())).thenReturn(researcher);
        when(darCollectionService.getByReferenceId(any())).thenReturn(collection);
        initResource();

        Response response = resource.getCollectionByReferenceId(authUser, "1");
        assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }

    @Test
    public void testCancelDarCollection_NotFoundStatus() {
        DarCollection collection = mockDarCollection();
        collection.setCreateUserId(researcher.getUserId());
        when(userService.findUserByEmail(anyString())).thenReturn(researcher);
        when(darCollectionService.getByCollectionId(anyInt())).thenReturn(null);
        initResource();

        Response response = resource.cancelDarCollectionByCollectionId(authUser, 1, null);
        assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
    }

    @Test
    public void testCancelDarCollection_BadRequestStatus() {
        DarCollection collection = mockDarCollection();
        collection.setCreateUserId(researcher.getUserId());
        when(userService.findUserByEmail(anyString())).thenReturn(researcher);
        when(darCollectionService.getByCollectionId(anyInt())).thenReturn(collection);
        when(darCollectionService.cancelDarCollectionAsResearcher(any(DarCollection.class))).thenThrow(new BadRequestException());
        initResource();

        Response response = resource.cancelDarCollectionByCollectionId(authUser, 1, null);
        assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }

    @Test
    public void testQueryCollectionsByFiltersAndUserRoles() {
        PaginationResponse<DarCollection> paginationResponse = new PaginationResponse<>();
        when(userService.findUserByEmail(anyString())).thenReturn(researcher);
        when(darCollectionService.queryCollectionsByFiltersAndUserRoles(any(User.class), any(PaginationToken.class), anyString()))
                .thenReturn(paginationResponse);
        initResource();

        Response response = resource.queryCollectionsByFiltersAndUserRoles(authUser, "researcher", "DESC", "filterTerm", "projectTitle", 10);
        assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }

    @Test
    public void testCancelDarCollection_InternalErrorStatus() {
        DarCollection collection = mockDarCollection();
        collection.setCreateUserId(researcher.getUserId());
        when(userService.findUserByEmail(anyString())).thenReturn(researcher);
        when(darCollectionService.getByCollectionId(anyInt())).thenReturn(collection);
        when(darCollectionService.cancelDarCollectionAsResearcher(any(DarCollection.class)))
                .thenThrow(new InternalServerErrorException());
        initResource();

        Response response = resource.cancelDarCollectionByCollectionId(authUser, 1, null);
        assertEquals(HttpStatusCodes.STATUS_CODE_SERVER_ERROR, response.getStatus());
    }

    @Test
    public void testCancelDarCollection_asAdmin() {
        List<UserRole> adminRole = List.of(new UserRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName()));
        User admin = new User(1, authUser.getEmail(), "Display Name", new Date(), adminRole);

        DarCollection collection = mockDarCollection();
        collection.setCreateUserId(admin.getUserId());
        when(userService.findUserByEmail(anyString())).thenReturn(admin);
        when(darCollectionService.getByCollectionId(anyInt())).thenReturn(collection);
        initResource();

        Response response = resource.cancelDarCollectionByCollectionId(authUser, 1, Resource.ADMIN);
        assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }

    @Test
    public void testCancelDarCollection_asChair() {
        List<UserRole> chairRole = List.of(new UserRole(UserRoles.CHAIRPERSON.getRoleId(), UserRoles.CHAIRPERSON.getRoleName()));
        User chair = new User(1, authUser.getEmail(), "Display Name", new Date(), chairRole);

        DarCollection collection = mockDarCollection();
        collection.setCreateUserId(chair.getUserId());
        when(userService.findUserByEmail(anyString())).thenReturn(chair);
        when(darCollectionService.getByCollectionId(anyInt())).thenReturn(collection);
        initResource();

        Response response = resource.cancelDarCollectionByCollectionId(authUser, 1, Resource.CHAIRPERSON);
        assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }

    @Test
    public void testCancelDarCollection_asChairAsAdmin() {
        List<UserRole> chairRole = List.of(new UserRole(UserRoles.CHAIRPERSON.getRoleId(), UserRoles.CHAIRPERSON.getRoleName()));
        User chair = new User(1, authUser.getEmail(), "Display Name", new Date(), chairRole);

        DarCollection collection = mockDarCollection();
        collection.setCreateUserId(chair.getUserId());
        when(userService.findUserByEmail(anyString())).thenReturn(chair);
        when(darCollectionService.getByCollectionId(anyInt())).thenReturn(collection);
        initResource();

        Response response = resource.cancelDarCollectionByCollectionId(authUser, 1, Resource.ADMIN);
        assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }

    @Test
    public void testCancelDarCollection_asResearcher() {
        DarCollection collection = mockDarCollection();
        collection.setCreateUserId(researcher.getUserId());
        when(userService.findUserByEmail(anyString())).thenReturn(researcher);
        when(darCollectionService.getByCollectionId(anyInt())).thenReturn(collection);
        initResource();

        Response response = resource.cancelDarCollectionByCollectionId(authUser, 1, Resource.RESEARCHER);
        assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }

    @Test
    public void testCancelDarCollection_asResearcherAsAdmin() {
        DarCollection collection = mockDarCollection();
        collection.setCreateUserId(researcher.getUserId());
        when(userService.findUserByEmail(anyString())).thenReturn(researcher);
        when(darCollectionService.getByCollectionId(anyInt())).thenReturn(collection);
        initResource();

        Response response = resource.cancelDarCollectionByCollectionId(authUser, 1, Resource.ADMIN);
        assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }

    @Test
    public void testGetCollectionsByToken_InvalidToken() {
        when(userService.findUserByEmail(anyString())).thenReturn(researcher);
        initResource();
        Response response = resource.getCollectionsByToken(authUser, "badTokenString", "researcher");
        assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }

    @Test
    public void testGetCollectionsByToken_NullToken() {
        when(userService.findUserByEmail(anyString())).thenReturn(researcher);
        initResource();
        Response response = resource.getCollectionsByToken(authUser, null, "researcher");
        assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }

    @Test
    public void testGetCollectionsByToken_EmptyStringToken() {
        when(userService.findUserByEmail(anyString())).thenReturn(researcher);
        initResource();
        Response response = resource.getCollectionsByToken(authUser, "", "researcher");
        assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }

    @Test
    public void testGetCollectionsByToken() {
        PaginationResponse<DarCollection> paginationResponse = new PaginationResponse<>();
        when(userService.findUserByEmail(anyString())).thenReturn(researcher);
        when(darCollectionService.queryCollectionsByFiltersAndUserRoles(any(User.class), any(PaginationToken.class), anyString()))
                .thenReturn(paginationResponse);
        initResource();

        String token = "eyJwYWdlIjoyLCJwYWdlU2l6ZSI6MSwic29ydEZpZWxkIjoiZGFyX2NvZGUiLCJzb3J0RGlyZWN0aW9uIjoiREVTQyIsImZpbHRlcmVkQ291bnQiOjQsImZpbHRlcmVkUGFnZUNvdW50Ijo0LCJ1bmZpbHRlcmVkQ291bnQiOjQsImFjY2VwdGFibGVTb3J0RmllbGRzIjp7InByb2plY3RUaXRsZSI6InByb2plY3RUaXRsZSIsInJlc2VhcmNoZXIiOiJyZXNlYXJjaGVyIiwiaW5zdGl0dXRpb24iOiJpbnN0aXR1dGlvbl9uYW1lIiwiZGFyQ29kZSI6ImRhcl9jb2RlIn19";
        Response response = resource.getCollectionsByToken(authUser, token, "researcher");
        assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }

    @Test
    public void testResubmitDarCollection_CollectionNotFound() {
        User user = mock(User.class);
        when(userService.findUserByEmail(anyString())).thenReturn(user);
        when(darCollectionService.getByCollectionId(any())).thenReturn(null);
        initResource();

        Response response = resource.resubmitDarCollection(authUser, 1);
        assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
    }

    @Test
    public void testResubmitDarCollection_UserNotCreator() {
        User user = mock(User.class);
        when(userService.findUserByEmail(anyString())).thenReturn(user);
        when(darCollectionService.getByCollectionId(any())).thenReturn(new DarCollection());
        initResource();

        Response response = resource.resubmitDarCollection(authUser, 1);
        assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
    }

    @Test
    public void testResubmitDarCollection_CollectionNotCanceled() {
        int userId = 1;
        User user = mock(User.class);
        when(userService.findUserByEmail(anyString())).thenReturn(user);
        when(user.getUserId()).thenReturn(userId);
        DarCollection collection = mock(DarCollection.class);
        when(collection.getCreateUserId()).thenReturn(userId);
        DataAccessRequest dar = mock(DataAccessRequest.class);
        DataAccessRequestData data = mock(DataAccessRequestData.class);
        String referenceId = UUID.randomUUID().toString();
        when(data.getStatus()).thenReturn("Not Canceled");
        when(dar.getData()).thenReturn(data);
        when(dar.getReferenceId()).thenReturn(referenceId);
        Map<String, DataAccessRequest> darMap = Map.of(dar.getReferenceId(), dar);
        when(collection.getDars()).thenReturn(darMap);
        when(darCollectionService.getByCollectionId(any())).thenReturn(collection);
        initResource();

        Response response = resource.resubmitDarCollection(authUser, 1);
        assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }

    @Test
    public void testResubmitDarCollection_Success() {
        int userId = 1;
        User user = mock(User.class);
        when(userService.findUserByEmail(anyString())).thenReturn(user);
        when(user.getUserId()).thenReturn(userId);
        DarCollection collection = mock(DarCollection.class);
        when(collection.getCreateUserId()).thenReturn(userId);
        DataAccessRequest dar = mock(DataAccessRequest.class);
        DataAccessRequestData data = mock(DataAccessRequestData.class);
        String referenceId = UUID.randomUUID().toString();
        when(data.getStatus()).thenReturn(DarStatus.CANCELED.getValue());
        when(dar.getData()).thenReturn(data);
        when(dar.getReferenceId()).thenReturn(referenceId);
        Map<String, DataAccessRequest> darMap = Map.of(dar.getReferenceId(), dar);
        when(collection.getDars()).thenReturn(darMap);
        when(darCollectionService.getByCollectionId(any())).thenReturn(collection);
        when(dataAccessRequestService.createDraftDarFromCanceledCollection(any(), any())).thenReturn(new DataAccessRequest());
        initResource();

        Response response = resource.resubmitDarCollection(authUser, 1);
        assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }

    @Test
    public void testCreateElectionsForCollection() {
        User user = mock(User.class);
        when(userService.findUserByEmail(anyString())).thenReturn(user);
        DarCollection collection = mock(DarCollection.class);
        when(darCollectionService.getByCollectionId(any())).thenReturn(collection);
        initResource();

        Response response = resource.createElectionsForCollection(authUser, 1);
        assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }

    @Test
    public void testCreateElectionsForCollectionNotFound() {
        User user = mock(User.class);
        when(userService.findUserByEmail(anyString())).thenReturn(user);
        when(darCollectionService.getByCollectionId(any())).thenReturn(null);
        initResource();

        Response response = resource.createElectionsForCollection(authUser, 1);
        assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
    }

    @Test
    public void getCollectionSummariesForUserByRole_Member() {
        User user = new User();
        UserRole userRole = new UserRole(UserRoles.MEMBER.getRoleId(), UserRoles.MEMBER.getRoleName());
        user.addRole(userRole);
        DarCollectionSummary mockSummary = new DarCollectionSummary();
        when(userService.findUserByEmail(anyString())).thenReturn(user);
        when(darCollectionService.getSummariesForRoleName(any(User.class), anyString()))
                .thenReturn(List.of(mockSummary));
        initResource();

        Response response = resource.getCollectionSummariesForUserByRole(authUser, UserRoles.MEMBER.getRoleName());
        assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }

    @Test
    public void getCollectionSummariesForUserByRole_Chair() {
        User user = new User();
        UserRole userRole = new UserRole(UserRoles.CHAIRPERSON.getRoleId(), UserRoles.CHAIRPERSON.getRoleName());
        user.addRole(userRole);
        DarCollectionSummary mockSummary = new DarCollectionSummary();
        when(userService.findUserByEmail(anyString())).thenReturn(user);
        when(darCollectionService.getSummariesForRoleName(any(User.class), anyString()))
                .thenReturn(List.of(mockSummary));
        initResource();

        Response response = resource.getCollectionSummariesForUserByRole(authUser, UserRoles.CHAIRPERSON.getRoleName());
        assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }

    @Test
    public void getCollectionSummariesForUserByRole_SO() {
        User user = new User();
        UserRole userRole = new UserRole(UserRoles.SIGNINGOFFICIAL.getRoleId(), UserRoles.SIGNINGOFFICIAL.getRoleName());
        user.addRole(userRole);
        DarCollectionSummary mockSummary = new DarCollectionSummary();
        when(userService.findUserByEmail(anyString())).thenReturn(user);
        when(darCollectionService.getSummariesForRoleName(any(User.class), anyString()))
                .thenReturn(List.of(mockSummary));
        initResource();

        Response response = resource.getCollectionSummariesForUserByRole(authUser, UserRoles.SIGNINGOFFICIAL.getRoleName());
        assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }

    @Test
    public void getCollectionSummariesForUserByRole_Researcher() {
        User user = new User();
        UserRole userRole = new UserRole(UserRoles.RESEARCHER.getRoleId(), UserRoles.RESEARCHER.getRoleName());
        user.addRole(userRole);
        DarCollectionSummary mockSummary = new DarCollectionSummary();
        when(userService.findUserByEmail(anyString())).thenReturn(user);
        when(darCollectionService.getSummariesForRoleName(any(User.class), anyString()))
                .thenReturn(List.of(mockSummary));
        initResource();

        Response response = resource.getCollectionSummariesForUserByRole(authUser, UserRoles.RESEARCHER.getRoleName());
        assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }

    @Test
    public void getCollectionSummariesForUserByRole_Admin() {
        User user = new User();
        UserRole userRole = new UserRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName());
        user.addRole(userRole);
        DarCollectionSummary mockSummary = new DarCollectionSummary();
        when(userService.findUserByEmail(anyString())).thenReturn(user);
        when(darCollectionService.getSummariesForRoleName(any(User.class), anyString()))
                .thenReturn(List.of(mockSummary));
        initResource();

        Response response = resource.getCollectionSummariesForUserByRole(authUser, UserRoles.ADMIN.getRoleName());
        assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }

    @Test
    public void getCollectionSummariesForUserByRole_NoRoleFound() {
        User user = new User();
        DarCollectionSummary mockSummary = new DarCollectionSummary();
        when(userService.findUserByEmail(anyString())).thenReturn(user);
        when(darCollectionService.getSummariesForRoleName(any(User.class), anyString()))
                .thenReturn(List.of(mockSummary));
        initResource();

        Response response = resource.getCollectionSummariesForUserByRole(authUser, UserRoles.SIGNINGOFFICIAL.getRoleName());
        assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }

    @Test
    public void getCollectionSummariesForUserByRole_InvalidRoleString() {
        User user = new User();
        DarCollectionSummary mockSummary = new DarCollectionSummary();
        when(userService.findUserByEmail(anyString())).thenReturn(user);
        when(darCollectionService.getSummariesForRoleName(any(User.class), anyString()))
                .thenReturn(List.of(mockSummary));
        initResource();

        Response response = resource.getCollectionSummariesForUserByRole(authUser, "invalid");
        assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }


    @Test
    public void getCollectionSummaryForRoleById_Member() {
        User user = new User();
        UserRole userRole = new UserRole(UserRoles.MEMBER.getRoleId(), UserRoles.MEMBER.getRoleName());
        user.addRole(userRole);

        DarCollectionSummary mockSummary = new DarCollectionSummary();
        mockSummary.setDatasetIds(Set.of(1));
        Integer collectionId = RandomUtils.nextInt(1, 100);

        when(userService.findUserByEmail(anyString())).thenReturn(user);
        when(darCollectionService.findDatasetIdsByUser(user)).thenReturn(List.of(1, 2));
        when(darCollectionService.getSummaryForRoleNameByCollectionId(any(User.class), anyString(), anyInt()))
                .thenReturn(mockSummary);
        initResource();

        Response response = resource.getCollectionSummaryForRoleById(authUser, UserRoles.MEMBER.getRoleName(), collectionId);
        assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }

    @Test
    public void getCollectionSummaryForRoleById_MemberNoDatasetsInCollection() {
        User user = new User();
        UserRole userRole = new UserRole(UserRoles.MEMBER.getRoleId(), UserRoles.MEMBER.getRoleName());
        user.addRole(userRole);

        DarCollectionSummary mockSummary = new DarCollectionSummary();
        mockSummary.setDatasetIds(Set.of(1));
        Integer collectionId = RandomUtils.nextInt(1, 100);

        when(userService.findUserByEmail(anyString())).thenReturn(user);
        when(darCollectionService.findDatasetIdsByUser(user)).thenReturn(List.of(2));
        when(darCollectionService.getSummaryForRoleNameByCollectionId(any(User.class), anyString(), anyInt()))
                .thenReturn(mockSummary);
        initResource();

        Response response = resource.getCollectionSummaryForRoleById(authUser, UserRoles.MEMBER.getRoleName(), collectionId);
        assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
    }

    @Test
    public void getCollectionSummaryForRoleById_Chair() {
        User user = new User();
        UserRole userRole = new UserRole(UserRoles.CHAIRPERSON.getRoleId(), UserRoles.CHAIRPERSON.getRoleName());
        user.addRole(userRole);

        DarCollectionSummary mockSummary = new DarCollectionSummary();
        mockSummary.setDatasetIds(Set.of(1));
        Integer collectionId = RandomUtils.nextInt(1, 100);

        when(userService.findUserByEmail(anyString())).thenReturn(user);
        when(darCollectionService.findDatasetIdsByUser(user)).thenReturn(List.of(1, 2));
        when(darCollectionService.getSummaryForRoleNameByCollectionId(any(User.class), anyString(), anyInt()))
                .thenReturn(mockSummary);
        initResource();

        Response response = resource.getCollectionSummaryForRoleById(authUser, UserRoles.CHAIRPERSON.getRoleName(), collectionId);
        assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }

    @Test
    public void getCollectionSummaryForRoleById_ChairNoDatasetsInCollection() {
        User user = new User();
        UserRole userRole = new UserRole(UserRoles.CHAIRPERSON.getRoleId(), UserRoles.CHAIRPERSON.getRoleName());
        user.addRole(userRole);

        DarCollectionSummary mockSummary = new DarCollectionSummary();
        mockSummary.setDatasetIds(Set.of(1));
        Integer collectionId = RandomUtils.nextInt(1, 100);

        when(userService.findUserByEmail(anyString())).thenReturn(user);
        when(darCollectionService.findDatasetIdsByUser(user)).thenReturn(List.of(2));
        when(darCollectionService.getSummaryForRoleNameByCollectionId(any(User.class), anyString(), anyInt()))
                .thenReturn(mockSummary);
        initResource();

        Response response = resource.getCollectionSummaryForRoleById(authUser, UserRoles.CHAIRPERSON.getRoleName(), collectionId);
        assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
    }

    @Test
    public void getCollectionSummaryForRoleById_SO() {
        User user = new User();
        UserRole userRole = new UserRole(UserRoles.SIGNINGOFFICIAL.getRoleId(), UserRoles.SIGNINGOFFICIAL.getRoleName());
        user.addRole(userRole);
        Integer institutionId = RandomUtils.nextInt(1, 100);
        user.setInstitutionId(institutionId);

        DarCollectionSummary mockSummary = new DarCollectionSummary();
        mockSummary.setInstitutionId(institutionId);
        Integer collectionId = RandomUtils.nextInt(1, 100);

        when(userService.findUserByEmail(anyString())).thenReturn(user);
        when(darCollectionService.getSummaryForRoleNameByCollectionId(any(User.class), anyString(), anyInt()))
                .thenReturn(mockSummary);
        initResource();

        Response response = resource.getCollectionSummaryForRoleById(authUser, UserRoles.SIGNINGOFFICIAL.getRoleName(), collectionId);
        assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }

    @Test
    public void getCollectionSummaryForRoleById_SODifferentInstitution() {
        User user = new User();
        UserRole userRole = new UserRole(UserRoles.SIGNINGOFFICIAL.getRoleId(), UserRoles.SIGNINGOFFICIAL.getRoleName());
        user.addRole(userRole);
        user.setInstitutionId(1);

        DarCollectionSummary mockSummary = new DarCollectionSummary();
        mockSummary.setInstitutionId(2);
        Integer collectionId = RandomUtils.nextInt(1, 100);

        when(userService.findUserByEmail(anyString())).thenReturn(user);
        when(darCollectionService.getSummaryForRoleNameByCollectionId(any(User.class), anyString(), anyInt()))
                .thenReturn(mockSummary);
        initResource();

        Response response = resource.getCollectionSummaryForRoleById(authUser, UserRoles.SIGNINGOFFICIAL.getRoleName(), collectionId);
        assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
    }

    @Test
    public void getCollectionSummaryForRoleById_Researcher() {
        User user = new User();
        Integer userId = RandomUtils.nextInt(1, 100);
        user.setUserId(userId);
        UserRole userRole = new UserRole(UserRoles.RESEARCHER.getRoleId(), UserRoles.RESEARCHER.getRoleName());
        user.addRole(userRole);

        DarCollectionSummary mockSummary = new DarCollectionSummary();
        mockSummary.setResearcherId(userId);
        Integer collectionId = RandomUtils.nextInt(1, 100);

        when(userService.findUserByEmail(anyString())).thenReturn(user);
        when(darCollectionService.getSummaryForRoleNameByCollectionId(any(User.class), anyString(), anyInt()))
                .thenReturn(mockSummary);
        initResource();

        Response response = resource.getCollectionSummaryForRoleById(authUser, UserRoles.RESEARCHER.getRoleName(), collectionId);
        assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }

    @Test
    public void getCollectionSummaryForRoleById_ResearcherNotCreateUser() {
        User user = new User();
        user.setUserId(1);
        UserRole userRole = new UserRole(UserRoles.RESEARCHER.getRoleId(), UserRoles.RESEARCHER.getRoleName());
        user.addRole(userRole);

        DarCollectionSummary mockSummary = new DarCollectionSummary();
        mockSummary.setResearcherId(2);
        Integer collectionId = RandomUtils.nextInt(1, 100);

        when(userService.findUserByEmail(anyString())).thenReturn(user);
        when(darCollectionService.getSummaryForRoleNameByCollectionId(any(User.class), anyString(), anyInt()))
                .thenReturn(mockSummary);
        initResource();

        Response response = resource.getCollectionSummaryForRoleById(authUser, UserRoles.RESEARCHER.getRoleName(), collectionId);
        assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
    }

    @Test
    public void getCollectionSummaryForRoleById_Admin() {
        User user = new User();
        UserRole userRole = new UserRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName());
        user.addRole(userRole);
        DarCollectionSummary mockSummary = new DarCollectionSummary();
        Integer collectionId = RandomUtils.nextInt(1, 100);

        when(userService.findUserByEmail(anyString())).thenReturn(user);
        when(darCollectionService.getSummaryForRoleNameByCollectionId(any(User.class), anyString(), anyInt()))
                .thenReturn(mockSummary);
        initResource();

        Response response = resource.getCollectionSummaryForRoleById(authUser, UserRoles.ADMIN.getRoleName(), collectionId);
        assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }

    @Test
    public void getCollectionSummaryForRoleById_NoRoleFound() {
        User user = new User();
        DarCollectionSummary mockSummary = new DarCollectionSummary();
        Integer collectionId = RandomUtils.nextInt(1, 100);

        when(userService.findUserByEmail(anyString())).thenReturn(user);
        when(darCollectionService.getSummaryForRoleNameByCollectionId(any(User.class), anyString(), anyInt()))
                .thenReturn(mockSummary);
        initResource();

        Response response = resource.getCollectionSummaryForRoleById(authUser, UserRoles.SIGNINGOFFICIAL.getRoleName(), collectionId);
        assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }

    @Test
    public void getCollectionSummaryForRoleById_InvalidRoleString() {
        User user = new User();
        DarCollectionSummary mockSummary = new DarCollectionSummary();
        Integer collectionId = RandomUtils.nextInt(1, 100);

        when(userService.findUserByEmail(anyString())).thenReturn(user);
        when(darCollectionService.getSummaryForRoleNameByCollectionId(any(User.class), anyString(), anyInt()))
                .thenReturn(mockSummary);
        initResource();

        Response response = resource.getCollectionSummaryForRoleById(authUser, "invalid", collectionId);
        assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }

    @Test
    public void getCollectionSummaryForRoleById_CollectionNotFound() {
        User user = new User();
        UserRole userRole = new UserRole(UserRoles.RESEARCHER.getRoleId(), UserRoles.RESEARCHER.getRoleName());
        user.addRole(userRole);
        Integer collectionId = RandomUtils.nextInt(1, 100);

        when(userService.findUserByEmail(anyString())).thenReturn(user);
        when(darCollectionService.getSummaryForRoleNameByCollectionId(any(User.class), anyString(), anyInt()))
                .thenThrow(new NotFoundException());
        initResource();

        Response response = resource.getCollectionSummaryForRoleById(authUser, UserRoles.RESEARCHER.getRoleName(), collectionId);
        assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
    }
}
