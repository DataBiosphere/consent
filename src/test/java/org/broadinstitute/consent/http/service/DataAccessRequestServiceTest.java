package org.broadinstitute.consent.http.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import jakarta.ws.rs.NotAcceptableException;
import jakarta.ws.rs.NotFoundException;
import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.db.DAOContainer;
import org.broadinstitute.consent.http.db.DacDAO;
import org.broadinstitute.consent.http.db.DarCollectionDAO;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.InstitutionDAO;
import org.broadinstitute.consent.http.db.MatchDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.enumeration.DarStatus;
import org.broadinstitute.consent.http.enumeration.HeaderDAR;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.DarCollection;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.LibraryCard;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.service.dao.DataAccessRequestServiceDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DataAccessRequestServiceTest {

  @Mock
  private CounterService counterService;
  @Mock
  private DataAccessRequestDAO dataAccessRequestDAO;
  @Mock
  private DarCollectionDAO darCollectionDAO;
  @Mock
  private DacDAO dacDAO;
  @Mock
  private UserDAO userDAO;
  @Mock
  private DatasetDAO dataSetDAO;
  @Mock
  private ElectionDAO electionDAO;
  @Mock
  private DacService dacService;
  @Mock
  private VoteDAO voteDAO;
  @Mock
  private InstitutionDAO institutionDAO;
  @Mock
  private MatchDAO matchDAO;
  @Mock
  private DataAccessRequestServiceDAO dataAccessRequestServiceDAO;
  @Mock
  private UseRestrictionConverter useRestrictionConverter;

  private DataAccessRequestService service;

  @BeforeEach
  public void setUp() {
    openMocks(this);
  }

  private void initService() {
    DAOContainer container = new DAOContainer();
    container.setDataAccessRequestDAO(dataAccessRequestDAO);
    container.setDarCollectionDAO(darCollectionDAO);
    container.setInstitutionDAO(institutionDAO);
    container.setDacDAO(dacDAO);
    container.setUserDAO(userDAO);
    container.setDatasetDAO(dataSetDAO);
    container.setElectionDAO(electionDAO);
    container.setVoteDAO(voteDAO);
    container.setMatchDAO(matchDAO);
    service = new DataAccessRequestService(counterService, container, dacService,
        dataAccessRequestServiceDAO, useRestrictionConverter);
  }

  @Test
  void testCreateDataAccessRequest_Update() {
    DataAccessRequest dar = generateDataAccessRequest();
    dar.addDatasetIds(List.of(1, 2, 3));
    User user = new User(1, "email@test.org", "Display Name", new Date());
    user.setLibraryCards(List.of(new LibraryCard()));
    when(counterService.getNextDarSequence()).thenReturn(1);
    when(dataAccessRequestDAO.findByReferenceId(any())).thenReturn(dar);
    doNothing().when(dataAccessRequestDAO)
        .updateDataByReferenceId(any(), any(), any(), any(), any(), any());
    initService();
    DataAccessRequest newDar = service.createDataAccessRequest(user, dar);
    assertNotNull(newDar);
  }

  @Test
  void testCreateDataAccessRequest_Create() {
    DataAccessRequest dar = generateDataAccessRequest();
    dar.addDatasetIds(List.of(1, 2, 3));
    dar.setCreateDate(new Timestamp(1000));
    dar.setSortDate(new Timestamp(1000));
    dar.setReferenceId("id");
    User user = new User(1, "email@test.org", "Display Name", new Date());
    user.setLibraryCards(List.of(new LibraryCard()));
    user.setLibraryCards(List.of(new LibraryCard()));
    when(counterService.getNextDarSequence()).thenReturn(1);
    when(dataAccessRequestDAO.findByReferenceId("id")).thenReturn(null);
    when(dataAccessRequestDAO.findByReferenceId(argThat(new LongerThanTwo()))).thenReturn(dar);
    when(darCollectionDAO.insertDarCollection(anyString(), anyInt(), any(Date.class))).thenReturn(
        RandomUtils.nextInt(1, 100));
    doNothing().when(dataAccessRequestDAO)
        .insertDataAccessRequest(anyInt(), anyString(), anyInt(), any(Date.class), any(Date.class),
            any(Date.class), any(Date.class), any(DataAccessRequestData.class));
    initService();
    DataAccessRequest newDar = service.createDataAccessRequest(user, dar);
    assertNotNull(newDar);
  }

  @Test
  void testCreateDataAccessRequest_FailsIfNoLibraryCard() {
    DataAccessRequest dar = generateDataAccessRequest();
    dar.addDatasetIds(List.of(1, 2, 3));
    dar.setCreateDate(new Timestamp(1000));
    dar.setSortDate(new Timestamp(1000));
    dar.setReferenceId("id");
    User user = new User(1, "email@test.org", "Display Name", new Date());
    user.setLibraryCards(List.of(new LibraryCard()));
    user.setLibraryCards(List.of());
    initService();
    assertThrows(IllegalArgumentException.class, () -> {
      service.createDataAccessRequest(user, dar);
    });
  }

  @Test
  void testUpdateByReferenceIdVersion2() throws Exception {
    DataAccessRequest dar = generateDataAccessRequest();
    dar.setCollectionId(RandomUtils.nextInt(0, 100));
    User user = new User(1, "email@test.org", "Display Name", new Date());
    dar.addDatasetIds(List.of(1, 2, 3));
    when(dataAccessRequestServiceDAO.updateByReferenceId(any(), any())).thenReturn(dar);
    initService();
    DataAccessRequest newDar = service.updateByReferenceId(user, dar);
    assertNotNull(newDar);
  }

  @Test
  void testUpdateByReferenceIdVersion2_WithCollection() throws Exception {
    DataAccessRequest dar = generateDataAccessRequest();
    User user = new User(1, "email@test.org", "Display Name", new Date());
    dar.addDatasetIds(List.of(1, 2, 3));
    when(dataAccessRequestServiceDAO.updateByReferenceId(user, dar)).thenReturn(dar);
    initService();
    DataAccessRequest newDar = service.updateByReferenceId(user, dar);
    assertNotNull(newDar);
  }

  @Test
  void testGetUsersApprovedForDataset() {
    Dataset d = new Dataset();
    d.setDataSetId(10);

    User user1 = new User();
    user1.setUserId(10);
    User user2 = new User();
    user2.setUserId(20);

    DataAccessRequest dar1 = new DataAccessRequest();
    dar1.setUserId(10);
    DataAccessRequest dar2 = new DataAccessRequest();
    dar2.setUserId(20);
    when(dataAccessRequestDAO
        .findApprovedDARsByDatasetId(d.getDataSetId()))
        .thenReturn(List.of(dar1, dar2));
    initService();

    assertEquals(List.of(dar1, dar2), service.getApprovedDARsForDataset(d));
  }

  @Test
  void testInsertDraftDataAccessRequest() {
    User user = new User();
    user.setUserId(1);
    DataAccessRequest draft = generateDataAccessRequest();
    doNothing()
        .when(dataAccessRequestDAO)
        .insertDraftDataAccessRequest(any(), any(), any(), any(), any(), any(), any());
    when(dataAccessRequestDAO.findByReferenceId(any())).thenReturn(draft);
    initService();
    DataAccessRequest dar = service.insertDraftDataAccessRequest(user, draft);
    assertNotNull(dar);
  }

  @Test
  void testInsertDraftDataAccessRequestFailure() {
    initService();
    assertThrows(IllegalArgumentException.class, () -> {
      DataAccessRequest dar = service.insertDraftDataAccessRequest(null, null);
      assertNotNull(dar);
    });
  }

  @Test
  void testCreateApprovedDARDocument() {
    Election election = generateElection(1);
    when(electionDAO.findDataAccessClosedElectionsByFinalResult(any()))
        .thenReturn(Collections.singletonList(election));
    DataAccessRequest dar = generateDataAccessRequest();
    dar.setUserId(1);
    DarCollection collection = new DarCollection();
    Map<String, DataAccessRequest> dars = new HashMap<>();
    dars.put(election.getReferenceId(), dar);
    collection.setDars(dars);
    Institution institution = new Institution();
    institution.setName("Institution");
    when(dataAccessRequestDAO.findByReferenceId(any())).thenReturn(dar);
    when(darCollectionDAO.findDARCollectionByReferenceId(any())).thenReturn(collection);
    initService();
    try {
      File file = service.createApprovedDARDocument();
      assertNotNull(file);
    } catch (IOException ioe) {
      assert false;
    }
  }

  @Test
  void testCreateReviewedDARDocument() {
    Election election = generateElection(1);
    election.setFinalVote(true);
    election.setFinalVoteDate(new Date());
    when(electionDAO.findDataAccessClosedElectionsByFinalResult(true))
        .thenReturn(Collections.singletonList(election));
    when(electionDAO.findDataAccessClosedElectionsByFinalResult(false))
        .thenReturn(Collections.emptyList());
    DataAccessRequest dar = generateDataAccessRequest();
    dar.setUserId(1);
    DarCollection collection = new DarCollection();
    Map<String, DataAccessRequest> dars = new HashMap<>();
    dars.put(election.getReferenceId(), dar);
    collection.setDars(dars);
    Dataset d = new Dataset();
    d.setDataSetId(1);
    d.setDataUse(new DataUseBuilder().setHmbResearch(true).build());
    dar.setDatasetIds(List.of(1));
    when(dataAccessRequestDAO.findByReferenceId(any())).thenReturn(dar);
    when(darCollectionDAO.findDARCollectionByReferenceId(any())).thenReturn(collection);
    when(dataSetDAO.findDatasetById(any())).thenReturn(d);
    when(dataSetDAO.findDatasetsByIdList(any())).thenReturn(List.of(d));
    when(useRestrictionConverter.translateDataUse(any(), any())).thenReturn("Use is limited to research");

    initService();

    try {
      File file = service.createReviewedDARDocument();

      assertNotNull(file);
    } catch (Exception e) {
      assert false;
    }
  }

  @Test
  void testCreateDatasetApprovedUsersContentAsNonPrivilegedUser() {
    DataAccessRequest dar = generateDataAccessRequest();
    dar.setUserId(1);
    User user = new User();
    user.setUserId(1);
    user.setDisplayName("displayName");
    user.setInstitutionId(1);
    Institution institution = new Institution();
    institution.setName("Institution");
    when(userDAO.findUserByEmail(any())).thenReturn(user);

    initService();

    try {
      String approvedUsers = service.getDatasetApprovedUsersContent(new AuthUser(), 1);
      System.out.println(approvedUsers);
      assertNotNull(approvedUsers);
      assertFalse(approvedUsers.contains(HeaderDAR.USERNAME.getValue()));
    } catch (Exception ioe) {
      assert false;
    }
  }

  @Test
  void testCreateDatasetApprovedUsersContentAsPrivilegedUser() {
    DataAccessRequest dar = generateDataAccessRequest();
    dar.setUserId(1);
    User user = new User();
    UserRole userRole = new UserRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName());
    user.addRole(userRole);
    user.setUserId(1);
    user.setDisplayName("displayName");
    user.setInstitutionId(1);
    Institution institution = new Institution();
    institution.setName("Institution");
    when(userDAO.findUserByEmail(any())).thenReturn(user);

    initService();

    try {
      String approvedUsers = service.getDatasetApprovedUsersContent(new AuthUser(), 1);
      System.out.println(approvedUsers);
      assertNotNull(approvedUsers);
      assertTrue(approvedUsers.contains(HeaderDAR.USERNAME.getValue()));
    } catch (Exception ioe) {
      assert false;
    }
  }

  private DataAccessRequest generateDataAccessRequest() {
    DataAccessRequest dar = new DataAccessRequest();
    DataAccessRequestData data = new DataAccessRequestData();
    Integer userId = userDAO.insertUser(UUID.randomUUID().toString(), "displayName", new Date());
    dar.setUserId(userId);
    dar.setReferenceId(UUID.randomUUID().toString());
    data.setReferenceId(dar.getReferenceId());
    dar.addDatasetId(1);
    data.setForProfit(false);
    data.setAddiction(false);
    data.setAnvilUse(true);
    data.setCheckCollaborator(false);
    data.setCloudUse(true);
    data.setCloudProvider("Google Cloud");
    data.setCloudProviderDescription("Google");
    data.setControls(false);
    data.setOneGender(false);
    data.setPediatric(false);
    data.setHmb(false);
    data.setDiseases(false);
    data.setSexualDiseases(false);
    data.setPoa(false);
    data.setIllegalBehavior(false);
    data.setProjectTitle("Title");
    data.setStigmatizedDiseases(false);
    data.setVulnerablePopulation(false);
    data.setPopulation(false);
    data.setPopulationMigration(true);
    data.setPsychiatricTraits(false);
    data.setNotHealth(true);
    data.setOntologies(Collections.emptyList());
    data.setMethods(false);
    data.setOther(false);
    dar.setData(data);
    return dar;
  }

  private Election generateElection(Integer datasetId) {
    String refId = UUID.randomUUID().toString();
    Election election = new Election();
    election.setDataSetId(datasetId);
    election.setReferenceId(refId);

    return election;
  }

  @Test
  void testFindAllDraftDataAccessRequests() {
    when(dataAccessRequestDAO.findAllDraftDataAccessRequests()).thenReturn(
        List.of(new DataAccessRequest()));
    initService();
    List<DataAccessRequest> drafts = service.findAllDraftDataAccessRequests();
    assertEquals(drafts.size(), 1);
  }

  @Test
  void testFindAllDraftDataAccessRequestsByUser() {
    when(dataAccessRequestDAO.findAllDraftsByUserId(any())).thenReturn(
        List.of(new DataAccessRequest()));
    initService();
    List<DataAccessRequest> drafts = service.findAllDraftDataAccessRequestsByUser(1);
    assertEquals(drafts.size(), 1);
  }

  @Test
  void getDataAccessRequestsForUser() {
    List<DataAccessRequest> dars = List.of(new DataAccessRequest());
    when(dataAccessRequestDAO.findAllDataAccessRequests()).thenReturn(dars);
    when(dacService.filterDataAccessRequestsByDac(eq(dars), any())).thenReturn(dars);
    initService();
    List<DataAccessRequest> foundDars = service.getDataAccessRequestsByUserRole(new User());
    assertEquals(foundDars.size(), 1);
  }

  @Test
  void testFindByReferenceId() {
    initService();
    DataAccessRequest dar = new DataAccessRequest();
    when(dataAccessRequestDAO.findByReferenceId(any())).thenReturn(dar);
    DataAccessRequest foundDar = service.findByReferenceId("refId");
    assertEquals(dar, foundDar);
  }

  @Test
  void testFindByReferenceId_NotFound() {
    initService();
    when(dataAccessRequestDAO.findByReferenceId(any())).thenThrow(new NotFoundException());
    assertThrows(NotFoundException.class, () -> {
      service.findByReferenceId("referenceId");
    });
  }

  @Test
  void testCreateDraftDarFromCanceledCollection_NoDars() {
    User user = new User();
    DarCollection sourceCollection = new DarCollection();
    initService();
    assertThrows(IllegalArgumentException.class, () -> {
      service.createDraftDarFromCanceledCollection(user, sourceCollection);
    });
  }

  @Test
  void testCreateDraftDarFromCanceledCollection_NoDarData() {
    User user = new User();
    DarCollection sourceCollection = new DarCollection();
    DataAccessRequest newDar = new DataAccessRequest();
    newDar.setReferenceId(UUID.randomUUID().toString());
    sourceCollection.addDar(newDar);
    initService();
    assertThrows(IllegalArgumentException.class, () -> {
      service.createDraftDarFromCanceledCollection(user, sourceCollection);
    });
  }

  @Test
  void testCreateDraftDarFromCanceledCollection_NoCanceledDars() {
    User user = new User();
    DarCollection sourceCollection = new DarCollection();
    DataAccessRequest dar = new DataAccessRequest();
    DataAccessRequestData data = new DataAccessRequestData();
    data.setReferenceId(UUID.randomUUID().toString());
    data.setStatus("Not Canceled");
    dar.setData(data);
    dar.setReferenceId(data.getReferenceId());
    sourceCollection.addDar(dar);
    initService();
    assertThrows(IllegalArgumentException.class, () -> {
      service.createDraftDarFromCanceledCollection(user, sourceCollection);
    });
  }

  @Test
  void testCreateDraftDarFromCanceledCollection_NoDatasets() {
    User user = new User();
    DarCollection sourceCollection = new DarCollection();
    DataAccessRequest dar = new DataAccessRequest();
    DataAccessRequestData data = new DataAccessRequestData();
    data.setReferenceId(UUID.randomUUID().toString());
    data.setStatus(DarStatus.CANCELED.getValue());
    dar.setData(data);
    dar.setDatasetIds(null);
    dar.setReferenceId(data.getReferenceId());
    sourceCollection.addDar(dar);
    initService();
    assertThrows(IllegalArgumentException.class, () -> {
      service.createDraftDarFromCanceledCollection(user, sourceCollection);
    });
  }

  @Test
  void testCreateDraftDarFromCanceledCollection_OpenElectionsOnCanceledDars() {
    User user = new User();
    DarCollection sourceCollection = new DarCollection();
    DataAccessRequest dar = new DataAccessRequest();
    DataAccessRequestData data = new DataAccessRequestData();
    data.setStatus(DarStatus.CANCELED.getValue());
    dar.addDatasetId(1);
    data.setReferenceId(UUID.randomUUID().toString());
    dar.setData(data);
    dar.setReferenceId(data.getReferenceId());
    sourceCollection.addDar(dar);
    when(electionDAO.getElectionIdsByReferenceIds(any())).thenReturn(List.of(1));
    initService();
    assertThrows(IllegalArgumentException.class, () -> {
      service.createDraftDarFromCanceledCollection(user, sourceCollection);
    });
  }

  @Test
  void testCreateDraftDarFromCanceledCollection() {
    User user = new User();
    DarCollection sourceCollection = new DarCollection();
    DataAccessRequest dar = new DataAccessRequest();
    DataAccessRequestData data = new DataAccessRequestData();
    data.setStatus(DarStatus.CANCELED.getValue());
    dar.addDatasetId(1);
    data.setReferenceId(UUID.randomUUID().toString());
    dar.setData(data);
    dar.setReferenceId(data.getReferenceId());
    sourceCollection.addDar(dar);
    when(electionDAO.getElectionIdsByReferenceIds(any())).thenReturn(List.of());
    doNothing().when(dataAccessRequestDAO).insertDraftDataAccessRequest(
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any()
    );
    when(dataAccessRequestDAO.findByReferenceId(any())).thenReturn(new DataAccessRequest());
    initService();
    service.createDraftDarFromCanceledCollection(user, sourceCollection);
  }

  @Test
  void testDeleteByReferenceIdAdmin() {
    String referenceId = UUID.randomUUID().toString();
    User adminUser = new User();
    adminUser.addRole(new UserRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName()));
    Election election = new Election();
    election.setElectionId(1);
    election.setReferenceId(referenceId);
    when(electionDAO.findElectionsByReferenceId(any())).thenReturn(List.of(election));
    doNothing().when(voteDAO).deleteVotesByReferenceId(any());
    doNothing().when(matchDAO).deleteMatchesByPurposeId(any());
    doNothing().when(dataAccessRequestDAO).deleteByReferenceId(any());
    initService();

    try {
      service.deleteByReferenceId(adminUser, referenceId);
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  void testDeleteByReferenceIdResearcherSuccess() {
    String referenceId = UUID.randomUUID().toString();
    User user = new User();
    user.addRole(
        new UserRole(UserRoles.RESEARCHER.getRoleId(), UserRoles.RESEARCHER.getRoleName()));
    when(electionDAO.findElectionsByReferenceId(any())).thenReturn(List.of());
    doNothing().when(matchDAO).deleteMatchesByPurposeId(any());
    doNothing().when(dataAccessRequestDAO).deleteByReferenceId(any());
    doNothing().when(dataAccessRequestDAO).deleteDARDatasetRelationByReferenceId(any());
    initService();

    service.deleteByReferenceId(user, referenceId);
  }

  @Test
  void testDeleteByReferenceIdResearcherFailure() {
    String referenceId = UUID.randomUUID().toString();
    User user = new User();
    user.addRole(
        new UserRole(UserRoles.RESEARCHER.getRoleId(), UserRoles.RESEARCHER.getRoleName()));
    Election election = new Election();
    election.setElectionId(1);
    election.setReferenceId(referenceId);
    when(electionDAO.findElectionsByReferenceId(any())).thenReturn(List.of(election));
    initService();

    assertThrows(NotAcceptableException.class, () -> {
      service.deleteByReferenceId(user, referenceId);
    });
  }

  private static class LongerThanTwo implements ArgumentMatcher<String> {

    @Override
    public boolean matches(String argument) {
      return argument.length() > 2;
    }
  }
}
