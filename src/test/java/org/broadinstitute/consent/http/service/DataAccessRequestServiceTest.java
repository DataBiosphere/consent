package org.broadinstitute.consent.http.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import freemarker.template.TemplateException;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotAcceptableException;
import jakarta.ws.rs.NotFoundException;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import org.broadinstitute.consent.http.AbstractTestHelper;
import org.broadinstitute.consent.http.configurations.ConsentConfiguration;
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
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.exceptions.NIHComplianceRuleException;
import org.broadinstitute.consent.http.exceptions.SubmittedDARCannotBeEditedException;
import org.broadinstitute.consent.http.models.CloseoutSupplement;
import org.broadinstitute.consent.http.models.Collaborator;
import org.broadinstitute.consent.http.models.DarCollection;
import org.broadinstitute.consent.http.models.DarDataset;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.LibraryCard;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.service.dao.DataAccessRequestServiceDAO;
import org.jetbrains.annotations.NotNull;
import org.jdbi.v3.core.statement.UnableToExecuteStatementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
class DataAccessRequestServiceTest extends AbstractTestHelper {

  private static final String PI_EMAIL = "pi@example.broadinstitute.org";
  private static final String SO_EMAIL = "so@example.broadinstitute.org";
  private static final String IT_EMAIL = "it@example.broadinstitute.org";
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
  private EmailService emailService;
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
  @Mock
  private InstitutionService institutionService;
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

  @NotNull
  private static DataAccessRequestData getDataAccessRequestData(String goodEmailAddress) {
    DataAccessRequestData data = new DataAccessRequestData();
    data.setPiEmail(goodEmailAddress);
    data.setSigningOfficialEmail(goodEmailAddress);
    data.setItDirectorEmail(goodEmailAddress);
    Collaborator collaborator = new Collaborator();
    collaborator.setEmail(goodEmailAddress);
    data.setInternalCollaborators(List.of(collaborator));
    Collaborator labStaffMember = new Collaborator();
    labStaffMember.setEmail(goodEmailAddress);
    data.setLabCollaborators(List.of(labStaffMember));
    return data;
  }

  @NotNull
  private static DataAccessRequestData getDataAccessRequestData(String goodEmailAddress,
      String badEmailAddress) {
    DataAccessRequestData data = new DataAccessRequestData();
    data.setPiEmail(goodEmailAddress);
    data.setSigningOfficialEmail(goodEmailAddress);
    data.setItDirectorEmail(goodEmailAddress);
    Collaborator collaborator1 = new Collaborator();
    collaborator1.setEmail(goodEmailAddress);
    Collaborator collaborator2 = new Collaborator();
    collaborator2.setEmail(goodEmailAddress);
    data.setLabCollaborators(List.of(collaborator1, collaborator2));
    Collaborator labStaffMember = new Collaborator();
    labStaffMember.setEmail(goodEmailAddress);
    Collaborator labStaffMember2 = new Collaborator();
    labStaffMember2.setEmail(badEmailAddress);
    data.setLabCollaborators(List.of(labStaffMember, labStaffMember2));
    return data;
  }

