package org.broadinstitute.consent.http.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotAcceptableException;
import jakarta.ws.rs.NotFoundException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;
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
import org.broadinstitute.consent.http.exceptions.NIHComplianceRuleException;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.exceptions.SubmittedDARCannotBeEditedException;
import org.broadinstitute.consent.http.models.Collaborator;
import org.broadinstitute.consent.http.models.DarDataset;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.LibraryCard;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.service.dao.DataAccessRequestServiceDAO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DataAccessRequestServiceTest {

  private static final String PI_EMAIL = "pi@example.broadinstitute.org";
  private static final String SO_EMAIL = "so@example.broadinstitute.org";
  private static final String IT_EMAIL = "it@example.broadinstitute.org";
  private static final int APPROVED_PROGRESS_REPORT_DATASET_ID = 1;
  private final List<UserRole> roles = List.of(UserRoles.Researcher());
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
  private UserService userService;
  private DataAccessRequestService service;

  private static Collaborator createCollaborator() {
    Collaborator validCollaborator = new Collaborator();
    validCollaborator.setEmail("collaborator@test.com");
    return validCollaborator;
  }

  private static DataAccessRequest createDataAccessRequest(
      List<Collaborator> internalCollaborators) {
    DataAccessRequestData data = new DataAccessRequestData();
    data.setInternalCollaborators(internalCollaborators);
    DataAccessRequest dar = new DataAccessRequest();
    dar.setData(data);
    return dar;
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
        dataAccessRequestServiceDAO, userService);
  }

  @Test
  void testCreateDataAccessRequest_Update() {
    DataAccessRequest dar = generateDataAccessRequest();
    dar.setCollectionId(null);
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
  void testCreateDataAccessRequest_CreateWithSubmittedDar() {
    DataAccessRequest dar = generateDataAccessRequest();
    dar.addDatasetIds(List.of(1, 2, 3));
    dar.setCreateDate(new Timestamp(1000));
    dar.setSortDate(new Timestamp(1000));
    dar.setReferenceId("id");
    dar.setSubmissionDate(Timestamp.from(Instant.now()));
    User user = new User(1, "email@test.org", "Display Name", new Date());
    user.setLibraryCards(List.of(new LibraryCard()));
    when(dataAccessRequestDAO.findByReferenceId(any())).thenReturn(dar);
    initService();
    assertThrows(SubmittedDARCannotBeEditedException.class, () -> {
      service.createDataAccessRequest(user, dar);
    });
  }

  @Test
  void testCreateDataAccessRequestCreateWithoutERACommons() {
    DataAccessRequest dar = generateDataAccessRequest();
    User user = new User(1, "email@test.org", "Display Name", new Date());
    user.setLibraryCards(List.of(new LibraryCard()));
    doThrow(BadRequestException.class).when(userService).hasValidActiveERACredentials(user);
    initService();
    assertThrows(BadRequestException.class, () -> {
      service.createDataAccessRequest(user, dar);
    });
  }


  @Test
  void testUpdateByReferenceIdThrowsOnDraft() throws Exception {
    DataAccessRequest dar = generateDataAccessRequest();
    dar.setCollectionId(RandomUtils.nextInt(0, 100));
    User user = new User(1, "email@test.org", "Display Name", new Date());
    dar.addDatasetIds(List.of(1, 2, 3));
    dar.setSubmissionDate(Timestamp.from(Instant.now()));
    initService();
    assertThrows(SubmittedDARCannotBeEditedException.class, () ->
        service.updateByReferenceId(user, dar)
    );
  }

  @Test
  void testCreateDataAccessRequest_FailsIfNoLibraryCard() {
    DataAccessRequest dar = generateDataAccessRequest();
    dar.addDatasetIds(List.of(1, 2, 3));
    dar.setCreateDate(new Timestamp(1000));
    dar.setSortDate(new Timestamp(1000));
    dar.setReferenceId("id");
    User user = new User(1, "email@test.org", "Display Name", new Date());
    user.setLibraryCards(List.of());
    initService();
    assertThrows(NIHComplianceRuleException.class, () -> {
      service.createDataAccessRequest(user, dar);
    });
  }

  @Test
  void createProgressReport() {
    DataAccessRequest parentDar = generateDataAccessRequest();
    DataAccessRequest progressReport = generateProgressReport();
    progressReport.setParentId(parentDar.getId().toString());
    progressReport.setCollectionId(parentDar.getCollectionId());
    parentDar.setSubmissionDate(Timestamp.from(Instant.now()));
    User user = new User(1, "email@test.org", "Display Name", new Date());
    user.setLibraryCards(List.of(new LibraryCard()));
    parentDar.setUserId(user.getUserId());
    when(dataAccessRequestDAO.findByReferenceId(progressReport.getReferenceId())).thenReturn(progressReport);
    when(dataAccessRequestDAO.findApprovedDatasetsByDar(parentDar.getReferenceId())).thenReturn(List.of(APPROVED_PROGRESS_REPORT_DATASET_ID));

    initService();
    DataAccessRequest newDar = service.createProgressReport(user, progressReport, parentDar);
    assertNotNull(newDar);
    verify(dataAccessRequestDAO)
        .insertProgressReport(parentDar.getId(), progressReport.getCollectionId(), progressReport.getReferenceId(), user.getUserId(),
            progressReport.getData());
    verify(dataAccessRequestDAO).insertAllDarDatasets(argThat(new DarDatasetMatcher(progressReport)));
  }

  @Test
  void createProgressReportFailsIfNonApprovedDatasets() {
    DataAccessRequest parentDar = generateDataAccessRequest();
    DataAccessRequest progressReport = generateProgressReport();
    progressReport.setParentId(parentDar.getId().toString());
    progressReport.setCollectionId(parentDar.getCollectionId());
    parentDar.setSubmissionDate(Timestamp.from(Instant.now()));
    User user = new User(1, "email@test.org", "Display Name", new Date());
    user.setLibraryCards(List.of(new LibraryCard()));
    parentDar.setUserId(user.getUserId());
    when(dataAccessRequestDAO.findApprovedDatasetsByDar(parentDar.getReferenceId())).thenReturn(List.of());
    initService();
    assertThrows(BadRequestException.class, () -> service.createProgressReport(user, progressReport, parentDar));
  }

  static class DarDatasetMatcher implements ArgumentMatcher<List<DarDataset>> {
    private final DataAccessRequest progressReport;

    public DarDatasetMatcher(DataAccessRequest progressReport) {
      this.progressReport = progressReport;
    }
    @Override
    public boolean matches(List<DarDataset> darDatasets) {
      for (int i=0; i < darDatasets.size(); i++) {
        if (!darDatasets.get(i).getReferenceId().equals(progressReport.getReferenceId()) ||
            !darDatasets.get(i).getDatasetId().equals(progressReport.getDatasetIds().get(i))) {
          return false;
        }
      }
      return true;
    }
  }

  private User createRequestingUser() {
    User requestingUser = new User(1, "requestor@test.com", "Requestor", new Date(), roles);
    requestingUser.setInstitutionId(1);
    Institution institution = new Institution();
    institution.setName("Test Institution");
    requestingUser.setInstitution(institution);
    return requestingUser;
  }

  @Test
  void validateProgressReportParentDarIsDraft() {
    User user = new User(1, "email@test.org", "Display Name", new Date());
    user.setLibraryCards(List.of(new LibraryCard()));
    DataAccessRequest progressReport = generateProgressReport();
    DataAccessRequest parentDar = generateDataAccessRequest();
    initService();
    assertThrows(BadRequestException.class, () -> {
      service.validateProgressReport(user, progressReport, parentDar);
    });
  }

  @Test
  void validateProgressReportNoDatasetIds() {
    User user = new User(1, "email@test.org", "Display Name", new Date());
    user.setLibraryCards(List.of(new LibraryCard()));
    DataAccessRequest progressReport = generateProgressReport();
    progressReport.setDatasetIds(Collections.emptyList());
    DataAccessRequest parentDar = generateDataAccessRequest();
    parentDar.setSubmissionDate(Timestamp.from(Instant.now()));
    parentDar.setUserId(user.getUserId());
    initService();
    assertThrows(BadRequestException.class, () -> {
      service.validateProgressReport(user, progressReport, parentDar);
    });
  }

  @Test
  void validateProgressReportNoSummary() {
    User user = new User(1, "email@test.org", "Display Name", new Date());
    user.setLibraryCards(List.of(new LibraryCard()));
    DataAccessRequest progressReport = generateProgressReport();
    progressReport.getData().setProgressReportSummary(null);
    DataAccessRequest parentDar = generateDataAccessRequest();
    parentDar.setSubmissionDate(Timestamp.from(Instant.now()));
    parentDar.setUserId(user.getUserId());
    initService();
    assertThrows(BadRequestException.class, () -> {
      service.validateProgressReport(user, progressReport, parentDar);
    });
  }

  @Test
  void validateProgressReportNoIPSummary() {
    User user = new User(1, "email@test.org", "Display Name", new Date());
    user.setLibraryCards(List.of(new LibraryCard()));
    DataAccessRequest progressReport = generateProgressReport();
    progressReport.getData().setIntellectualPropertySummary(null);
    DataAccessRequest parentDar = generateDataAccessRequest();
    parentDar.setSubmissionDate(Timestamp.from(Instant.now()));
    parentDar.setUserId(user.getUserId());
    initService();
    assertThrows(BadRequestException.class, () -> {
      service.validateProgressReport(user, progressReport, parentDar);
    });
  }

  @Test
  void validateProgressReportInvalidDatasetIds() {
    User user = new User(1, "email@test.org", "Display Name", new Date());
    user.setLibraryCards(List.of(new LibraryCard()));
    DataAccessRequest progressReport = generateProgressReport();
    progressReport.setDatasetIds(List.of(3, 4, 5)); // IDs not all in parent DAR
    DataAccessRequest parentDar = generateDataAccessRequest();
    parentDar.setSubmissionDate(Timestamp.from(Instant.now()));
    parentDar.setDatasetIds(List.of(1, 2, 3));
    parentDar.setUserId(user.getUserId());
    initService();
    assertThrows(BadRequestException.class, () -> {
      service.validateProgressReport(user, progressReport, parentDar);
    });
  }

  @Test
  void validateProgressReport() {
    User user = new User(1, "email@test.org", "Display Name", new Date());
    user.setLibraryCards(List.of(new LibraryCard()));
    DataAccessRequest progressReport = generateProgressReport();
    progressReport.setDatasetIds(List.of(1, 2));
    DataAccessRequest parentDar = generateDataAccessRequest();
    parentDar.setSubmissionDate(Timestamp.from(Instant.now()));
    parentDar.setDatasetIds(List.of(1, 2, 3));
    parentDar.setUserId(user.getUserId());
    initService();
    assertDoesNotThrow(() -> {
      service.validateProgressReport(user, progressReport, parentDar);
    });
  }

  @Test
  void validateDarNullUser() {
    DataAccessRequest dar = generateDataAccessRequest();
    initService();
    assertThrows(IllegalArgumentException.class, () -> {
      service.validateDar(null, dar);
    });
  }

  @Test
  void validateDarNullDar() {
    User user = new User(1, "email@test.org", "Display Name", new Date());
    initService();
    assertThrows(IllegalArgumentException.class, () -> {
      service.validateDar(user, null);
    });
  }

  @Test
  void validateDarNullReferenceId() {
    DataAccessRequest dar = generateDataAccessRequest();
    dar.setReferenceId(null);
    User user = new User(1, "email@test.org", "Display Name", new Date());
    initService();
    assertThrows(IllegalArgumentException.class, () -> {
      service.validateDar(user, dar);
    });
  }

  @Test
  void validateDarNullData() {
    DataAccessRequest dar = generateDataAccessRequest();
    dar.setData(null);
    User user = new User(1, "email@test.org", "Display Name", new Date());
    initService();
    assertThrows(IllegalArgumentException.class, () -> {
      service.validateDar(user, dar);
    });
  }

  @Test
  void validateDarNoLibraryCards() {
    DataAccessRequest dar = generateDataAccessRequest();
    User user = new User(1, "email@test.org", "Display Name", new Date());
    user.setLibraryCards(Collections.emptyList());
    initService();
    assertThrows(NIHComplianceRuleException.class, () -> {
      service.validateDar(user, dar);
    });
  }

  @Test
  void validateDar() {
    DataAccessRequest dar = generateDataAccessRequest();
    User user = new User(1, "email@test.org", "Display Name", new Date());
    user.setLibraryCards(List.of(new LibraryCard()));
    initService();
    assertDoesNotThrow(() -> {
      service.validateDar(user, dar);
    });
  }

  @Test
  void testValidateInternalCollaboratorsNone() {
    User requestingUser = createRequestingUser();
    DataAccessRequest dar = createDataAccessRequest(List.of());
    initService();
    assertDoesNotThrow(() -> service.validateInternalCollaborators(dar, requestingUser));
  }

  @Test
  void testValidateInternalCollaboratorsValid() {
    User requestingUser = createRequestingUser();
    Collaborator validCollaborator = createCollaborator();
    User collaboratorUser = new User(2, validCollaborator.getEmail(), "Collaborator", new Date(),
        roles);
    collaboratorUser.setInstitutionId(requestingUser.getInstitutionId());
    LibraryCard libraryCard = new LibraryCard();
    libraryCard.setInstitutionId(requestingUser.getInstitutionId());
    collaboratorUser.setLibraryCards(List.of(libraryCard));
    DataAccessRequest dar = createDataAccessRequest(List.of(validCollaborator));
    when(userDAO.findUserByEmail(validCollaborator.getEmail())).thenReturn(collaboratorUser);

    initService();
    assertDoesNotThrow(() -> service.validateInternalCollaborators(dar, requestingUser));
  }

  @Test
  void testValidateInternalCollaboratorsDoesNotExist() {
    User requestingUser = createRequestingUser();
    Collaborator invalidCollaborator = createCollaborator();
    DataAccessRequest dar = createDataAccessRequest(List.of(invalidCollaborator));
    when(userDAO.findUserByEmail(invalidCollaborator.getEmail())).thenReturn(null);

    initService();
    NotFoundException exception = assertThrows(NotFoundException.class, () ->
        service.validateInternalCollaborators(dar, requestingUser));
    assertEquals(exception.getMessage(),
        "Unable to find User with the provided email: " + invalidCollaborator.getEmail());
  }

  @Test
  void testValidateInternalCollaboratorsDifferentInstitution() {
    User requestingUser = createRequestingUser();
    Collaborator invalidCollaborator = createCollaborator();
    User collaboratorUser = new User(2, invalidCollaborator.getEmail(), "Collaborator", new Date(),
        roles);
    collaboratorUser.setInstitutionId(2);
    DataAccessRequest dar = createDataAccessRequest(List.of(invalidCollaborator));
    when(userDAO.findUserByEmail(invalidCollaborator.getEmail())).thenReturn(collaboratorUser);

    initService();
    BadRequestException exception = assertThrows(BadRequestException.class, () ->
        service.validateInternalCollaborators(dar, requestingUser)
    );
    assertEquals(exception.getMessage(), "Collaborator " + invalidCollaborator.getEmail()
        + " is not part of the same institution, Test Institution");
  }

  @Test
  void testValidateInternalCollaboratorsNoLibraryCard() {
    User requestingUser = createRequestingUser();
    Collaborator invalidCollaborator = createCollaborator();
    User collaboratorUser = new User(2, invalidCollaborator.getEmail(), "Collaborator", new Date(),
        roles);
    collaboratorUser.setInstitutionId(requestingUser.getInstitutionId());
    collaboratorUser.setLibraryCards(Collections.emptyList());
    DataAccessRequest dar = createDataAccessRequest(List.of(invalidCollaborator));
    when(userDAO.findUserByEmail(invalidCollaborator.getEmail())).thenReturn(collaboratorUser);

    initService();
    BadRequestException exception = assertThrows(BadRequestException.class, () ->
        service.validateInternalCollaborators(dar, requestingUser)
    );
    assertEquals(exception.getMessage(),
        "Collaborator " + invalidCollaborator.getEmail() + " does not have a library card.");
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
    d.setDatasetId(10);

    User user1 = new User();
    user1.setUserId(10);
    User user2 = new User();
    user2.setUserId(20);

    DataAccessRequest dar1 = new DataAccessRequest();
    dar1.setUserId(10);
    DataAccessRequest dar2 = new DataAccessRequest();
    dar2.setUserId(20);
    when(dataAccessRequestDAO
        .findApprovedDARsByDatasetId(d.getDatasetId()))
        .thenReturn(List.of(dar1, dar2));
    initService();

    assertEquals(List.of(dar1, dar2), service.getApprovedDARsForDataset(d));
  }

  @Test
  void testInsertDraftDataAccessRequest() {
    User user = new User();
    user.setUserId(1);
    user.setLibraryCards(List.of(new LibraryCard()));
    DataAccessRequest draft = generateDataAccessRequest();
    doNothing()
        .when(dataAccessRequestDAO)
        .insertDraftDataAccessRequest(any(), any(), any(), any(), any(), any());
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

  private DataAccessRequest generateProgressReport() {
    DataAccessRequest progressReport = generateDataAccessRequest();
    progressReport.setDatasetIds(List.of(APPROVED_PROGRESS_REPORT_DATASET_ID));
    progressReport.getData().setProgressReportSummary("Progress Report Summary");
    progressReport.getData().setIntellectualPropertySummary("Intellectual Property Summary");
    return progressReport;
  }

  private DataAccessRequest generateDataAccessRequest() {
    DataAccessRequest dar = new DataAccessRequest();
    DataAccessRequestData data = new DataAccessRequestData();
    dar.setId(new Random().nextInt());
    dar.setCollectionId(new Random().nextInt());
    dar.setReferenceId(UUID.randomUUID().toString());
    data.setReferenceId(dar.getReferenceId());
    dar.addDatasetId(1);
    data.setPiEmail(PI_EMAIL);
    data.setItDirectorEmail(IT_EMAIL);
    data.setSigningOfficialEmail(SO_EMAIL);
    data.setForProfit(false);
    data.setAddiction(false);
    data.setAnvilUse(true);
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
    dar.setSubmissionDate(null);
    return dar;
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
  void testDeleteByReferenceIdAdmin() {
    String referenceId = UUID.randomUUID().toString();
    User adminUser = new User();
    adminUser.setAdminRole();
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
    user.setResearcherRole();
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
    user.setResearcherRole();
    Election election = new Election();
    election.setElectionId(1);
    election.setReferenceId(referenceId);
    when(electionDAO.findElectionsByReferenceId(any())).thenReturn(List.of(election));
    initService();

    assertThrows(NotAcceptableException.class, () -> {
      service.deleteByReferenceId(user, referenceId);
    });
  }

  @Test
  void testValidateNoKeyPersonnelDuplicates() {
    DataAccessRequestData data = new DataAccessRequestData();
    data.setPiEmail(PI_EMAIL);
    data.setItDirectorEmail(IT_EMAIL);
    data.setSigningOfficialEmail(SO_EMAIL);
    initService();
    try {
      service.validateNoKeyPersonnelDuplicates(data);
    } catch (IllegalArgumentException e) {
      fail("Should not have thrown exception");
    }
  }

  @Test
  void testValidateNoKeyPersonnelDuplicatesBadPIEmail() {
    DataAccessRequestData data = new DataAccessRequestData();
    data.setPiEmail("invalid");
    data.setItDirectorEmail(IT_EMAIL);
    data.setSigningOfficialEmail(SO_EMAIL);
    initService();
    assertThrows(IllegalArgumentException.class, () -> {
      service.validateNoKeyPersonnelDuplicates(data);
    });
  }

  @Test
  void testValidateNoKeyPersonnelDuplicatesBadITDirectorEmail() {
    DataAccessRequestData data = new DataAccessRequestData();
    data.setPiEmail(PI_EMAIL);
    data.setItDirectorEmail("invalid");
    data.setSigningOfficialEmail(SO_EMAIL);
    initService();
    assertThrows(IllegalArgumentException.class, () -> {
      service.validateNoKeyPersonnelDuplicates(data);
    });
  }

  @Test
  void testValidateNoKeyPersonnelDuplicatesBadSO() {
    DataAccessRequestData data = new DataAccessRequestData();
    data.setPiEmail(PI_EMAIL);
    data.setItDirectorEmail(IT_EMAIL);
    data.setSigningOfficialEmail("invalid");
    initService();
    assertThrows(IllegalArgumentException.class, () -> {
      service.validateNoKeyPersonnelDuplicates(data);
    });
  }

  @Test
  void testValidateNoKeyPersonnelDuplicatesItDirector() {
    DataAccessRequestData data = new DataAccessRequestData();
    data.setPiEmail(PI_EMAIL);
    data.setItDirectorEmail(PI_EMAIL);
    data.setSigningOfficialEmail(SO_EMAIL);
    initService();
    assertThrows(IllegalArgumentException.class, () -> {
      service.validateNoKeyPersonnelDuplicates(data);
    });
  }

  @Test
  void testValidateNoKeyPersonnelDuplicatesSO() {
    DataAccessRequestData data = new DataAccessRequestData();
    data.setPiEmail(PI_EMAIL);
    data.setItDirectorEmail(IT_EMAIL);
    data.setSigningOfficialEmail(PI_EMAIL);
    initService();
    assertThrows(IllegalArgumentException.class, () -> {
      service.validateNoKeyPersonnelDuplicates(data);
    });
  }

  private static class LongerThanTwo implements ArgumentMatcher<String> {

    @Override
    public boolean matches(String argument) {
      return argument.length() > 2;
    }
  }
}
