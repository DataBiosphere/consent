package org.broadinstitute.consent.http.resources;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import com.google.api.client.http.HttpStatusCodes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.enumeration.DarStatus;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.DarCollection;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.DataSet;
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

public class DarCollectionResourceTest {
  private final AuthUser authUser = new AuthUser("test@test.com");
  private final List<UserRole> researcherRole = Collections.singletonList(
    new UserRole(UserRoles.RESEARCHER.getRoleId(), UserRoles.RESEARCHER.getRoleName())
  );
  private final User researcher = new User(1, authUser.getEmail(), "Display Name", new Date(), researcherRole, authUser.getEmail());

  private DarCollectionResource resource;

  @Mock private UserService userService;
  @Mock private DatasetService datasetService;
  @Mock private DarCollectionService darCollectionService;
  @Mock private DataAccessRequestService dataAccessRequestService;

  private void initResource() {
    resource = new DarCollectionResource(userService, darCollectionService, dataAccessRequestService);
  }

  private DataAccessRequest mockDataAccessRequestWithDatasetIds() {
    DataAccessRequest dar = new DataAccessRequest();
    DataAccessRequestData data = new DataAccessRequestData();
    data.setDatasetIds(Collections.singletonList(RandomUtils.nextInt(1, 100)));
    dar.setData(data);
    return dar;
  }

  private DarCollection mockDarCollection() {
    DarCollection collection = new DarCollection();
    for(int i = 0; i < 3; i++) {
      collection.addDar(mockDataAccessRequestWithDatasetIds());
    }
    return collection;
  }

