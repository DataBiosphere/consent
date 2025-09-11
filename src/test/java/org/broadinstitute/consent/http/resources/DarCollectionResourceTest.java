package org.broadinstitute.consent.http.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.api.client.http.HttpStatusCodes;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.broadinstitute.consent.http.AbstractTestHelper;
import org.broadinstitute.consent.http.enumeration.DarStatus;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.DarCollection;
import org.broadinstitute.consent.http.models.DarCollectionSummary;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.service.DarCollectionService;
import org.broadinstitute.consent.http.service.UserService;
import org.glassfish.jersey.server.ContainerRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DarCollectionResourceTest extends AbstractTestHelper {

  private final AuthUser authUser = new AuthUser("test@test.com");
  private final List<UserRole> researcherRole = List.of(UserRoles.Researcher());
  private final User researcher = new User(1, authUser.getEmail(), "Display Name", new Date(),
      researcherRole);
  private final DuosUser duosResearcher = new DuosUser(authUser, researcher);
  private final List<UserRole> signingOfficialRole = List.of(UserRoles.SigningOfficial());
  private final User signingOfficial = new User(4, authUser.getEmail(), "Display Name", new Date(),
      signingOfficialRole);

  private DarCollectionResource resource;

  @Mock
  private DarCollectionService darCollectionService;
  @Mock
  private UserService userService;
  @Mock
  private ContainerRequest request;

  @BeforeEach
  void initResource() {
    resource = new DarCollectionResource(darCollectionService, userService);
  }

  private DataAccessRequest mockDataAccessRequestWithDatasetIds() {
    DataAccessRequest dar = new DataAccessRequest();
    DataAccessRequestData data = new DataAccessRequestData();
    dar.addDatasetId(randomInt(1, 100));
    dar.setData(data);
    dar.setReferenceId(UUID.randomUUID().toString());
    return dar;
  }

  private DarCollection mockDarCollection() {
    DarCollection collection = new DarCollection();
    collection.setDarCollectionId(randomInt(1, 100));
    for (int i = 0; i < 3; i++) {
      collection.addDar(mockDataAccessRequestWithDatasetIds());
    }
    return collection;
  }

  @Test
  void testGetCollectionByIdResearcher() {
    DuosUser duosUser = new DuosUser(authUser, researcher);
    DarCollection collection = mockDarCollection();
    collection.setCreateUser(researcher);
    collection.setCreateUserId(researcher.getUserId());
    when(darCollectionService.getByCollectionId(duosUser.getUser(), collection.getDarCollectionId())).thenReturn(collection);

    Response response = resource.getCollectionById(duosUser, collection.getDarCollectionId());
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void testGetCollectionByIdAdmin() {
    DarCollection collection = mockDarCollection();
    UserRole adminRole = UserRoles.Admin();
    User admin = new User(1, authUser.getEmail(), "Display Name", new Date(), List.of(adminRole));
    DuosUser duosUser = new DuosUser(authUser, admin);
    collection.setCreateUser(researcher);
    collection.setCreateUserId(researcher.getUserId());

    when(darCollectionService.getByCollectionId(duosUser.getUser(), collection.getDarCollectionId())).thenReturn(collection);

    Response response = resource.getCollectionById(duosUser, collection.getDarCollectionId());
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void testGetCollectionByIdSO() {
    DuosUser duosUser = new DuosUser(authUser, signingOfficial);
    DarCollection collection = mockDarCollection();
    signingOfficial.setInstitutionId(1);
    researcher.setInstitutionId(1);
    collection.setCreateUser(researcher);
    collection.setCreateUserId(researcher.getUserId());

    when(darCollectionService.getByCollectionId(duosUser.getUser(), collection.getDarCollectionId())).thenReturn(collection);

    Response response = resource.getCollectionById(duosUser, collection.getDarCollectionId());
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void testGetCollectionByIdSOWrongInstitution() {
    DuosUser duosUser = new DuosUser(authUser, signingOfficial);
    DarCollection collection = mockDarCollection();
    signingOfficial.setInstitutionId(2);
    researcher.setInstitutionId(1);
    collection.setCreateUser(researcher);
    collection.setCreateUserId(researcher.getUserId());

    when(darCollectionService.getByCollectionId(duosUser.getUser(), collection.getDarCollectionId())).thenReturn(collection);

    Response response = resource.getCollectionById(duosUser, collection.getDarCollectionId());
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  void testGetCollectionByIdNoInstitution() {
    DuosUser duosUser = new DuosUser(authUser, signingOfficial);
    DarCollection collection = mockDarCollection();
    researcher.setInstitutionId(1);
    collection.setCreateUser(researcher);
    collection.setCreateUserId(researcher.getUserId());

    when(darCollectionService.getByCollectionId(duosUser.getUser(), collection.getDarCollectionId())).thenReturn(collection);

    Response response = resource.getCollectionById(duosUser, collection.getDarCollectionId());
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  void testGetCollectionByIdSONoInstitution() {
    DuosUser duosUser = new DuosUser(authUser, signingOfficial);
    DarCollection collection = mockDarCollection();
    signingOfficial.setInstitutionId(1);
    collection.setCreateUser(researcher);
    collection.setCreateUserId(researcher.getUserId());

    when(darCollectionService.getByCollectionId(duosUser.getUser(), collection.getDarCollectionId())).thenReturn(collection);

    Response response = resource.getCollectionById(duosUser, collection.getDarCollectionId());
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  void testGetCollectionByIdChair() {
    List<UserRole> chairRole = List.of(UserRoles.Chairperson());
    User chair = new User(3, authUser.getEmail(), "Display Name", new Date(), chairRole);
    DuosUser duosUser = new DuosUser(authUser, chair);
    DarCollection collection = mockDarCollection();
    collection.setCreateUser(researcher);
    collection.setCreateUserId(researcher.getUserId());

    Dataset dataSet = new Dataset();
    dataSet.setDatasetId(2);
    collection.addDataset(dataSet);

    when(darCollectionService.getByCollectionId(duosUser.getUser(), collection.getDarCollectionId())).thenReturn(collection);
    when(darCollectionService.findDatasetIdsByDACUser(chair)).thenReturn(Arrays.asList(1, 2));

    Response response = resource.getCollectionById(duosUser, collection.getDarCollectionId());
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void testGetCollectionByIdDacMember() {
    List<UserRole> memberRole = List.of(UserRoles.Member());
    User member = new User(3, authUser.getEmail(), "Display Name", new Date(), memberRole);
    DuosUser duosUser = new DuosUser(authUser, member);
    DarCollection collection = mockDarCollection();
    collection.setCreateUser(researcher);
    collection.setCreateUserId(researcher.getUserId());

    Dataset dataSet = new Dataset();
    dataSet.setDatasetId(2);
    collection.addDataset(dataSet);

    when(darCollectionService.getByCollectionId(duosUser.getUser(), collection.getDarCollectionId())).thenReturn(collection);
    when(darCollectionService.findDatasetIdsByDACUser(member)).thenReturn(Arrays.asList(1, 2));

    Response response = resource.getCollectionById(duosUser, collection.getDarCollectionId());
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void testGetCollectionByIdDacMemberNoDatasetIdMatch() {
    List<UserRole> chairRole = List.of(UserRoles.Chairperson());
    User chair = new User(3, authUser.getEmail(), "Display Name", new Date(), chairRole);
    DuosUser duosUser = new DuosUser(authUser, chair);
    DarCollection collection = mockDarCollection();
    collection.setCreateUser(researcher);
    collection.setCreateUserId(researcher.getUserId());

    Dataset dataSet = new Dataset();
    dataSet.setDatasetId(3);
    collection.addDataset(dataSet);

    when(darCollectionService.getByCollectionId(duosUser.getUser(), collection.getDarCollectionId())).thenReturn(collection);
    when(darCollectionService.findDatasetIdsByDACUser(chair)).thenReturn(Arrays.asList(1, 2));

    Response response = resource.getCollectionById(duosUser, collection.getDarCollectionId());
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  void testGetCollectionByIdMultipleRoles() {
    UserRole chairRole = UserRoles.Chairperson();
    UserRole localResearcherRole = UserRoles.Researcher();
    User user = new User(1, authUser.getEmail(), "Display Name", new Date(),
        List.of(chairRole, localResearcherRole));
    DuosUser duosUser = new DuosUser(authUser, user);
    DarCollection collection = mockDarCollection();
    collection.setCreateUser(user);
    collection.setCreateUserId(user.getUserId());

    Dataset dataSet = new Dataset();
    dataSet.setDatasetId(3);
    collection.addDataset(dataSet);

    when(darCollectionService.getByCollectionId(duosUser.getUser(), collection.getDarCollectionId())).thenReturn(collection);
    when(darCollectionService.findDatasetIdsByDACUser(user)).thenReturn(Arrays.asList(1, 2));

    Response response = resource.getCollectionById(duosUser, collection.getDarCollectionId());
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void testGetCollectionWithAllElectionsByCollectionIdAdmin() {
    DarCollection collection = mockDarCollection();
    User admin = new User(2, authUser.getEmail(), "Admin User", new Date(),
        List.of(UserRoles.Admin()));
    DuosUser duosAdmin = new DuosUser(authUser, admin);
    collection.setCreateUser(researcher);
    collection.setCreateUserId(researcher.getUserId());

    when(darCollectionService.getCollectionWithAllElectionsByCollectionId(collection.getDarCollectionId())).thenReturn(collection);

    Response response = resource.getCollectionWithAllElectionsByCollectionId(duosAdmin, collection.getDarCollectionId());
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void testGetCollectionWithAllElectionsByCollectionIdDacMember() {
    DarCollection collection = mockDarCollection();
    List<Integer> userDatasetIds = List.of(1, 2);
    User member = new User(2, authUser.getEmail(), "Dac Member User", new Date(),
        List.of(UserRoles.Member()));
    DuosUser duosMember = new DuosUser(authUser, member);
    collection.setCreateUser(researcher);
    collection.setCreateUserId(researcher.getUserId());
    Dataset dataset1 = new Dataset();
    dataset1.setDatasetId(1);
    collection.setDatasets(Set.of(dataset1));

    when(darCollectionService.findDatasetIdsByDACUser(member)).thenReturn(userDatasetIds);
    when(darCollectionService.getCollectionWithElectionsByCollectionIdAndDatasetIds(userDatasetIds, collection.getDarCollectionId())).thenReturn(collection);

    Response response = resource.getCollectionWithAllElectionsByCollectionId(duosMember, collection.getDarCollectionId());
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void testGetCollectionWithAllElectionsById_CollectionNotFoundCollection() {
    when(darCollectionService.findDatasetIdsByDACUser(duosResearcher.getUser())).thenReturn(List.of());
    when(darCollectionService.getCollectionWithElectionsByCollectionIdAndDatasetIds(List.of(), 1))
        .thenThrow(new NotFoundException("Collection not found"));

    Response response = resource.getCollectionWithAllElectionsByCollectionId(duosResearcher, 1);
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  void testGetCollectionWithAllElectionsByCollectionId_ServiceException() {
    when(darCollectionService.findDatasetIdsByDACUser(duosResearcher.getUser())).thenReturn(List.of());
    when(darCollectionService.getCollectionWithElectionsByCollectionIdAndDatasetIds(List.of(), 1))
        .thenThrow(new RuntimeException("Service error"));

    Response response = resource.getCollectionWithAllElectionsByCollectionId(duosResearcher, 1);
    assertEquals(HttpStatusCodes.STATUS_CODE_SERVER_ERROR, response.getStatus());
  }

  @Test
  void testGetCollectionWithAllElectionsByCollectionId_UserNotAuthorized() {
    DarCollection collection = mockDarCollection();
    collection.setCreateUser(researcher);
    collection.setCreateUserId(researcher.getUserId());

    when(darCollectionService.findDatasetIdsByDACUser(researcher)).thenReturn(List.of());
    when(darCollectionService.getCollectionWithElectionsByCollectionIdAndDatasetIds(List.of(),
        collection.getDarCollectionId())).thenReturn(collection);

    Response response = resource.getCollectionWithAllElectionsByCollectionId(duosResearcher,
        collection.getDarCollectionId());
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  void testGetCollectionByReferenceId() {
    DuosUser duosUser = new DuosUser(authUser, researcher);
    DarCollection collection = mockDarCollection();
    String referenceId = collection.getDars().values().stream().findFirst().orElseThrow().getReferenceId();
    assertNotNull(referenceId);
    collection.setCreateUserId(researcher.getUserId());
    when(darCollectionService.getByReferenceId(duosUser.getUser(), referenceId)).thenReturn(collection);

    Response response = resource.getCollectionByReferenceId(duosUser, referenceId);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void testGetCollectionByReferenceIdNotFound() {
    DuosUser duosUser = new DuosUser(authUser, researcher);
    DarCollection collection = mockDarCollection();
    String referenceId = collection.getDars().values().stream().findFirst().orElseThrow().getReferenceId();
    assertNotNull(referenceId);
    collection.setCreateUserId(researcher.getUserId() + 1);
    when(darCollectionService.getByReferenceId(duosUser.getUser(), referenceId)).thenThrow(new NotFoundException("Collection not found"));

    Response response = resource.getCollectionByReferenceId(duosUser, referenceId);
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  void testCancelDarCollection_NotFoundStatus() {
    DuosUser duosUser = new DuosUser(authUser, researcher);
    DarCollection collection = mockDarCollection();
    collection.setCreateUserId(researcher.getUserId());
    when(darCollectionService.getByCollectionId(duosUser.getUser(), collection.getDarCollectionId())).thenReturn(null);

    try (var response = resource.cancelDarCollectionByCollectionId(duosUser, request, collection.getDarCollectionId(), null)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
    }
  }

  @Test
  void testCancelDarCollection_BadRequestStatus() {
    DuosUser duosUser = new DuosUser(authUser, researcher);
    DarCollection collection = mockDarCollection();
    collection.setCreateUserId(researcher.getUserId());
    int collectionId = collection.getDarCollectionId();
    when(darCollectionService.getByCollectionId(duosUser.getUser(), collectionId)).thenReturn(collection);
    when(darCollectionService.cancelDarCollectionByRole(researcher, collection, UserRoles.RESEARCHER))
        .thenThrow(new BadRequestException());

    try (var response = resource.cancelDarCollectionByCollectionId(duosUser, request, collectionId, null)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }
  }

  @Test
  void testCancelDarCollection_InternalErrorStatus() {
    DuosUser duosUser = new DuosUser(authUser, researcher);
    DarCollection collection = mockDarCollection();
    collection.setCreateUserId(researcher.getUserId());
    int collectionId = collection.getDarCollectionId();
    when(darCollectionService.getByCollectionId(duosUser.getUser(), collectionId)).thenReturn(collection);
    when(darCollectionService.cancelDarCollectionByRole(researcher, collection, UserRoles.RESEARCHER))
        .thenThrow(new InternalServerErrorException());

    try (var response = resource.cancelDarCollectionByCollectionId(duosUser, request, collectionId, null)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_SERVER_ERROR, response.getStatus());
    }
  }

  @Test
  void testCancelDarCollection_asAdmin() {
    List<UserRole> adminRole = List.of(UserRoles.Admin());
    User admin = new User(1, authUser.getEmail(), "Display Name", new Date(), adminRole);
    DuosUser duosUser = new DuosUser(authUser, admin);

    DarCollection collection = mockDarCollection();
    collection.setCreateUserId(admin.getUserId());
    int collectionId = collection.getDarCollectionId();
    when(darCollectionService.getByCollectionId(duosUser.getUser(), collectionId)).thenReturn(collection);
    when(darCollectionService.cancelDarCollectionByRole(admin, collection, UserRoles.ADMIN))
        .thenReturn(collection);

    try (var response = resource.cancelDarCollectionByCollectionId(duosUser, request, collectionId, Resource.ADMIN)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }
  }

  @Test
  void testCancelDarCollection_asChair() {
    List<UserRole> chairRole = List.of(UserRoles.Chairperson());
    User chair = new User(1, authUser.getEmail(), "Display Name", new Date(), chairRole);
    DuosUser duosUser = new DuosUser(authUser, chair);

    DarCollection collection = mockDarCollection();
    collection.setCreateUserId(chair.getUserId());
    int collectionId = collection.getDarCollectionId();
    when(darCollectionService.getByCollectionId(duosUser.getUser(), collectionId)).thenReturn(collection);
    when(darCollectionService.cancelDarCollectionByRole(chair, collection, UserRoles.CHAIRPERSON))
        .thenReturn(collection);

    try (var response = resource.cancelDarCollectionByCollectionId(duosUser, request, collectionId, Resource.CHAIRPERSON)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }
  }

  @Test
  void testCancelDarCollection_asChairAsAdmin() {
    List<UserRole> chairRole = List.of(UserRoles.Chairperson());
    User chair = new User(1, authUser.getEmail(), "Display Name", new Date(), chairRole);
    DuosUser duosUser = new DuosUser(authUser, chair);

    DarCollection collection = mockDarCollection();
    collection.setCreateUserId(chair.getUserId());
    when(darCollectionService.getByCollectionId(duosUser.getUser(), collection.getDarCollectionId())).thenReturn(collection);

    try (var response = resource.cancelDarCollectionByCollectionId(duosUser, request, collection.getDarCollectionId(), Resource.ADMIN)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }
  }

  @Test
  void testCancelDarCollection_asResearcher() {
    DuosUser duosUser = new DuosUser(authUser, researcher);
    DarCollection collection = mockDarCollection();
    int collectionId = collection.getDarCollectionId();
    collection.setCreateUserId(researcher.getUserId());
    when(darCollectionService.getByCollectionId(duosUser.getUser(), collectionId)).thenReturn(collection);
    when(darCollectionService.cancelDarCollectionByRole(researcher, collection, UserRoles.RESEARCHER))
        .thenReturn(collection);

    try (var response = resource.cancelDarCollectionByCollectionId(duosUser, request, collectionId, Resource.RESEARCHER)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }
  }

  @Test
  void testCancelDarCollection_asResearcherAsAdmin() {
    DuosUser duosUser = new DuosUser(authUser, researcher);
    DarCollection collection = mockDarCollection();
    collection.setCreateUserId(researcher.getUserId());
    when(darCollectionService.getByCollectionId(duosUser.getUser(), collection.getDarCollectionId())).thenReturn(collection);

    try (var response = resource.cancelDarCollectionByCollectionId(duosUser, request, collection.getDarCollectionId(), Resource.ADMIN)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }
  }


  @Test
  void testResubmitDarCollection_CollectionNotFound() {
    int collectionId = 1;
    User user = mock(User.class);
    DuosUser duosUser = new DuosUser(authUser, user);
    when(darCollectionService.getByCollectionId(duosUser.getUser(), collectionId)).thenReturn(null);

    try (var response = resource.resubmitDarCollection(duosUser, collectionId)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
    }
  }

  @Test
  void testResubmitDarCollection_UserNotCreator() {
    int collectionId = 1;
    User user = mock(User.class);
    DuosUser duosUser = new DuosUser(authUser, user);
    when(darCollectionService.getByCollectionId(duosUser.getUser(), collectionId)).thenReturn(new DarCollection());

    try (var response = resource.resubmitDarCollection(duosUser, collectionId)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
    }
  }

  @Test
  void testResubmitDarCollection_CollectionNotCanceled() {
    int userId = 1;
    User user = mock(User.class);
    DuosUser duosUser = new DuosUser(authUser, user);
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
    when(darCollectionService.getByCollectionId(duosUser.getUser(), collection.getDarCollectionId())).thenReturn(collection);

    try (var response = resource.resubmitDarCollection(duosUser, collection.getDarCollectionId())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }
  }

  @Test
  void testResubmitDarCollection_Success() {
    int userId = 1;
    User user = mock(User.class);
    DuosUser duosUser = new DuosUser(authUser, user);
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
    when(darCollectionService.getByCollectionId(duosUser.getUser(), collection.getDarCollectionId())).thenReturn(collection);

    try (var response = resource.resubmitDarCollection(duosUser, collection.getDarCollectionId())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }
  }

  @Test
  void testCreateElectionsForCollection() {
    User user = mock(User.class);
    DuosUser duosUser = new DuosUser(authUser, user);
    DarCollection collection = mock(DarCollection.class);
    when(darCollectionService.getByCollectionId(duosUser.getUser(), collection.getDarCollectionId())).thenReturn(collection);

    try (var response = resource.createElectionsForCollection(duosUser, collection.getDarCollectionId())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }
  }

  @Test
  void testCreateElectionsForCollectionNotFound() {
    int collectionId = 1;
    DuosUser duosUser = new DuosUser(authUser, researcher);
    when(darCollectionService.getByCollectionId(duosUser.getUser(), collectionId)).thenReturn(null);

    try (var response = resource.createElectionsForCollection(duosUser, collectionId)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
    }
  }

  @Test
  void getCollectionSummariesForUserByRole_Member() {
    User user = new User();
    user.setMemberRole();
    DarCollectionSummary mockSummary = new DarCollectionSummary();
    when(userService.findUserByEmail(authUser.getEmail())).thenReturn(user);
    when(darCollectionService.getSummariesForRole(user, UserRoles.MEMBER))
        .thenReturn(List.of(mockSummary));

    Response response = resource.getCollectionSummariesForUserByRole(authUser,
        UserRoles.MEMBER.getRoleName());
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void getCollectionSummariesForUserByRole_Chair() {
    User user = new User();
    user.setChairpersonRole();
    DarCollectionSummary mockSummary = new DarCollectionSummary();
    when(userService.findUserByEmail(authUser.getEmail())).thenReturn(user);
    when(darCollectionService.getSummariesForRole(user, UserRoles.CHAIRPERSON))
        .thenReturn(List.of(mockSummary));

    Response response = resource.getCollectionSummariesForUserByRole(authUser,
        UserRoles.CHAIRPERSON.getRoleName());
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void getCollectionSummariesForUserByRole_SO() {
    User user = new User();
    user.setSigningOfficialRole();
    DarCollectionSummary mockSummary = new DarCollectionSummary();
    when(userService.findUserByEmail(authUser.getEmail())).thenReturn(user);
    when(darCollectionService.getSummariesForRole(user, UserRoles.SIGNINGOFFICIAL))
        .thenReturn(List.of(mockSummary));

    Response response = resource.getCollectionSummariesForUserByRole(authUser,
        UserRoles.SIGNINGOFFICIAL.getRoleName());
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void getCollectionSummariesForUserByRole_Researcher() {
    User user = new User();
    user.setResearcherRole();
    DarCollectionSummary mockSummary = new DarCollectionSummary();
    when(userService.findUserByEmail(authUser.getEmail())).thenReturn(user);
    when(darCollectionService.getSummariesForRole(user, UserRoles.RESEARCHER))
        .thenReturn(List.of(mockSummary));

    Response response = resource.getCollectionSummariesForUserByRole(authUser,
        UserRoles.RESEARCHER.getRoleName());
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void getCollectionSummariesForUserByRole_Admin() {
    User user = new User();
    user.setAdminRole();
    DarCollectionSummary mockSummary = new DarCollectionSummary();
    when(userService.findUserByEmail(authUser.getEmail())).thenReturn(user);
    when(darCollectionService.getSummariesForRole(user, UserRoles.ADMIN))
        .thenReturn(List.of(mockSummary));

    Response response = resource.getCollectionSummariesForUserByRole(authUser,
        UserRoles.ADMIN.getRoleName());
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void getCollectionSummariesForUserByRole_NoRoleFound() {
    User user = new User();
    when(userService.findUserByEmail(anyString())).thenReturn(user);

    Response response = resource.getCollectionSummariesForUserByRole(authUser,
        UserRoles.SIGNINGOFFICIAL.getRoleName());
    assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
  }

  @Test
  void getCollectionSummariesForUserByRole_InvalidRoleString() {
    User user = new User();
    when(userService.findUserByEmail(anyString())).thenReturn(user);

    Response response = resource.getCollectionSummariesForUserByRole(authUser, "invalid");
    assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
  }


  @Test
  void getCollectionSummaryForRoleById_Member() {
    User user = new User();
    user.setMemberRole();

    DarCollectionSummary mockSummary = new DarCollectionSummary();
    mockSummary.setDatasetIds(Set.of(1));
    Integer collectionId = randomInt(1, 100);

    when(userService.findUserByEmail(authUser.getEmail())).thenReturn(user);
    when(darCollectionService.findDatasetIdsByDACUser(user)).thenReturn(List.of(1, 2));
    when(darCollectionService.getSummaryForRoleByCollectionId(user, UserRoles.MEMBER, collectionId))
        .thenReturn(mockSummary);

    Response response = resource.getCollectionSummaryForRoleById(authUser,
        UserRoles.MEMBER.getRoleName(), collectionId);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void getCollectionSummaryForRoleById_MemberNoDatasetsInCollection() {
    User user = new User();
    user.setMemberRole();

    DarCollectionSummary mockSummary = new DarCollectionSummary();
    mockSummary.setDatasetIds(Set.of(1));
    Integer collectionId = randomInt(1, 100);

    when(userService.findUserByEmail(authUser.getEmail())).thenReturn(user);
    when(darCollectionService.findDatasetIdsByDACUser(user)).thenReturn(List.of(2));
    when(darCollectionService.getSummaryForRoleByCollectionId(user, UserRoles.MEMBER, collectionId))
        .thenReturn(mockSummary);

    Response response = resource.getCollectionSummaryForRoleById(authUser,
        UserRoles.MEMBER.getRoleName(), collectionId);
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  void getCollectionSummaryForRoleById_Chair() {
    User user = new User();
    user.setChairpersonRole();

    DarCollectionSummary mockSummary = new DarCollectionSummary();
    mockSummary.setDatasetIds(Set.of(1));
    Integer collectionId = randomInt(1, 100);

    when(userService.findUserByEmail(authUser.getEmail())).thenReturn(user);
    when(darCollectionService.findDatasetIdsByDACUser(user)).thenReturn(List.of(1, 2));
    when(darCollectionService.getSummaryForRoleByCollectionId(user, UserRoles.CHAIRPERSON, collectionId))
        .thenReturn(mockSummary);

    Response response = resource.getCollectionSummaryForRoleById(authUser,
        UserRoles.CHAIRPERSON.getRoleName(), collectionId);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void getCollectionSummaryForRoleById_ChairNoDatasetsInCollection() {
    User user = new User();
    user.setChairpersonRole();

    DarCollectionSummary mockSummary = new DarCollectionSummary();
    mockSummary.setDatasetIds(Set.of(1));
    Integer collectionId = randomInt(1, 100);

    when(userService.findUserByEmail(authUser.getEmail())).thenReturn(user);
    when(darCollectionService.findDatasetIdsByDACUser(user)).thenReturn(List.of(2));
    when(darCollectionService.getSummaryForRoleByCollectionId(user, UserRoles.CHAIRPERSON, collectionId))
        .thenReturn(mockSummary);

    Response response = resource.getCollectionSummaryForRoleById(authUser,
        UserRoles.CHAIRPERSON.getRoleName(), collectionId);
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  void getCollectionSummaryForRoleById_SO() {
    User user = new User();
    user.setSigningOfficialRole();
    Integer institutionId = randomInt(1, 100);
    user.setInstitutionId(institutionId);

    DarCollectionSummary mockSummary = new DarCollectionSummary();
    mockSummary.setInstitutionId(institutionId);
    Integer collectionId = randomInt(1, 100);

    when(userService.findUserByEmail(authUser.getEmail())).thenReturn(user);
    when(darCollectionService.getSummaryForRoleByCollectionId(user, UserRoles.SIGNINGOFFICIAL,
        collectionId))
        .thenReturn(mockSummary);

    Response response = resource.getCollectionSummaryForRoleById(authUser,
        UserRoles.SIGNINGOFFICIAL.getRoleName(), collectionId);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void getCollectionSummaryForRoleById_SODifferentInstitution() {
    User user = new User();
    user.setSigningOfficialRole();
    user.setInstitutionId(1);

    DarCollectionSummary mockSummary = new DarCollectionSummary();
    mockSummary.setInstitutionId(2);
    Integer collectionId = randomInt(1, 100);

    when(userService.findUserByEmail(authUser.getEmail())).thenReturn(user);
    when(darCollectionService.getSummaryForRoleByCollectionId(user, UserRoles.SIGNINGOFFICIAL,
        collectionId))
        .thenReturn(mockSummary);

    Response response = resource.getCollectionSummaryForRoleById(authUser,
        UserRoles.SIGNINGOFFICIAL.getRoleName(), collectionId);
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  void getCollectionSummaryForRoleById_Researcher() {
    User user = new User();
    Integer userId = randomInt(1, 100);
    user.setUserId(userId);
    user.setResearcherRole();

    DarCollectionSummary mockSummary = new DarCollectionSummary();
    mockSummary.setResearcherId(userId);
    Integer collectionId = randomInt(1, 100);

    when(userService.findUserByEmail(authUser.getEmail())).thenReturn(user);
    when(darCollectionService.getSummaryForRoleByCollectionId(user, UserRoles.RESEARCHER,
        collectionId))
        .thenReturn(mockSummary);

    Response response = resource.getCollectionSummaryForRoleById(authUser,
        UserRoles.RESEARCHER.getRoleName(), collectionId);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void getCollectionSummaryForRoleById_ResearcherNotCreateUser() {
    User user = new User();
    user.setUserId(1);
    user.setResearcherRole();

    DarCollectionSummary mockSummary = new DarCollectionSummary();
    mockSummary.setResearcherId(2);
    Integer collectionId = randomInt(1, 100);

    when(userService.findUserByEmail(authUser.getEmail())).thenReturn(user);
    when(darCollectionService.getSummaryForRoleByCollectionId(user, UserRoles.RESEARCHER,
        collectionId))
        .thenReturn(mockSummary);

    Response response = resource.getCollectionSummaryForRoleById(authUser,
        UserRoles.RESEARCHER.getRoleName(), collectionId);
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  void getCollectionSummaryForRoleById_Admin() {
    User user = new User();
    user.setAdminRole();
    DarCollectionSummary mockSummary = new DarCollectionSummary();
    Integer collectionId = randomInt(1, 100);

    when(userService.findUserByEmail(authUser.getEmail())).thenReturn(user);
    when(darCollectionService.getSummaryForRoleByCollectionId(user, UserRoles.ADMIN,
        collectionId))
        .thenReturn(mockSummary);

    Response response = resource.getCollectionSummaryForRoleById(authUser,
        UserRoles.ADMIN.getRoleName(), collectionId);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void getCollectionSummaryForRoleById_NoRoleFound() {
    User user = new User();
    Integer collectionId = randomInt(1, 100);

    when(userService.findUserByEmail(anyString())).thenReturn(user);

    Response response = resource.getCollectionSummaryForRoleById(authUser,
        UserRoles.SIGNINGOFFICIAL.getRoleName(), collectionId);
    assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
  }

  @Test
  void getCollectionSummaryForRoleById_InvalidRoleString() {
    User user = new User();
    Integer collectionId = randomInt(1, 100);

    when(userService.findUserByEmail(anyString())).thenReturn(user);

    Response response = resource.getCollectionSummaryForRoleById(authUser, "invalid", collectionId);
    assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
  }

  @Test
  void getCollectionSummaryForRoleById_CollectionNotFound() {
    User user = new User();
    user.setResearcherRole();
    Integer collectionId = randomInt(1, 100);

    when(userService.findUserByEmail(authUser.getEmail())).thenReturn(user);
    when(darCollectionService.getSummaryForRoleByCollectionId(user, UserRoles.RESEARCHER,
        collectionId))
        .thenThrow(new NotFoundException());

    Response response = resource.getCollectionSummaryForRoleById(authUser,
        UserRoles.RESEARCHER.getRoleName(), collectionId);
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }
}