  @BeforeEach
  void initService() {
    ConsentConfiguration config = new ConsentConfiguration();
    config.getServicesConfiguration().setLocalURL("local_url/");
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
        dataAccessRequestServiceDAO, userService, institutionService,  emailService, config);
  }

  @Test
  void testCreateDataAccessRequest_Update() {
    DataAccessRequest dar = generateDataAccessRequest();
    dar.setCollectionId(null);
    dar.addDatasetIds(List.of(1, 2, 3));
    User user = createUserWithPrerequisites();
    when(institutionService.findInstitutionForEmail(any())).thenReturn(user.getInstitution());
    when(counterService.getNextDarSequence()).thenReturn(1);
    when(dataAccessRequestDAO.findByReferenceId(any())).thenReturn(dar);
    doNothing().when(dataAccessRequestDAO)
        .updateDataByReferenceId(any(), any(), any(), any(), any(), any(), any());
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
    User user = createUserWithPrerequisites();
    when(institutionService.findInstitutionForEmail(any())).thenReturn(user.getInstitution());
    when(counterService.getNextDarSequence()).thenReturn(1);
    when(dataAccessRequestDAO.findByReferenceId("id")).thenReturn(null);
    when(dataAccessRequestDAO.findByReferenceId(argThat(new LongerThanTwo()))).thenReturn(dar);
    when(darCollectionDAO.insertDarCollection(anyString(), anyInt(), any(Date.class))).thenReturn(
        randomInt(1, 100));
    doNothing().when(dataAccessRequestDAO)
        .insertDataAccessRequest(anyInt(), anyString(), anyInt(), any(Date.class), any(Date.class),
            any(Date.class), any(Date.class), any(DataAccessRequestData.class), anyString());
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
    User user = createUserWithPrerequisites();
    when(institutionService.findInstitutionForEmail(any())).thenReturn(user.getInstitution());
    when(dataAccessRequestDAO.findByReferenceId(any())).thenReturn(dar);
    assertThrows(SubmittedDARCannotBeEditedException.class,
        () -> service.createDataAccessRequest(user, dar));
  }

  @Test
  void testCreateDataAccessRequestCreateWithoutERACommons() {
    DataAccessRequest dar = generateDataAccessRequest();
    User user = createUserWithPrerequisites();
    doThrow(BadRequestException.class).when(userService).validateActiveERACredentials(user);
    assertThrows(BadRequestException.class, () -> service.createDataAccessRequest(user, dar));
  }

  @Test
  void testUpdateByReferenceIdThrowsOnDraft() {
    DataAccessRequest dar = generateDataAccessRequest();
    dar.setCollectionId(randomInt(0, 100));
    User user = new User(1, "email@test.org", "Display Name", new Date());
    dar.addDatasetIds(List.of(1, 2, 3));
    dar.setSubmissionDate(Timestamp.from(Instant.now()));
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
    assertThrows(NIHComplianceRuleException.class,
        () -> service.createDataAccessRequest(user, dar));
  }

  @Test
  void createProgressReport() {
    DataAccessRequest parentDar = generateDataAccessRequest();
    DataAccessRequest progressReport = generateProgressReport();
    progressReport.setParentId(parentDar.getId());
    progressReport.setCollectionId(parentDar.getCollectionId());
    parentDar.setSubmissionDate(Timestamp.from(Instant.now()));
    User user = createUserWithPrerequisites();
    parentDar.setUserId(user.getUserId());
    when(dataAccessRequestDAO.findByReferenceId(progressReport.getReferenceId())).thenReturn(progressReport);
    when(dataAccessRequestDAO.findDatasetApprovalsByDar(parentDar.getReferenceId())).thenReturn(
        Set.copyOf(progressReport.getDatasetIds()));
    DataAccessRequest newDar = service.createProgressReport(user, progressReport, parentDar);
    assertNotNull(newDar);
    verify(dataAccessRequestDAO)
        .insertProgressReport(parentDar.getId(), progressReport.getCollectionId(),
            progressReport.getReferenceId(), user.getUserId(),
            progressReport.getData());
    verify(dataAccessRequestDAO).insertAllDarDatasets(
        argThat(new DarDatasetMatcher(progressReport)));
  }

  @Test
  void createProgressReportFailsIfNonApprovedDatasets() {
    DataAccessRequest parentDar = generateDataAccessRequest();
    DataAccessRequest progressReport = generateProgressReport();
    progressReport.setParentId(parentDar.getId());
    progressReport.setCollectionId(parentDar.getCollectionId());
    parentDar.setSubmissionDate(Timestamp.from(Instant.now()));
    User user = createUserWithPrerequisites();
    parentDar.setUserId(user.getUserId());
    when(dataAccessRequestDAO.findDatasetApprovalsByDar(parentDar.getReferenceId())).thenReturn(Set.of());
    assertThrows(BadRequestException.class, () -> service.createProgressReport(user, progressReport, parentDar));
  }

  @Test
  void createProgressReportFailsIfDAOOperationFails() {
    DataAccessRequest parentDar = generateDataAccessRequest();
    DataAccessRequest progressReport = generateProgressReport();
    progressReport.setParentId(parentDar.getId());
    progressReport.setCollectionId(parentDar.getCollectionId());
    parentDar.setSubmissionDate(Timestamp.from(Instant.now()));
    User user = createUserWithPrerequisites();
    parentDar.setUserId(user.getUserId());
    when(dataAccessRequestDAO.findDatasetApprovalsByDar(parentDar.getReferenceId())).thenReturn(
        Set.copyOf(progressReport.getDatasetIds()));
    doThrow(new UnableToExecuteStatementException("Test exception"))
        .when(dataAccessRequestDAO)
        .insertProgressReport(
            parentDar.getId(),
            parentDar.getCollectionId(),
            progressReport.referenceId,
            user.getUserId(),
            progressReport.data);
    assertThrows(BadRequestException.class, () -> service.createProgressReport(user, progressReport, parentDar));
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
    User user = createUserWithPrerequisites();
    DataAccessRequest progressReport = generateProgressReport();
    DataAccessRequest parentDar = generateDataAccessRequest();
    assertThrows(BadRequestException.class,
        () -> service.validateProgressReport(user, progressReport, parentDar));
  }

  @Test
  void validateProgressReportNoDatasetIds() {
    User user = createUserWithPrerequisites();
    DataAccessRequest progressReport = generateProgressReport();
    progressReport.setDatasetIds(Collections.emptyList());
    DataAccessRequest parentDar = generateDataAccessRequest();
    parentDar.setSubmissionDate(Timestamp.from(Instant.now()));
    parentDar.setUserId(user.getUserId());
    assertThrows(BadRequestException.class,
        () -> service.validateProgressReport(user, progressReport, parentDar));
  }

  @Test
  void validateProgressReportNoSummary() {
    User user = createUserWithPrerequisites();
    DataAccessRequest progressReport = generateProgressReport();
    progressReport.getData().setProgressReportSummary(null);
    DataAccessRequest parentDar = generateDataAccessRequest();
    parentDar.setSubmissionDate(Timestamp.from(Instant.now()));
    parentDar.setUserId(user.getUserId());
    assertThrows(BadRequestException.class,
        () -> service.validateProgressReport(user, progressReport, parentDar));
  }

  @Test
  void validateProgressReportInvalidDatasetIds() {
    User user = createUserWithPrerequisites();
    DataAccessRequest progressReport = generateProgressReport();
    progressReport.setDatasetIds(List.of(3, 4, 5)); // IDs not all in parent DAR
    DataAccessRequest parentDar = generateDataAccessRequest();
    parentDar.setSubmissionDate(Timestamp.from(Instant.now()));
    parentDar.setDatasetIds(List.of(1, 2, 3));
    parentDar.setUserId(user.getUserId());
    assertThrows(BadRequestException.class,
        () -> service.validateProgressReport(user, progressReport, parentDar));
  }

  @Test
  void validateProgressReport() {
    User user = createUserWithPrerequisites();
    DataAccessRequest progressReport = generateProgressReport();
    progressReport.setDatasetIds(List.of(1, 2));
    DataAccessRequest parentDar = generateDataAccessRequest();
    parentDar.setSubmissionDate(Timestamp.from(Instant.now()));
    parentDar.setDatasetIds(List.of(1, 2, 3));
    parentDar.setUserId(user.getUserId());
    assertDoesNotThrow(() -> service.validateProgressReport(user, progressReport, parentDar));
  }

  @Test
  void validateProgressReportWithCollaboratorsAndStaffInSameInstitution() {
    User user = createUserWithPrerequisites();
    user.setLibraryCard(new LibraryCard());
    DataAccessRequest progressReport = generateProgressReport();
    progressReport.setDatasetIds(List.of(1, 2));
    DataAccessRequestData progressReportData = progressReport.getData();
    Collaborator collaborator1 = new Collaborator();
    collaborator1.setEmail("1" + user.getEmail());
    progressReportData.setInternalCollaborators(Collections.singletonList(collaborator1));
    Collaborator collaborator2 = new Collaborator();
    collaborator2.setEmail("2" + user.getEmail());
    progressReportData.setLabCollaborators(Collections.singletonList(collaborator2));
    User collaborator1User = createUserWithPrerequisites();
    collaborator1User.setEmail(collaborator1.getEmail());
    collaborator1User.setInstitutionId(user.getInstitutionId());
    collaborator1User.setLibraryCard(new LibraryCard());
    User collaborator2User = createUserWithPrerequisites();
    collaborator2User.setEmail(collaborator2.getEmail());
    collaborator2User.setInstitutionId(user.getInstitutionId());
    collaborator2User.setLibraryCard(new LibraryCard());
    DataAccessRequest parentDar = generateDataAccessRequest();
    parentDar.setSubmissionDate(Timestamp.from(Instant.now()));
    parentDar.setDatasetIds(List.of(1, 2, 3));
    parentDar.setUserId(user.getUserId());
    when(userDAO.findUserByEmail(collaborator1.getEmail())).thenReturn(collaborator1User);
    when(userDAO.findUserByEmail(collaborator2.getEmail())).thenReturn(collaborator2User);
    when(institutionService.findInstitutionForEmail(any())).thenReturn(user.getInstitution());
    assertDoesNotThrow(() -> service.validateProgressReport(user, progressReport, parentDar));
  }

  @Test
  void validateProgressReportWithCollaboratorsAndStaffInInvalidInstitution() {
    User user = createUserWithPrerequisites();
    DataAccessRequest progressReport = generateProgressReport();
    progressReport.setDatasetIds(List.of(1, 2));
    DataAccessRequestData progressReportData = progressReport.getData();
    Collaborator collaborator1 = new Collaborator();
    collaborator1.setEmail("alice@1otherdomain.org");
    progressReportData.setInternalCollaborators(Collections.singletonList(collaborator1));
    Collaborator collaborator2 = new Collaborator();
    collaborator2.setEmail("eve@yetanotherdomain.org");
    progressReportData.setLabCollaborators(Collections.singletonList(collaborator2));
    DataAccessRequest parentDar = generateDataAccessRequest();
    parentDar.setSubmissionDate(Timestamp.from(Instant.now()));
    parentDar.setDatasetIds(List.of(1, 2, 3));
    parentDar.setUserId(user.getUserId());
    when(userDAO.findUserByEmail(collaborator1.getEmail())).thenReturn(user);
    when(institutionService.findInstitutionForEmail(any())).thenReturn(null);
    BadRequestException badRequestException = assertThrows(BadRequestException.class, () -> service.validateProgressReport(user, progressReport, parentDar));
    assertThat(
        badRequestException.getMessage(),
        containsString(
"""
All listed personnel must share the same institutional affiliation and have a library card.  The \
following list of roles and members must have email addresses associated with your institution or \
library cards issued: Internal Collaborator member:  (missing institution) alice@1otherdomain.org, \
Lab staff member:  (missing institution) eve@yetanotherdomain.org, Lab staff member:  (missing \
library card) eve@yetanotherdomain.org\
"""));
  }

  @Test
  void validateDarNullUser() {
    DataAccessRequest dar = generateDataAccessRequest();
    assertThrows(IllegalArgumentException.class, () -> service.validateDar(null, dar));
  }

  @Test
  void validateDarNullDar() {
    User user = new User(1, "email@test.org", "Display Name", new Date());
    assertThrows(IllegalArgumentException.class, () -> service.validateDar(user, null));
  }

  @Test
  void validateDarNullReferenceId() {
    DataAccessRequest dar = generateDataAccessRequest();
    dar.setReferenceId(null);
    User user = new User(1, "email@test.org", "Display Name", new Date());
    assertThrows(IllegalArgumentException.class, () -> service.validateDar(user, dar));
  }

  @Test
  void validateDarNullData() {
    DataAccessRequest dar = generateDataAccessRequest();
    dar.setData(null);
    User user = new User(1, "email@test.org", "Display Name", new Date());
    assertThrows(IllegalArgumentException.class, () -> service.validateDar(user, dar));
  }

  @Test
  void validateDarNoLibraryCards() {
    DataAccessRequest dar = generateDataAccessRequest();
    User user = new User(1, "email@test.org", "Display Name", new Date());
    assertThrows(NIHComplianceRuleException.class, () -> service.validateDar(user, dar));
  }

  @Test
  void validateDar() {
    DataAccessRequest dar = generateDataAccessRequest();
    User user = createUserWithPrerequisites();
    when(institutionService.findInstitutionForEmail(any())).thenReturn(user.getInstitution());
    assertDoesNotThrow(() -> service.validateDar(user, dar));
  }

  @Test
  void testValidateInternalCollaboratorsNone() {
    User user = createRequestingUser();
    DataAccessRequest dar = createDataAccessRequest(List.of());
    assertDoesNotThrow(() -> service.validateInternalCollaborators(user, dar));
  }

  @Test
  void testValidateInternalCollaboratorsValid() {
    User requestingUser = createRequestingUser();
    Collaborator validCollaborator = createCollaborator();
    User collaboratorUser = new User(2, validCollaborator.getEmail(), "Collaborator", new Date(),
        roles);
    collaboratorUser.setInstitutionId(requestingUser.getInstitutionId());
    LibraryCard libraryCard = new LibraryCard();
    collaboratorUser.setLibraryCard(libraryCard);
    DataAccessRequest dar = createDataAccessRequest(List.of(validCollaborator));
    when(institutionService.findInstitutionForEmail(collaboratorUser.getEmail())).thenReturn(requestingUser.getInstitution());
    when(userDAO.findUserByEmail(validCollaborator.getEmail())).thenReturn(collaboratorUser);

    assertDoesNotThrow(() -> service.validateInternalCollaborators(requestingUser, dar));
  }

  @Test
  void testValidateInternalCollaboratorsLibraryCard_NoInstitution() {
    User requestingUser = createRequestingUser();
    Collaborator validCollaborator = createCollaborator();
    User collaboratorUser = new User(2, validCollaborator.getEmail(), "Collaborator", new Date(),
        roles);
    collaboratorUser.setInstitutionId(requestingUser.getInstitutionId());
    LibraryCard libraryCard = new LibraryCard();
    collaboratorUser.setLibraryCard(libraryCard);
    DataAccessRequest dar = createDataAccessRequest(List.of(validCollaborator));
    when(userDAO.findUserByEmail(validCollaborator.getEmail())).thenReturn(collaboratorUser);

    BadRequestException exception = assertThrows(BadRequestException.class, () -> service.validateInternalCollaborators(requestingUser, dar));
    assertEquals("""
 All listed personnel must share the same institutional affiliation and have a library card.  \
 The following list of roles and members must have email addresses associated with your \
 institution or library cards issued: Internal Collaborator member:  (missing institution) %s\
 """.formatted(collaboratorUser.getEmail()), exception.getMessage());
  }

  @Test
  void testValidateInternalCollaboratorsDoesNotExist() {
    User requestingUser = createRequestingUser();
    Collaborator invalidCollaborator = createCollaborator();
    DataAccessRequest dar = createDataAccessRequest(List.of(invalidCollaborator));
    when(userDAO.findUserByEmail(invalidCollaborator.getEmail())).thenReturn(null);

    BadRequestException exception =
        assertThrows(
            BadRequestException.class,
            () -> service.validateInternalCollaborators(requestingUser, dar));
    assertEquals(
        """
    All listed personnel must share the same institutional affiliation and have a \
    library card.  The following list of roles and members must have email addresses associated with your \
    institution or library cards issued: Internal Collaborator member:  (missing institution) \
    %s, Internal Collaborator member:  (missing library card) %s\
    """
            .formatted(invalidCollaborator.getEmail(), invalidCollaborator.getEmail()),
        exception.getMessage());
  }

  @Test
  void testValidateInternalCollaboratorsNoLibraryCard() {
    User requestingUser = createRequestingUser();
    Collaborator invalidCollaborator = createCollaborator();
    User collaboratorUser = new User(2, invalidCollaborator.getEmail(), "Collaborator", new Date(),
        roles);
    collaboratorUser.setInstitutionId(requestingUser.getInstitutionId());
    DataAccessRequest dar = createDataAccessRequest(List.of(invalidCollaborator));
    when(userDAO.findUserByEmail(invalidCollaborator.getEmail())).thenReturn(collaboratorUser);
    when(institutionService.findInstitutionForEmail(invalidCollaborator.getEmail())).thenReturn(requestingUser.getInstitution());

    BadRequestException exception = assertThrows(BadRequestException.class, () ->
        service.validateInternalCollaborators(requestingUser, dar)
    );
    assertEquals("""
All listed personnel must share the same institutional affiliation and have a library card.  \
The following list of roles and members must have email addresses associated with your \
institution or library cards issued: Internal Collaborator member:  \
(missing library card) %s\
""".formatted(collaboratorUser.getEmail()), exception.getMessage());
  }

  @Test
  void testUpdateByReferenceIdVersion2() throws Exception {
    DataAccessRequest dar = generateDataAccessRequest();
    dar.setCollectionId(randomInt(0, 100));
    User user = new User(1, "email@test.org", "Display Name", new Date());
    dar.addDatasetIds(List.of(1, 2, 3));
    when(dataAccessRequestServiceDAO.updateByReferenceId(any(), any())).thenReturn(dar);
    DataAccessRequest newDar = service.updateByReferenceId(user, dar);
    assertNotNull(newDar);
  }

  @Test
  void testUpdateByReferenceIdVersion2_WithCollection() throws Exception {
    DataAccessRequest dar = generateDataAccessRequest();
    User user = new User(1, "email@test.org", "Display Name", new Date());
    dar.addDatasetIds(List.of(1, 2, 3));
    when(dataAccessRequestServiceDAO.updateByReferenceId(user, dar)).thenReturn(dar);
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
    dar1.setUserId(user1.getUserId());
    DataAccessRequest dar2 = new DataAccessRequest();
    dar2.setUserId(user2.getUserId());
    when(dataAccessRequestDAO
        .findApprovedDARsByDatasetId(d.getDatasetId()))
        .thenReturn(List.of(dar1, dar2));

    assertEquals(List.of(dar1, dar2), service.getApprovedDARsForDataset(d));
  }

  @Test
  void testInsertDraftDataAccessRequest() {
    User user = new User();
    user.setUserId(1);
    user.setLibraryCard(new LibraryCard());
    DataAccessRequest draft = generateDataAccessRequest();
    doNothing()
        .when(dataAccessRequestDAO)
        .insertDraftDataAccessRequest(any(), any(), any(), any(), any(), any());
    when(dataAccessRequestDAO.findByReferenceId(any())).thenReturn(draft);
    DataAccessRequest dar = service.insertDraftDataAccessRequest(user, draft);
    assertNotNull(dar);
  }

  @Test
  void testInsertDraftDataAccessRequestFailure() {
    assertThrows(IllegalArgumentException.class, () -> service.insertDraftDataAccessRequest(null, null));
  }

  private DataAccessRequest generateProgressReport() {
    DataAccessRequest progressReport = generateDataAccessRequest();
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
    List<DataAccessRequest> drafts = service.findAllDraftDataAccessRequests();
    assertEquals(1, drafts.size());
  }

  @Test
  void testFindAllDraftDataAccessRequestsByUser() {
    when(dataAccessRequestDAO.findAllDraftsByUserId(any())).thenReturn(
        List.of(new DataAccessRequest()));
    List<DataAccessRequest> drafts = service.findAllDraftDataAccessRequestsByUser(1);
    assertEquals(1, drafts.size());
  }

  @Test
  void getDataAccessRequestsForUser() {
    List<DataAccessRequest> dars = List.of(new DataAccessRequest());
    when(dataAccessRequestDAO.findAllDataAccessRequests()).thenReturn(dars);
    when(dacService.filterDataAccessRequestsByDac(eq(dars), any())).thenReturn(dars);
    List<DataAccessRequest> foundDars = service.getDataAccessRequestsByUserRole(new User());
    assertEquals(1, foundDars.size());
  }

  @Test
  void testFindByReferenceId() {
    DataAccessRequest dar = new DataAccessRequest();
    when(dataAccessRequestDAO.findByReferenceId(any())).thenReturn(dar);
    DataAccessRequest foundDar = service.findByReferenceId("refId");
    assertEquals(dar, foundDar);
  }

  @Test
  void testFindByReferenceId_NotFound() {
    when(dataAccessRequestDAO.findByReferenceId(any())).thenThrow(new NotFoundException());
    assertThrows(NotFoundException.class, () -> service.findByReferenceId("referenceId"));
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

    assertDoesNotThrow(() -> service.deleteByReferenceId(user, referenceId));
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

    assertThrows(NotAcceptableException.class,
        () -> service.deleteByReferenceId(user, referenceId));
  }

  @Test
  void testValidateNoKeyPersonnelDuplicates() {
    DataAccessRequestData data = new DataAccessRequestData();
    data.setPiEmail(PI_EMAIL);
    data.setItDirectorEmail(IT_EMAIL);
    data.setSigningOfficialEmail(SO_EMAIL);
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
    assertThrows(IllegalArgumentException.class,
        () -> service.validateNoKeyPersonnelDuplicates(data));
  }

  @Test
  void testValidateNoKeyPersonnelDuplicatesBadITDirectorEmail() {
    DataAccessRequestData data = new DataAccessRequestData();
    data.setPiEmail(PI_EMAIL);
    data.setItDirectorEmail("invalid");
    data.setSigningOfficialEmail(SO_EMAIL);
    assertThrows(IllegalArgumentException.class,
        () -> service.validateNoKeyPersonnelDuplicates(data));
  }

  @Test
  void testValidateNoKeyPersonnelDuplicatesBadSO() {
    DataAccessRequestData data = new DataAccessRequestData();
    data.setPiEmail(PI_EMAIL);
    data.setItDirectorEmail(IT_EMAIL);
    data.setSigningOfficialEmail("invalid");
    assertThrows(IllegalArgumentException.class,
        () -> service.validateNoKeyPersonnelDuplicates(data));
  }

  @Test
  void testValidateNoKeyPersonnelDuplicatesItDirector() {
    DataAccessRequestData data = new DataAccessRequestData();
    data.setPiEmail(PI_EMAIL);
    data.setItDirectorEmail(PI_EMAIL);
    data.setSigningOfficialEmail(SO_EMAIL);
    assertThrows(IllegalArgumentException.class,
        () -> service.validateNoKeyPersonnelDuplicates(data));
  }

  @Test
  void testValidateNoKeyPersonnelDuplicatesSO() {
    DataAccessRequestData data = new DataAccessRequestData();
    data.setPiEmail(PI_EMAIL);
    data.setItDirectorEmail(IT_EMAIL);
    data.setSigningOfficialEmail(PI_EMAIL);
    assertThrows(IllegalArgumentException.class,
        () -> service.validateNoKeyPersonnelDuplicates(data));
  }

  @Test
  void testValidatePersonnelInstitutionAndLibraryCardRequirementsThrowsException() {
    String badEmailAddress = "j@example.com";
    User user = new User(1, "email@test.org", "Display Name", new Date());
    Institution usersInstitution = new Institution();
    usersInstitution.setId(1);
    user.setInstitution(usersInstitution);
    DataAccessRequestData data = getDataAccessRequestData(badEmailAddress);

    Institution badInstitution = new Institution();
    badInstitution.setId(2);
    when(institutionService.findInstitutionForEmail(badEmailAddress)).thenReturn(badInstitution);
    initService();
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> service.validatePersonnelInstitutionAndLibraryCardRequirements(user, data));
    validateException(
        exception,
        badEmailAddress,
        List.of(
            "Principal Investigator",
            "Signing Official",
            "IT Director",
            "Internal Collaborator",
            "Lab staff"));
  }

  @Test
  void testValidatePersonnelInstitutionAndLibraryCardRequirementsDoesNotThrowException() {
    String goodEmailAddress = "j@example.com";
    User user = new User(1, "j@example.com", "Display Name", new Date());
    Institution goodInstitution = new Institution();
    goodInstitution.setId(1);
    user.setInstitution(goodInstitution);
    user.setLibraryCard(new LibraryCard());
    DataAccessRequestData data = getDataAccessRequestData(goodEmailAddress);

    when(institutionService.findInstitutionForEmail(goodEmailAddress)).thenReturn(goodInstitution);
    when(userDAO.findUserByEmail(goodEmailAddress)).thenReturn(user);
    initService();
    assertDoesNotThrow(() -> service.validatePersonnelInstitutionAndLibraryCardRequirements(user, data));
  }

  @Test
  void testValidatePersonnelInstitutionAndLibraryCardRequirementsThrowsForBadPI() {
    String badEmailAddress = "bad@evil.com";
    User user = new User(1, "j@example.com", "Display Name", new Date());
    Institution goodInstitution = new Institution();
    goodInstitution.setId(1);
    user.setInstitution(goodInstitution);
    DataAccessRequestData data = getDataAccessRequestData(badEmailAddress);
    when(institutionService.findInstitutionForEmail(badEmailAddress)).thenReturn(null);
    initService();
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> service.validatePersonnelInstitutionAndLibraryCardRequirements(user, data));
    validateException(exception, badEmailAddress, List.of("Principal Investigator"));
  }

  @Test
  void testValidatePersonnelInstitutionAndLibraryCardRequirementsThrowsForBadSO() {
    String goodEmailAddress = "j@example.com";
    String badEmailAddress = "bad@evil.com";
    User user = new User(1, "j@example.com", "Display Name", new Date());
    Institution goodInstitution = new Institution();
    goodInstitution.setId(1);
    user.setInstitution(goodInstitution);
    DataAccessRequestData data = new DataAccessRequestData();
    data.setPiEmail(goodEmailAddress);
    data.setSigningOfficialEmail(badEmailAddress);
    data.setItDirectorEmail(goodEmailAddress);
    when(institutionService.findInstitutionForEmail(goodEmailAddress)).thenReturn(goodInstitution);
    when(institutionService.findInstitutionForEmail(badEmailAddress)).thenReturn(null);
    initService();
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> service.validatePersonnelInstitutionAndLibraryCardRequirements(user, data));
    validateException(exception, badEmailAddress, List.of("Signing Official"));
  }

  private void validateException(
      IllegalArgumentException exception, String email, List<String> errorTypes) {
    assertTrue(exception.getMessage().toLowerCase().contains(email.toLowerCase()));
    errorTypes.forEach(
        errorType ->
            assertTrue(exception.getMessage().toLowerCase().contains(errorType.toLowerCase())));
  }

  @Test
  void testValidatePersonnelInstitutionAndLibraryCardRequirementsThrowsForBadIT() {
    String goodEmailAddress = "j@example.com";
    String badEmailAddress = "bad@evil.com";
    User user = new User(1, "j@example.com", "Display Name", new Date());
    Institution goodInstitution = new Institution();
    goodInstitution.setId(1);
    user.setInstitution(goodInstitution);
    DataAccessRequestData data = new DataAccessRequestData();
    data.setPiEmail(goodEmailAddress);
    data.setSigningOfficialEmail(goodEmailAddress);
    data.setItDirectorEmail(badEmailAddress);
    when(institutionService.findInstitutionForEmail(goodEmailAddress)).thenReturn(goodInstitution);
    when(institutionService.findInstitutionForEmail(badEmailAddress)).thenReturn(null);
    initService();
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> service.validatePersonnelInstitutionAndLibraryCardRequirements(user, data));
    validateException(exception, badEmailAddress, List.of("IT Director"));
  }

  @Test
  void testValidatePersonnelInstitutionAndLibraryCardRequirementsThrowsForBadCollaborator() {
    String goodEmailAddress = "j@example.com";
    String badEmailAddress = "bad@evil.com";
    User user = new User(1, "j@example.com", "Display Name", new Date());
    Institution goodInstitution = new Institution();
    goodInstitution.setId(1);
    user.setInstitution(goodInstitution);
    DataAccessRequestData data = new DataAccessRequestData();
    data.setPiEmail(goodEmailAddress);
    data.setSigningOfficialEmail(goodEmailAddress);
    data.setItDirectorEmail(goodEmailAddress);
    Collaborator collaborator1 = new Collaborator();
    collaborator1.setEmail(goodEmailAddress);
    Collaborator collaborator2 = new Collaborator();
    collaborator2.setEmail(badEmailAddress);
    data.setInternalCollaborators(List.of(collaborator1, collaborator2));
    when(institutionService.findInstitutionForEmail(goodEmailAddress)).thenReturn(goodInstitution);
    when(institutionService.findInstitutionForEmail(badEmailAddress)).thenReturn(null);
    initService();
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> service.validatePersonnelInstitutionAndLibraryCardRequirements(user, data));
    validateException(exception, badEmailAddress, List.of("Internal Collaborator"));
  }

  @Test
  void testValidatePersonnelInstitutionAndLibraryCardRequirementsThrowsForBadLabStaffMember() {
    String goodEmailAddress = "j@example.com";
    String badEmailAddress = "bad@evil.com";
    User user = new User(1, "j@example.com", "Display Name", new Date());
    Institution goodInstitution = new Institution();
    goodInstitution.setId(1);
    user.setInstitution(goodInstitution);
    DataAccessRequestData data = getDataAccessRequestData(goodEmailAddress, badEmailAddress);
    when(institutionService.findInstitutionForEmail(goodEmailAddress)).thenReturn(goodInstitution);
    when(institutionService.findInstitutionForEmail(badEmailAddress)).thenReturn(null);
    initService();
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> service.validatePersonnelInstitutionAndLibraryCardRequirements(user, data));
    validateException(exception, badEmailAddress, List.of("Lab staff member"));
  }

  @Test
  void testValidatePersonnelInstitution_AndLibraryCardRequirements_NoCollaborators() {
    String badEmailAddress = "j@example.com";
    User user = new User(1, "email@test.org", "Display Name", new Date());
    Institution usersInstitution = new Institution();
    usersInstitution.setId(1);
    user.setInstitution(usersInstitution);
    DataAccessRequestData data = new DataAccessRequestData();
    data.setPiEmail(badEmailAddress);
    data.setSigningOfficialEmail(badEmailAddress);
    data.setItDirectorEmail(badEmailAddress);

    Institution badInstitution = new Institution();
    badInstitution.setId(2);
    when(institutionService.findInstitutionForEmail(badEmailAddress)).thenReturn(badInstitution);
    initService();
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> service.validatePersonnelInstitutionAndLibraryCardRequirements(user, data));
    validateException(
        exception,
        badEmailAddress,
        List.of("Principal Investigator", "Signing Official", "IT Director"));
  }

  private User createUserWithPrerequisites() {
    User user = new User(1, "email@test.org", "Display Name", new Date());
    Institution institution = new Institution();
    institution.setId(1);
    user.setInstitution(institution);
    user.setLibraryCard(new LibraryCard());
    user.setEraCommonsId("eraCommonsId");
    return user;
  }

  @Test
  void testSendReminderMessage() throws TemplateException, IOException {
    Election election = new Election();
    election.setElectionId(randomInt(0, 100));
    election.setReferenceId(UUID.randomUUID().toString());
    election.setElectionType(ElectionType.DATA_ACCESS.getValue());
    when(electionDAO.findElectionWithFinalVoteById(any())).thenReturn(election);

    Vote vote = new Vote();
    vote.setVoteId(randomInt(0, 100));
    vote.setElectionId(election.getElectionId());
    when(voteDAO.findVoteById(any())).thenReturn(vote);

    DarCollection collection = new DarCollection();
    collection.setDarCollectionId(randomInt(0, 100));
    collection.setDarCode("DAR-12345");
    when(darCollectionDAO.findDARCollectionByReferenceId(any())).thenReturn(collection);

    User user = new User();
    user.setDisplayName(randomAlphanumeric(10));
    user.setEmail(randomAlphanumeric(10));
    when(userDAO.findUserById(any())).thenReturn(user);

    initService();
    service.sendReminderMessage(vote.getVoteId());
    verify(emailService)
        .sendReminderMessage(
            user,
            vote,
            collection.getDarCode(),
            election.getElectionType(),
            "local_url/dar_collection/" + collection.getDarCollectionId());
    verify(voteDAO).updateVoteReminderFlag(vote.getVoteId(), true);
  }

  @Test
  void sendExpirationNoticesTest() {
    User user1 = new User();
    user1.setUserId(123);
    user1.setDisplayName("John Doe");
    user1.setEmail("jd@somewhere");

    User user2 = new User();
    user2.setUserId(124);
    user2.setDisplayName("Jane Doe");
    user2.setEmail("jd@somewhereelse");

    DataAccessRequest dar1 = getMockedDar("DAR-12345", UUID.randomUUID().toString(), user1);
    DataAccessRequest dar2 = getMockedDar("DAR-12346", UUID.randomUUID().toString(), user2);

    when(userDAO.findUserById(user1.getUserId())).thenReturn(user1);
    when(userDAO.findUserById(user2.getUserId())).thenReturn(user2);
    List<DataAccessRequest> dars = List.of(dar1, dar2);
    when(dataAccessRequestDAO.findAgedDARsByEmailTypeOlderThanInterval(any(), any(), any())).thenReturn(dars);
    initService();
    assertDoesNotThrow(() -> service.sendExpirationNotices());
  }

  @Test
  void sendExpirationNoticesTestMissingEmailForOneUser() {
    ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
    ch.qos.logback.classic.Logger log = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(DataAccessRequestService.class);
    listAppender.start();
    log.addAppender(listAppender);
    User user1 = new User();
    user1.setUserId(123);
    user1.setDisplayName("John Doe");
    user1.setEmail("jd@somewhere");

    User user2 = new User();
    user2.setUserId(124);
    user2.setDisplayName("Jane Doe");

    DataAccessRequest dar1 = getMockedDar("DAR-12345", UUID.randomUUID().toString(), user1);
    DataAccessRequest dar2 = getMockedDar("DAR-12346", UUID.randomUUID().toString(), user2);

    when(userDAO.findUserById(user1.getUserId())).thenReturn(user1);
    when(userDAO.findUserById(user2.getUserId())).thenReturn(user2);

    List<DataAccessRequest> dars = List.of(dar2, dar1);

    when(dataAccessRequestDAO.findAgedDARsByEmailTypeOlderThanInterval(any(), any(), any())).thenReturn(dars);
    initService();

    assertDoesNotThrow(()->service.sendExpirationNotices());

    assertEquals(2, listAppender.list.size());
  }

  @Test
  void sendExpirationNoticesTestUnderlyingExceptionThrownSendingOneTypeOfMessage() {
    ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
    ch.qos.logback.classic.Logger log = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(DataAccessRequestService.class);
    listAppender.start();
    log.addAppender(listAppender);
    User user1 = new User();
    user1.setUserId(123);
    user1.setDisplayName("John Doe");
    user1.setEmail("jd@somewhere");

    User user2 = new User();
    user2.setUserId(124);
    user2.setDisplayName("Jane Doe");
    user2.setEmail(null);

    DataAccessRequest dar1 = getMockedDar("DAR-12345", UUID.randomUUID().toString(), user1);
    DataAccessRequest dar2 = getMockedDar("DAR-12346", UUID.randomUUID().toString(), user2);

    when(userDAO.findUserById(user1.getUserId())).thenReturn(user1);
    when(userDAO.findUserById(user2.getUserId())).thenReturn(user2);

    List<DataAccessRequest> dars = List.of(dar2, dar1);

    when(dataAccessRequestDAO.findAgedDARsByEmailTypeOlderThanInterval(any(), any(), any())).thenReturn(dars);
    initService();
    assertDoesNotThrow(()->service.sendExpirationNotices());

    assertEquals(2, listAppender.list.size());
  }

  @Test
  void testValidateCloseoutApproval_NonCloseout(){
    User user = new User();
    user.setUserId(123);
    DataAccessRequest dar = new DataAccessRequest();
    BadRequestException exception = assertThrows(BadRequestException.class, ()->service.validateCloseoutApproval(user, dar));
    assertThat(exception.getMessage(), containsString("Signing officials can only approve closeout progress reports."));
  }


  @Test
  void testValidateCloseoutApproval_AlreadyApproved(){
    User user = new User();
    user.setUserId(123);
    DataAccessRequest dar = new DataAccessRequest();
    dar.setSubmissionDate(Timestamp.from(Instant.now()));
    dar.setParentId(1);
    dar.setCloseoutSigningOfficialApprovedDate(Timestamp.from(Instant.now()));
    dar.setCloseoutSigningOfficialApprovedUserId(1);
    DataAccessRequestData data =  new DataAccessRequestData();
    data.setCloseoutSupplement(new CloseoutSupplement(List.of(""), "", 1));
    dar.setData(data);

    BadRequestException exception = assertThrows(BadRequestException.class, ()->service.validateCloseoutApproval(user, dar));
    assertThat(exception.getMessage(), containsString("This progress report closeout has already been approved by a signing official."));
  }

  @Test
  void testValidateCloseoutApproval_NotTheSelectedSigningOfficial(){
    User user = new User();
    user.setUserId(123);
    DataAccessRequest dar = new DataAccessRequest();
    dar.setSubmissionDate(Timestamp.from(Instant.now()));
    dar.setParentId(1);

    DataAccessRequestData data =  new DataAccessRequestData();
    data.setCloseoutSupplement(new CloseoutSupplement(List.of(""), "", 1));
    dar.setData(data);

    BadRequestException exception = assertThrows(BadRequestException.class, ()->service.validateCloseoutApproval(user, dar));
    assertThat(
        exception.getMessage(),
        containsString(
           "This request can only be approved by the signing official selected in the closeout request."));
  }

  @Test
  void testValidateCloseoutApproval_NotInSameInstitution(){
    User actor = new User();
    actor.setUserId(123);
    actor.setInstitutionId(1);
    User darSubmitter = new User();
    darSubmitter.setUserId(124);
    darSubmitter.setInstitutionId(2);
    DataAccessRequest dar = new DataAccessRequest();
    dar.setUserId(darSubmitter.getUserId());
    dar.setSubmissionDate(Timestamp.from(Instant.now()));
    dar.setParentId(1);

    DataAccessRequestData data =  new DataAccessRequestData();
    data.setCloseoutSupplement(new CloseoutSupplement(List.of(""), "", actor.getUserId()));
    dar.setData(data);

    when(userService.findUserById(darSubmitter.getUserId())).thenReturn(darSubmitter);
    BadRequestException exception = assertThrows(BadRequestException.class, ()->service.validateCloseoutApproval(actor, dar));
    assertThat(
        exception.getMessage(),
        containsString(
            "Signing Officials must be in the same institution as the creator of the closeout request."));
  }

  @Test
  void testValidateCloseoutApproval(){
    User actor = new User();
    actor.setUserId(123);
    actor.setInstitutionId(1);
    User darSubmitter = new User();
    darSubmitter.setUserId(124);
    darSubmitter.setInstitutionId(1);
    DataAccessRequest dar = new DataAccessRequest();
    dar.setUserId(darSubmitter.getUserId());
    dar.setSubmissionDate(Timestamp.from(Instant.now()));
    dar.setParentId(1);

    DataAccessRequestData data =  new DataAccessRequestData();
    data.setCloseoutSupplement(new CloseoutSupplement(List.of(""), "", actor.getUserId()));
    dar.setData(data);

    when(userService.findUserById(darSubmitter.getUserId())).thenReturn(darSubmitter);
    assertDoesNotThrow(()->service.validateCloseoutApproval(actor, dar));
  }

  private DataAccessRequest getMockedDar(String darCode, String referenceId, User user) {
    DataAccessRequest dar = mock(DataAccessRequest.class);
    when(dar.getReferenceId()).thenReturn(referenceId);
    when(dar.getDarCode()).thenReturn(darCode);
    when(dar.getUserId()).thenReturn(user.getUserId());
    return dar;
  }

  static class DarDatasetMatcher implements ArgumentMatcher<List<DarDataset>> {

    private final DataAccessRequest progressReport;

    public DarDatasetMatcher(DataAccessRequest progressReport) {
      this.progressReport = progressReport;
    }

    @Override
    public boolean matches(List<DarDataset> darDatasets) {
      for (int i = 0; i < darDatasets.size(); i++) {
        if (!darDatasets.get(i).getReferenceId().equals(progressReport.getReferenceId()) ||
            !darDatasets.get(i).getDatasetId().equals(progressReport.getDatasetIds().get(i))) {
          return false;
        }
      }
      return true;
    }
  }

  private static class LongerThanTwo implements ArgumentMatcher<String> {

    @Override
    public boolean matches(String argument) {
      return argument.length() > 2;
    }
  }

}