  private Set<DataSet> mockDatasetsForResearcherCollection() {
    Set<DataSet> datasets = new HashSet<>();
    for(int i = 1; i < 3; i++) {
      DataSet newDataset = new DataSet();
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
    when(datasetService.getDatasetWithDataUseByIds(any())).thenReturn(mockDatasetsForResearcherCollection());
    initResource();

    Response response = resource.getCollectionsForResearcher(authUser);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  public void testGetCollectionsForUserByRoleAdmin() {
    List<DarCollection> mockCollectionsList = List.of(mockDarCollection());
    UserRole adminRole = new UserRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName());
    User admin = new User(1, authUser.getEmail(), "Display Name", new Date(), List.of(adminRole), authUser.getEmail());

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
    User admin = new User(1, authUser.getEmail(), "Display Name", new Date(), List.of(adminRole), authUser.getEmail());

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
    User admin = new User(1, authUser.getEmail(), "Display Name", new Date(), List.of(adminRole, soRole), authUser.getEmail());

    when(userService.findUserByEmail(anyString())).thenReturn(admin);
    when(darCollectionService.getAllCollections()).thenReturn(mockCollectionsList);
    initResource();

    Response response = resource.getCollectionsForUserByRole(authUser, UserRoles.SIGNINGOFFICIAL.getRoleName());
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  public void testGetCollectionById() {
    DarCollection collection = mockDarCollection();
    collection.setCreateUserId(researcher.getDacUserId());
    when(userService.findUserByEmail(anyString())).thenReturn(researcher);
    when(darCollectionService.getByCollectionId(any())).thenReturn(collection);
    initResource();

    Response response = resource.getCollectionById(authUser, 1);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  public void testGetCollectionByIdNotFound() {
    DarCollection collection = mockDarCollection();
    collection.setCreateUserId(researcher.getDacUserId() + 1);
    when(userService.findUserByEmail(anyString())).thenReturn(researcher);
    when(darCollectionService.getByCollectionId(any())).thenReturn(collection);
    initResource();

    Response response = resource.getCollectionById(authUser, 1);
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  public void testGetCollectionByReferenceId() {
    DarCollection collection = mockDarCollection();
    collection.setCreateUserId(researcher.getDacUserId());
    when(userService.findUserByEmail(anyString())).thenReturn(researcher);
    when(darCollectionService.getByReferenceId(any())).thenReturn(collection);
    initResource();

    Response response = resource.getCollectionByReferenceId(authUser, "1");
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  public void testGetCollectionByReferenceIdNotFound() {
    DarCollection collection = mockDarCollection();
    collection.setCreateUserId(researcher.getDacUserId() + 1);
    when(userService.findUserByEmail(anyString())).thenReturn(researcher);
    when(darCollectionService.getByReferenceId(any())).thenReturn(collection);
    initResource();

    Response response = resource.getCollectionByReferenceId(authUser, "1");
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  public void testGetCollectionsByInitialQuery_BadSortField() {
    when(userService.findUserByEmail(anyString())).thenReturn(researcher);
    initResource();

    Response response = resource.getCollectionsByInitialQuery(authUser, "filterTerm", "badSortFieldName", "ASC", 10);
    assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
  }

  @Test
  public void testGetCollectionsByInitialQuery_BadSortOrder() {
    when(userService.findUserByEmail(anyString())).thenReturn(researcher);
    initResource();

    Response response = resource.getCollectionsByInitialQuery(authUser, "filterTerm", "projectTitle", "badSortOrder", 10);
    assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
  }

  @Test
  public void testCancelDarCollection_OKStatus() {
    DarCollection collection = mockDarCollection();
    collection.setCreateUserId(researcher.getDacUserId());
    when(userService.findUserByEmail(anyString())).thenReturn(researcher);
    when(darCollectionService.getByReferenceId(any())).thenReturn(collection);
    initResource();

    Response response = resource.getCollectionByReferenceId(authUser, "1");
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  public void testCancelDarCollection_NotFoundStatus() {
    DarCollection collection = mockDarCollection();
    collection.setCreateUserId(researcher.getDacUserId());
    when(userService.findUserByEmail(anyString())).thenReturn(researcher);
    when(darCollectionService.getByCollectionId(anyInt())).thenReturn(null);
    initResource();

    Response response = resource.cancelDarCollectionByCollectionId(authUser, 1);
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  public void testCancelDarCollection_BadRequestStatus() {
    DarCollection collection = mockDarCollection();
    collection.setCreateUserId(researcher.getDacUserId());
    when(userService.findUserByEmail(anyString())).thenReturn(researcher);
    when(darCollectionService.getByCollectionId(anyInt())).thenReturn(collection);
    when(darCollectionService.cancelDarCollection(any(DarCollection.class))).thenThrow(new BadRequestException());
    initResource();

    Response response = resource.cancelDarCollectionByCollectionId(authUser, 1);
    assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
  }

  @Test
  public void testGetCollectionsByInitialQuery() {
    PaginationResponse<DarCollection> paginationResponse = new PaginationResponse<>();
    when(userService.findUserByEmail(anyString())).thenReturn(researcher);
    when(darCollectionService.getCollectionsWithFilters(any(PaginationToken.class), any(User.class)))
      .thenReturn(paginationResponse);
    initResource();

    Response response = resource.getCollectionsByInitialQuery(authUser, "filterTerm", "projectTitle", "asc", 10);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  public void testCancelDarCollection_InternalErrorStatus() {
    DarCollection collection = mockDarCollection();
    collection.setCreateUserId(researcher.getDacUserId());
    when(userService.findUserByEmail(anyString())).thenReturn(researcher);
    when(darCollectionService.getByCollectionId(anyInt())).thenReturn(collection);
    when(darCollectionService.cancelDarCollection(any(DarCollection.class)))
        .thenThrow(new InternalServerErrorException());
    initResource();

    Response response = resource.cancelDarCollectionByCollectionId(authUser, 1);
    assertEquals(HttpStatusCodes.STATUS_CODE_SERVER_ERROR, response.getStatus());
  }

  @Test
  public void testGetCollectionsByToken_InvalidToken() {
    initResource();
    Response response = resource.getCollectionsByToken(authUser, "badTokenString");
    assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
  }

  @Test
  public void testGetCollectionsByToken_NullToken() {
    initResource();
    Response response = resource.getCollectionsByToken(authUser, null);
    assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
  }

  @Test
  public void testGetCollectionsByToken_EmptyStringToken() {
    initResource();
    Response response = resource.getCollectionsByToken(authUser, "");
    assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
  }

  @Test
  public void testGetCollectionsByToken() {
    PaginationResponse<DarCollection> paginationResponse = new PaginationResponse<>();
    when(userService.findUserByEmail(anyString())).thenReturn(researcher);
    when(darCollectionService.getCollectionsWithFilters(any(PaginationToken.class), any(User.class)))
      .thenReturn(paginationResponse);
    initResource();

    String token = "eyJwYWdlIjoyLCJwYWdlU2l6ZSI6MSwic29ydEZpZWxkIjoiZGFyX2NvZGUiLCJzb3J0RGlyZWN0aW9uIjoiREVTQyIsImZpbHRlcmVkQ291bnQiOjQsImZpbHRlcmVkUGFnZUNvdW50Ijo0LCJ1bmZpbHRlcmVkQ291bnQiOjQsImFjY2VwdGFibGVTb3J0RmllbGRzIjp7InByb2plY3RUaXRsZSI6InByb2plY3RUaXRsZSIsInJlc2VhcmNoZXIiOiJyZXNlYXJjaGVyIiwiaW5zdGl0dXRpb24iOiJpbnN0aXR1dGlvbl9uYW1lIiwiZGFyQ29kZSI6ImRhcl9jb2RlIn19";
    Response response = resource.getCollectionsByToken(authUser, token);
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
    when(user.getDacUserId()).thenReturn(userId);
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
    when(user.getDacUserId()).thenReturn(userId);
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
}
