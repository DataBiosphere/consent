package org.broadinstitute.consent.http.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
  private final User researcher =
      new User(1, authUser.getEmail(), "Display Name", new Date(), researcherRole);
  private final DuosUser duosResearcher = new DuosUser(authUser, researcher);
  private final List<UserRole> signingOfficialRole = List.of(UserRoles.SigningOfficial());
  private final User signingOfficial =
      new User(4, authUser.getEmail(), "Display Name", new Date(), signingOfficialRole);
  private final DuosUser duosSigningOfficial = new DuosUser(authUser, signingOfficial);

  private final List<UserRole> adminRole = List.of(UserRoles.Admin());
  private final User admin =
      new User(5, authUser.getEmail(), "Display Name", new Date(), adminRole);
  private final DuosUser duosAdmin = new DuosUser(authUser, admin);

  private final List<UserRole> chairpersonRole = List.of(UserRoles.Chairperson());
  private final User chairperson =
      new User(6, authUser.getEmail(), "Display Name", new Date(), chairpersonRole);
  private final DuosUser duosChairperson = new DuosUser(authUser, chairperson);

  private final List<UserRole> memberRole = List.of(UserRoles.Member());
  private final User member =
      new User(7, authUser.getEmail(), "Display Name", new Date(), memberRole);
  private final DuosUser duosMember = new DuosUser(authUser, member);

  private DarCollectionResource resource;

  @Mock private DarCollectionService darCollectionService;
  @Mock private ContainerRequest request;

  @BeforeEach
  void initResource() {
    resource = new DarCollectionResource(darCollectionService);
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
    when(darCollectionService.getByCollectionId(researcher, collection.getDarCollectionId()))
        .thenReturn(collection);

    Response response = resource.getCollectionById(duosUser, collection.getDarCollectionId());
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void testGetCollectionByIdResearcherNotFound() {
    DarCollection collection = mockDarCollection();
    collection.setCreateUser(researcher);
    collection.setCreateUserId(researcher.getUserId() + 1);
    when(darCollectionService.getByCollectionId(researcher, collection.getDarCollectionId()))
        .thenReturn(collection);

    Response response = resource.getCollectionById(duosResearcher, collection.getDarCollectionId());
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  void testGetCollectionByIdAdmin() {
    DarCollection collection = mockDarCollection();
    collection.setCreateUser(researcher);
    collection.setCreateUserId(researcher.getUserId());

    when(darCollectionService.getByCollectionId(admin, collection.getDarCollectionId()))
        .thenReturn(collection);

    Response response = resource.getCollectionById(duosAdmin, collection.getDarCollectionId());
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void testGetCollectionByIdSO() {
    DarCollection collection = mockDarCollection();
    signingOfficial.setInstitutionId(1);
    researcher.setInstitutionId(1);
    collection.setCreateUser(researcher);
    collection.setCreateUserId(researcher.getUserId());

    when(darCollectionService.getByCollectionId(signingOfficial, collection.getDarCollectionId()))
        .thenReturn(collection);

    Response response =
        resource.getCollectionById(duosSigningOfficial, collection.getDarCollectionId());
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void testGetCollectionByIdSOWrongInstitution() {
    DarCollection collection = mockDarCollection();
    signingOfficial.setInstitutionId(2);
    researcher.setInstitutionId(1);
    collection.setCreateUser(researcher);
    collection.setCreateUserId(researcher.getUserId());

    when(darCollectionService.getByCollectionId(signingOfficial, collection.getDarCollectionId()))
        .thenReturn(collection);

    Response response =
        resource.getCollectionById(duosSigningOfficial, collection.getDarCollectionId());
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  void testGetCollectionByIdNoInstitution() {
    DarCollection collection = mockDarCollection();
    researcher.setInstitutionId(1);
    collection.setCreateUser(researcher);
    collection.setCreateUserId(researcher.getUserId());

    when(darCollectionService.getByCollectionId(signingOfficial, collection.getDarCollectionId()))
        .thenReturn(collection);

    Response response =
        resource.getCollectionById(duosSigningOfficial, collection.getDarCollectionId());
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  void testGetCollectionByIdSONoInstitution() {
    DarCollection collection = mockDarCollection();
    signingOfficial.setInstitutionId(1);
    collection.setCreateUser(researcher);
    collection.setCreateUserId(researcher.getUserId());

    when(darCollectionService.getByCollectionId(signingOfficial, collection.getDarCollectionId()))
        .thenReturn(collection);

    Response response =
        resource.getCollectionById(duosSigningOfficial, collection.getDarCollectionId());
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  void testGetCollectionByIdChair() {
    List<UserRole> chairRole = List.of(UserRoles.Chairperson());
    User chair = new User(3, authUser.getEmail(), "Display Name", new Date(), chairRole);
    DarCollection collection = mockDarCollection();
    collection.setCreateUser(researcher);
    collection.setCreateUserId(researcher.getUserId());

    Dataset dataSet = new Dataset();
    dataSet.setDatasetId(2);
    collection.addDataset(dataSet);

    when(darCollectionService.getByCollectionId(chair, collection.getDarCollectionId()))
        .thenReturn(collection);
    when(darCollectionService.findDatasetIdsByDACUser(chair)).thenReturn(Arrays.asList(1, 2));

    Response response =
        resource.getCollectionById(new DuosUser(authUser, chair), collection.getDarCollectionId());
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void testGetCollectionByIdDacMember() {
    DarCollection collection = mockDarCollection();
    collection.setCreateUser(researcher);
    collection.setCreateUserId(researcher.getUserId());

    Dataset dataSet = new Dataset();
    dataSet.setDatasetId(2);
    collection.addDataset(dataSet);

    when(darCollectionService.getByCollectionId(member, collection.getDarCollectionId()))
        .thenReturn(collection);
    when(darCollectionService.findDatasetIdsByDACUser(member)).thenReturn(Arrays.asList(1, 2));

    Response response = resource.getCollectionById(duosMember, collection.getDarCollectionId());
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void testGetCollectionByIdDacMemberNoDatasetIdMatch() {
    List<UserRole> chairRole = List.of(UserRoles.Chairperson());
    User chair = new User(3, authUser.getEmail(), "Display Name", new Date(), chairRole);
    DarCollection collection = mockDarCollection();
    collection.setCreateUser(researcher);
    collection.setCreateUserId(researcher.getUserId());

    Dataset dataSet = new Dataset();
    dataSet.setDatasetId(3);
    collection.addDataset(dataSet);

    when(darCollectionService.getByCollectionId(chair, collection.getDarCollectionId()))
        .thenReturn(collection);
    when(darCollectionService.findDatasetIdsByDACUser(chair)).thenReturn(Arrays.asList(1, 2));

    Response response =
        resource.getCollectionById(new DuosUser(authUser, chair), collection.getDarCollectionId());
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  void testGetCollectionByIdMultipleRoles() {
    UserRole chairRole = UserRoles.Chairperson();
    UserRole localResearcherRole = UserRoles.Researcher();
    User user =
        new User(
            1,
            authUser.getEmail(),
            "Display Name",
            new Date(),
            List.of(chairRole, localResearcherRole));
    DarCollection collection = mockDarCollection();
    collection.setCreateUser(user);
    collection.setCreateUserId(user.getUserId());

    Dataset dataSet = new Dataset();
    dataSet.setDatasetId(3);
    collection.addDataset(dataSet);

    when(darCollectionService.getByCollectionId(researcher, collection.getDarCollectionId()))
        .thenReturn(collection);
    when(darCollectionService.findDatasetIdsByDACUser(researcher)).thenReturn(Arrays.asList(1, 2));

    Response response = resource.getCollectionById(duosResearcher, collection.getDarCollectionId());
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void testGetCollectionWithAllElectionsByCollectionIdAdmin() {
    DarCollection collection = mockDarCollection();
    collection.setCreateUser(researcher);
    collection.setCreateUserId(researcher.getUserId());

    when(darCollectionService.getCollectionWithAllElectionsByCollectionId(
            admin, collection.getDarCollectionId()))
        .thenReturn(collection);

    Response response =
        resource.getCollectionWithAllElectionsByCollectionId(
            duosAdmin, collection.getDarCollectionId());
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void testGetCollectionWithAllElectionsByCollectionIdDacMember() {
    DarCollection collection = mockDarCollection();
    List<Integer> userDatasetIds = List.of(1, 2);
    collection.setCreateUser(researcher);
    collection.setCreateUserId(researcher.getUserId());
    Dataset dataset1 = new Dataset();
    dataset1.setDatasetId(1);
    collection.setDatasets(Set.of(dataset1));

    when(darCollectionService.findDatasetIdsByDACUser(member)).thenReturn(userDatasetIds);
    when(darCollectionService.getCollectionWithElectionsByCollectionIdAndDatasetIds(
            member, userDatasetIds, collection.getDarCollectionId()))
        .thenReturn(collection);

    Response response =
        resource.getCollectionWithAllElectionsByCollectionId(
            duosMember, collection.getDarCollectionId());
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void testGetCollectionWithAllElectionsById_CollectionNotFoundCollection() {
    when(darCollectionService.findDatasetIdsByDACUser(researcher)).thenReturn(List.of());
    when(darCollectionService.getCollectionWithElectionsByCollectionIdAndDatasetIds(
            researcher, List.of(), 1))
        .thenThrow(new NotFoundException("Collection not found"));

    Response response = resource.getCollectionWithAllElectionsByCollectionId(duosResearcher, 1);
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  void testGetCollectionWithAllElectionsByCollectionId_ServiceException() {
    when(darCollectionService.findDatasetIdsByDACUser(researcher)).thenReturn(List.of());
    when(darCollectionService.getCollectionWithElectionsByCollectionIdAndDatasetIds(
            researcher, List.of(), 1))
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
    when(darCollectionService.getCollectionWithElectionsByCollectionIdAndDatasetIds(
            researcher, List.of(), collection.getDarCollectionId()))
        .thenReturn(collection);

    Response response =
        resource.getCollectionWithAllElectionsByCollectionId(
            duosResearcher, collection.getDarCollectionId());
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  void testGetCollectionByReferenceId() {
    DarCollection collection = mockDarCollection();
    String referenceId =
        collection.getDars().values().stream().findFirst().orElseThrow().getReferenceId();
    assertNotNull(referenceId);
    collection.setCreateUserId(researcher.getUserId());
    when(darCollectionService.getByReferenceId(researcher, referenceId)).thenReturn(collection);

    Response response = resource.getCollectionByReferenceId(duosResearcher, referenceId);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void testGetCollectionByReferenceIdNotFound() {
    DarCollection collection = mockDarCollection();
    String referenceId =
        collection.getDars().values().stream().findFirst().orElseThrow().getReferenceId();
    assertNotNull(referenceId);
    collection.setCreateUserId(researcher.getUserId() + 1);
    when(darCollectionService.getByReferenceId(researcher, referenceId)).thenReturn(collection);

    Response response = resource.getCollectionByReferenceId(duosResearcher, referenceId);
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  void testCancelDarCollection_NotFoundStatus() {
    DarCollection collection = mockDarCollection();
    collection.setCreateUserId(researcher.getUserId());
    when(darCollectionService.getByCollectionId(researcher, collection.getDarCollectionId()))
        .thenReturn(null);

    try (var response =
        resource.cancelDarCollectionByCollectionId(
            duosResearcher, request, collection.getDarCollectionId(), null)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
    }
  }

  @Test
  void testCancelDarCollection_BadRequestStatus() {
    DarCollection collection = mockDarCollection();
    collection.setCreateUserId(researcher.getUserId());
    int collectionId = collection.getDarCollectionId();
    when(darCollectionService.getByCollectionId(researcher, collectionId)).thenReturn(collection);
    when(darCollectionService.cancelDarCollectionByRole(
            researcher, collection, UserRoles.RESEARCHER))
        .thenThrow(new BadRequestException());

    try (var response =
        resource.cancelDarCollectionByCollectionId(duosResearcher, request, collectionId, null)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }
  }

  @Test
  void testCancelDarCollection_InternalErrorStatus() {
    DarCollection collection = mockDarCollection();
    collection.setCreateUserId(researcher.getUserId());
    int collectionId = collection.getDarCollectionId();
    when(darCollectionService.getByCollectionId(researcher, collectionId)).thenReturn(collection);
    when(darCollectionService.cancelDarCollectionByRole(
            researcher, collection, UserRoles.RESEARCHER))
        .thenThrow(new InternalServerErrorException());

    try (var response =
        resource.cancelDarCollectionByCollectionId(duosResearcher, request, collectionId, null)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_SERVER_ERROR, response.getStatus());
    }
  }

  @Test
  void testCancelDarCollection_asAdmin() {
    DarCollection collection = mockDarCollection();
    collection.setCreateUserId(admin.getUserId());
    int collectionId = collection.getDarCollectionId();
    when(darCollectionService.getByCollectionId(admin, collectionId)).thenReturn(collection);
    when(darCollectionService.cancelDarCollectionByRole(admin, collection, UserRoles.ADMIN))
        .thenReturn(collection);

    try (var response =
        resource.cancelDarCollectionByCollectionId(
            duosAdmin, request, collectionId, Resource.ADMIN)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }
  }

  @Test
  void testCancelDarCollection_asChair() {
    DarCollection collection = mockDarCollection();
    collection.setCreateUserId(chairperson.getUserId());
    int collectionId = collection.getDarCollectionId();
    when(darCollectionService.getByCollectionId(chairperson, collectionId)).thenReturn(collection);
    when(darCollectionService.cancelDarCollectionByRole(
            chairperson, collection, UserRoles.CHAIRPERSON))
        .thenReturn(collection);

    try (var response =
        resource.cancelDarCollectionByCollectionId(
            duosChairperson, request, collectionId, Resource.CHAIRPERSON)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }
  }

  @Test
  void testCancelDarCollection_asChairAsAdmin() {
    DarCollection collection = mockDarCollection();
    collection.setCreateUserId(chairperson.getUserId());
    when(darCollectionService.getByCollectionId(chairperson, collection.getDarCollectionId()))
        .thenReturn(collection);

    try (var response =
        resource.cancelDarCollectionByCollectionId(
            duosChairperson, request, collection.getDarCollectionId(), Resource.ADMIN)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }
  }

  @Test
  void testCancelDarCollection_asResearcher() {
    DarCollection collection = mockDarCollection();
    int collectionId = collection.getDarCollectionId();
    collection.setCreateUserId(researcher.getUserId());
    when(darCollectionService.getByCollectionId(researcher, collectionId)).thenReturn(collection);
    when(darCollectionService.cancelDarCollectionByRole(
            researcher, collection, UserRoles.RESEARCHER))
        .thenReturn(collection);

    try (var response =
        resource.cancelDarCollectionByCollectionId(
            duosResearcher, request, collectionId, Resource.RESEARCHER)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }
  }

  @Test
  void testCancelDarCollection_asResearcherAsAdmin() {
    DarCollection collection = mockDarCollection();
    collection.setCreateUserId(researcher.getUserId());
    when(darCollectionService.getByCollectionId(researcher, collection.getDarCollectionId()))
        .thenReturn(collection);

    try (var response =
        resource.cancelDarCollectionByCollectionId(
            duosResearcher, request, collection.getDarCollectionId(), Resource.ADMIN)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }
  }

  @Test
  void testResubmitDarCollection_CollectionNotFound() {
    when(darCollectionService.getByCollectionId(researcher, 1)).thenReturn(null);

    try (var response = resource.resubmitDarCollection(duosResearcher, 1)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
    }
  }

  @Test
  void testResubmitDarCollection_UserNotCreator() {
    when(darCollectionService.getByCollectionId(researcher, 1)).thenReturn(new DarCollection());

    try (var response = resource.resubmitDarCollection(duosResearcher, 1)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
    }
  }

  @Test
  void testResubmitDarCollection_CollectionNotCanceled() {
    DarCollection collection = mock(DarCollection.class);
    when(collection.getCreateUserId()).thenReturn(researcher.getUserId());
    DataAccessRequest dar = mock(DataAccessRequest.class);
    DataAccessRequestData data = mock(DataAccessRequestData.class);
    String referenceId = UUID.randomUUID().toString();
    when(data.getStatus()).thenReturn("Not Canceled");
    when(dar.getData()).thenReturn(data);
    when(dar.getReferenceId()).thenReturn(referenceId);
    Map<String, DataAccessRequest> darMap = Map.of(dar.getReferenceId(), dar);
    when(collection.getDars()).thenReturn(darMap);
    when(darCollectionService.getByCollectionId(researcher, collection.getDarCollectionId()))
        .thenReturn(collection);

    try (var response =
        resource.resubmitDarCollection(duosResearcher, collection.getDarCollectionId())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }
  }

  @Test
  void testResubmitDarCollection_Success() {
    DarCollection collection = mock(DarCollection.class);
    when(collection.getCreateUserId()).thenReturn(researcher.getUserId());
    DataAccessRequest dar = mock(DataAccessRequest.class);
    DataAccessRequestData data = mock(DataAccessRequestData.class);
    String referenceId = UUID.randomUUID().toString();
    when(data.getStatus()).thenReturn(DarStatus.CANCELED.getValue());
    when(dar.getData()).thenReturn(data);
    when(dar.getReferenceId()).thenReturn(referenceId);
    Map<String, DataAccessRequest> darMap = Map.of(dar.getReferenceId(), dar);
    when(collection.getDars()).thenReturn(darMap);
    when(darCollectionService.getByCollectionId(researcher, collection.getDarCollectionId()))
        .thenReturn(collection);

    try (var response =
        resource.resubmitDarCollection(duosResearcher, collection.getDarCollectionId())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }
  }

  @Test
  void testCreateElectionsForCollection() {
    DarCollection collection = mock(DarCollection.class);
    when(darCollectionService.getByCollectionId(researcher, 1)).thenReturn(collection);

    try (var response = resource.createElectionsForCollection(duosResearcher, 1, request)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }
  }

  @Test
  void testCreateElectionsForCollectionNotFound() {
    when(darCollectionService.getByCollectionId(chairperson, 1)).thenReturn(null);

    try (var response = resource.createElectionsForCollection(duosChairperson, 1, request)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
    }
  }

  @Test
  void testApproveCollection() {
    DarCollection collection = mock(DarCollection.class);
    when(darCollectionService.getByCollectionId(signingOfficial, 1)).thenReturn(collection);
    try (var response = resource.approveCollection(duosSigningOfficial, 1, request)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }
  }

  @Test
  void testApproveCollection_NotFound() {
    when(darCollectionService.getByCollectionId(signingOfficial, 1)).thenReturn(null);
    try (var response = resource.approveCollection(duosSigningOfficial, 1, request)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
    }
  }

  @Test
  void getCollectionSummariesForUserByRole_Member() {
    DarCollectionSummary mockSummary = new DarCollectionSummary();
    when(darCollectionService.getSummariesForRole(member, UserRoles.MEMBER))
        .thenReturn(List.of(mockSummary));

    Response response =
        resource.getCollectionSummariesForUserByRole(duosMember, UserRoles.MEMBER.getRoleName());
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void getCollectionSummariesForUserByRole_Chair() {
    DarCollectionSummary mockSummary = new DarCollectionSummary();
    when(darCollectionService.getSummariesForRole(chairperson, UserRoles.CHAIRPERSON))
        .thenReturn(List.of(mockSummary));

    Response response =
        resource.getCollectionSummariesForUserByRole(
            duosChairperson, UserRoles.CHAIRPERSON.getRoleName());
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void getCollectionSummariesForUserByRole_SO() {
    DarCollectionSummary mockSummary = new DarCollectionSummary();
    when(darCollectionService.getSummariesForRole(signingOfficial, UserRoles.SIGNINGOFFICIAL))
        .thenReturn(List.of(mockSummary));

    Response response =
        resource.getCollectionSummariesForUserByRole(
            duosSigningOfficial, UserRoles.SIGNINGOFFICIAL.getRoleName());
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void getCollectionSummariesForUserByRole_Researcher() {
    DarCollectionSummary mockSummary = new DarCollectionSummary();
    when(darCollectionService.getSummariesForRole(researcher, UserRoles.RESEARCHER))
        .thenReturn(List.of(mockSummary));

    Response response =
        resource.getCollectionSummariesForUserByRole(
            duosResearcher, UserRoles.RESEARCHER.getRoleName());
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void getCollectionSummariesForUserByRole_Admin() {
    DarCollectionSummary mockSummary = new DarCollectionSummary();
    when(darCollectionService.getSummariesForRole(admin, UserRoles.ADMIN))
        .thenReturn(List.of(mockSummary));

    Response response =
        resource.getCollectionSummariesForUserByRole(duosAdmin, UserRoles.ADMIN.getRoleName());
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void getCollectionSummariesForUserByRole_NoRoleFound() {
    Response response =
        resource.getCollectionSummariesForUserByRole(
            duosResearcher, UserRoles.SIGNINGOFFICIAL.getRoleName());
    assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
  }

  @Test
  void getCollectionSummariesForUserByRole_InvalidRoleString() {
    Response response = resource.getCollectionSummariesForUserByRole(duosResearcher, "invalid");
    assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
  }

  @Test
  void getCollectionSummaryForRoleById_Member() {
    DarCollectionSummary mockSummary = new DarCollectionSummary();
    mockSummary.setDatasetIds(Set.of(1));
    Integer collectionId = randomInt(1, 100);

    when(darCollectionService.findDatasetIdsByDACUser(member)).thenReturn(List.of(1, 2));
    when(darCollectionService.getSummaryForRoleByCollectionId(
            member, UserRoles.MEMBER, collectionId))
        .thenReturn(mockSummary);

    Response response =
        resource.getCollectionSummaryForRoleById(
            duosMember, UserRoles.MEMBER.getRoleName(), collectionId);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void getCollectionSummaryForRoleById_MemberNoDatasetsInCollection() {
    DarCollectionSummary mockSummary = new DarCollectionSummary();
    mockSummary.setDatasetIds(Set.of(1));
    Integer collectionId = randomInt(1, 100);

    when(darCollectionService.findDatasetIdsByDACUser(member)).thenReturn(List.of(2));
    when(darCollectionService.getSummaryForRoleByCollectionId(
            member, UserRoles.MEMBER, collectionId))
        .thenReturn(mockSummary);

    Response response =
        resource.getCollectionSummaryForRoleById(
            duosMember, UserRoles.MEMBER.getRoleName(), collectionId);
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  void getCollectionSummaryForRoleById_Chair() {
    DarCollectionSummary mockSummary = new DarCollectionSummary();
    mockSummary.setDatasetIds(Set.of(1));
    Integer collectionId = randomInt(1, 100);

    when(darCollectionService.findDatasetIdsByDACUser(chairperson)).thenReturn(List.of(1, 2));
    when(darCollectionService.getSummaryForRoleByCollectionId(
            chairperson, UserRoles.CHAIRPERSON, collectionId))
        .thenReturn(mockSummary);

    Response response =
        resource.getCollectionSummaryForRoleById(
            duosChairperson, UserRoles.CHAIRPERSON.getRoleName(), collectionId);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void getCollectionSummaryForRoleById_ChairNoDatasetsInCollection() {
    DarCollectionSummary mockSummary = new DarCollectionSummary();
    mockSummary.setDatasetIds(Set.of(1));
    Integer collectionId = randomInt(1, 100);

    when(darCollectionService.findDatasetIdsByDACUser(chairperson)).thenReturn(List.of(2));
    when(darCollectionService.getSummaryForRoleByCollectionId(
            chairperson, UserRoles.CHAIRPERSON, collectionId))
        .thenReturn(mockSummary);

    Response response =
        resource.getCollectionSummaryForRoleById(
            duosChairperson, UserRoles.CHAIRPERSON.getRoleName(), collectionId);
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  void getCollectionSummaryForRoleById_SO() {
    DarCollectionSummary mockSummary = new DarCollectionSummary();
    signingOfficial.setInstitutionId(1);
    mockSummary.setInstitutionId(signingOfficial.getInstitutionId());
    Integer collectionId = randomInt(1, 100);

    when(darCollectionService.getSummaryForRoleByCollectionId(
            signingOfficial, UserRoles.SIGNINGOFFICIAL, collectionId))
        .thenReturn(mockSummary);

    Response response =
        resource.getCollectionSummaryForRoleById(
            duosSigningOfficial, UserRoles.SIGNINGOFFICIAL.getRoleName(), collectionId);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void getCollectionSummaryForRoleById_SODifferentInstitution() {
    DarCollectionSummary mockSummary = new DarCollectionSummary();
    mockSummary.setInstitutionId(2);
    Integer collectionId = randomInt(1, 100);

    when(darCollectionService.getSummaryForRoleByCollectionId(
            signingOfficial, UserRoles.SIGNINGOFFICIAL, collectionId))
        .thenReturn(mockSummary);

    Response response =
        resource.getCollectionSummaryForRoleById(
            duosSigningOfficial, UserRoles.SIGNINGOFFICIAL.getRoleName(), collectionId);
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  void getCollectionSummaryForRoleById_Researcher() {
    DarCollectionSummary mockSummary = new DarCollectionSummary();
    mockSummary.setResearcherId(researcher.getUserId());
    Integer collectionId = randomInt(1, 100);

    when(darCollectionService.getSummaryForRoleByCollectionId(
            researcher, UserRoles.RESEARCHER, collectionId))
        .thenReturn(mockSummary);

    Response response =
        resource.getCollectionSummaryForRoleById(
            duosResearcher, UserRoles.RESEARCHER.getRoleName(), collectionId);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void getCollectionSummaryForRoleById_ResearcherNotCreateUser() {
    DarCollectionSummary mockSummary = new DarCollectionSummary();
    mockSummary.setResearcherId(2);
    Integer collectionId = randomInt(1, 100);

    when(darCollectionService.getSummaryForRoleByCollectionId(
            researcher, UserRoles.RESEARCHER, collectionId))
        .thenReturn(mockSummary);

    Response response =
        resource.getCollectionSummaryForRoleById(
            duosResearcher, UserRoles.RESEARCHER.getRoleName(), collectionId);
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  void getCollectionSummaryForRoleById_Admin() {
    DarCollectionSummary mockSummary = new DarCollectionSummary();
    Integer collectionId = randomInt(1, 100);

    when(darCollectionService.getSummaryForRoleByCollectionId(admin, UserRoles.ADMIN, collectionId))
        .thenReturn(mockSummary);

    Response response =
        resource.getCollectionSummaryForRoleById(
            duosAdmin, UserRoles.ADMIN.getRoleName(), collectionId);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void getCollectionSummaryForRoleById_NoRoleFound() {
    User noRoleUser = new User();
    Integer collectionId = randomInt(1, 100);

    Response response =
        resource.getCollectionSummaryForRoleById(
            new DuosUser(authUser, noRoleUser),
            UserRoles.SIGNINGOFFICIAL.getRoleName(),
            collectionId);
    assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
  }

  @Test
  void getCollectionSummaryForRoleById_InvalidRoleString() {
    Integer collectionId = randomInt(1, 100);

    Response response =
        resource.getCollectionSummaryForRoleById(duosResearcher, "invalid", collectionId);
    assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
  }

  @Test
  void getCollectionSummaryForRoleById_CollectionNotFound() {
    Integer collectionId = randomInt(1, 100);

    when(darCollectionService.getSummaryForRoleByCollectionId(
            duosResearcher.getUser(), UserRoles.RESEARCHER, collectionId))
        .thenThrow(new NotFoundException());

    Response response =
        resource.getCollectionSummaryForRoleById(
            duosResearcher, UserRoles.RESEARCHER.getRoleName(), collectionId);
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }
}
