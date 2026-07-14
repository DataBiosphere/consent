package org.broadinstitute.consent.http.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import org.broadinstitute.consent.http.AbstractTestHelper;
import org.broadinstitute.consent.http.configurations.ConsentConfiguration;
import org.broadinstitute.consent.http.db.DaaDAO;
import org.broadinstitute.consent.http.db.DarCollectionDAO;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.MatchDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.exceptions.NIHComplianceRuleException;
import org.broadinstitute.consent.http.exceptions.SubmittedDARCannotBeEditedException;
import org.broadinstitute.consent.http.mail.message.DarExpirationReminderMessage;
import org.broadinstitute.consent.http.mail.message.DarExpiredMessage;
import org.broadinstitute.consent.http.mail.message.ReminderMessage;
import org.broadinstitute.consent.http.mail.message.SubmittedCloseoutMessage;
import org.broadinstitute.consent.http.models.CloseoutSupplement;
import org.broadinstitute.consent.http.models.Collaborator;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DarCollection;
import org.broadinstitute.consent.http.models.DarDataset;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.DataManagementIncident;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetDaaMapping;
import org.broadinstitute.consent.http.models.DatasetDaaSnapshot;
import org.broadinstitute.consent.http.models.DatasetDaaSnapshotDetail;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.IntellectualProperty;
import org.broadinstitute.consent.http.models.LibraryCard;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.rules.DACAutomationRuleType;
import org.broadinstitute.consent.http.service.dao.DataAccessRequestServiceDAO;
import org.broadinstitute.consent.http.util.CountryValidator;
import org.glassfish.jersey.server.ContainerRequest;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.UnableToExecuteStatementException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
class DataAccessRequestServiceTest extends AbstractTestHelper {

  private static final String PI_EMAIL = "pi@example.broadinstitute.org";
  private static final String SO_EMAIL = "so@example.broadinstitute.org";
  private static final String IT_EMAIL = "it@example.broadinstitute.org";
  private static final String USER_EMAIL = "email@test.org";
  private static final String USER_NAME = "Display Name";
  private final List<UserRole> roles = List.of(UserRoles.Researcher());
  @Mock private Jdbi jdbi;
  @Mock private CounterService counterService;
  @Mock private DataAccessRequestDAO dataAccessRequestDAO;
  @Mock private DarCollectionDAO darCollectionDAO;
  @Mock private UserDAO userDAO;
  @Mock private DatasetDAO dataSetDAO;
  @Mock private DaaDAO daaDAO;
  @Mock private ElectionDAO electionDAO;
  @Mock private EmailService emailService;
  @Mock private DacService dacService;
  @Mock private VoteDAO voteDAO;
  @Mock private MatchDAO matchDAO;
  @Mock private DataAccessRequestServiceDAO dataAccessRequestServiceDAO;
  @Mock private UserService userService;
  @Mock private InstitutionService institutionService;
  @Mock private DACAutomationRuleService ruleService;
  @Mock private ContainerRequest request;
  private DataAccessRequestService service;

  private static Collaborator createCollaborator() {
    return createCollaborator("collaborator@test.com");
  }

  private static Collaborator createCollaborator(String email) {
    return new Collaborator(null, email, null, null, null, null, "United States of America (the)");
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
    Collaborator collaborator = createCollaborator(goodEmailAddress);
    data.setInternalCollaborators(List.of(collaborator));
    Collaborator labStaffMember = createCollaborator(goodEmailAddress);
    data.setLabCollaborators(List.of(labStaffMember));
    return data;
  }

  @NotNull
  private static DataAccessRequestData getDataAccessRequestData(
      String goodEmailAddress, String badEmailAddress) {
    DataAccessRequestData data = new DataAccessRequestData();
    data.setPiEmail(goodEmailAddress);
    data.setSigningOfficialEmail(goodEmailAddress);
    data.setItDirectorEmail(goodEmailAddress);
    Collaborator collaborator1 = createCollaborator(goodEmailAddress);
    Collaborator collaborator2 = createCollaborator(goodEmailAddress);
    data.setLabCollaborators(List.of(collaborator1, collaborator2));
    Collaborator labStaffMember = createCollaborator(goodEmailAddress);
    Collaborator labStaffMember2 = createCollaborator(badEmailAddress);
    data.setLabCollaborators(List.of(labStaffMember, labStaffMember2));
    return data;
  }

  @BeforeEach
  void initService() {
    ConsentConfiguration config = new ConsentConfiguration();
    config.getServicesConfiguration().setLocalURL("local_url/");
    when(jdbi.onDemand(DatasetDAO.class)).thenReturn(dataSetDAO);
    when(jdbi.onDemand(DataAccessRequestDAO.class)).thenReturn(dataAccessRequestDAO);
    when(jdbi.onDemand(DarCollectionDAO.class)).thenReturn(darCollectionDAO);
    when(jdbi.onDemand(ElectionDAO.class)).thenReturn(electionDAO);
    when(jdbi.onDemand(MatchDAO.class)).thenReturn(matchDAO);
    when(jdbi.onDemand(VoteDAO.class)).thenReturn(voteDAO);
    when(jdbi.onDemand(UserDAO.class)).thenReturn(userDAO);
    when(jdbi.onDemand(DaaDAO.class)).thenReturn(daaDAO);
    service =
        new DataAccessRequestService(
            jdbi,
            dataAccessRequestServiceDAO,
            counterService,
            dacService,
            userService,
            institutionService,
            emailService,
            ruleService,
            new CountryValidator(),
            config);
  }

  @Test
  void testCreateDataAccessRequest_Update() {
    DataAccessRequest dar = generateDataAccessRequest();
    dar.setCollectionId(null);
    dar.addDatasetIds(List.of(1, 2, 3));
    User user = createUserWithPrerequisites();
    mockApprovedDatasets(dar.getDatasetIds());
    when(institutionService.findInstitutionForEmail(any())).thenReturn(user.getInstitution());
    when(counterService.getNextDarSequence()).thenReturn(1);
    when(dataAccessRequestDAO.findByReferenceId(any())).thenReturn(dar);
    doNothing()
        .when(dataAccessRequestDAO)
        .updateDataByReferenceId(any(), any(), any(), any(), any(), any());
    DataAccessRequest newDar = service.createDataAccessRequest(user, dar, request);
    assertNotNull(newDar);
  }

  @Test
  void testCreateDataAccessRequest_Create() {
    DataAccessRequest dar = generateDataAccessRequest();
    dar.addDatasetIds(List.of(1, 2, 3));
    dar.setCreateDate(new Timestamp(1000));
    dar.setReferenceId("id");
    User user = createUserWithPrerequisites();
    mockApprovedDatasets(dar.getDatasetIds());
    when(institutionService.findInstitutionForEmail(any())).thenReturn(user.getInstitution());
    when(counterService.getNextDarSequence()).thenReturn(1);
    when(dataAccessRequestDAO.findByReferenceId("id")).thenReturn(null);
    when(dataAccessRequestDAO.findByReferenceId(argThat(new LongerThanTwo()))).thenReturn(dar);
    when(darCollectionDAO.insertDarCollection(anyString(), anyInt(), any(Date.class)))
        .thenReturn(randomInt(1, 100));
    when(dataAccessRequestDAO.insertDataAccessRequest(
            anyInt(),
            anyString(),
            anyInt(),
            any(Date.class),
            any(Date.class),
            any(Date.class),
            any(DataAccessRequestData.class),
            anyString()))
        .thenReturn(1);
    when(dataSetDAO.filterDatasetIdsByAutomationRuleType(
            dar.getDatasetIds(), DACAutomationRuleType.REQUIRE_SO_DAR_APPROVAL.name()))
        .thenReturn(List.of());
    when(daaDAO.findDaaIdsByDatasetIds(anyList())).thenReturn(Set.of());
    DataAccessRequest newDar = service.createDataAccessRequest(user, dar, request);
    assertNotNull(newDar);
    verify(dataAccessRequestDAO, never()).updateRequiresSOApproval(eq(true), anyString());
  }

  @Test
  void testCreateDataAccessRequest_CapturesDatasetDaaSnapshots() {
    DataAccessRequest dar = generateDataAccessRequest();
    dar.setReferenceId("id");
    dar.setDatasetIds(List.of(1, 2));
    User user = createUserWithPrerequisites();
    mockApprovedDatasets(dar.getDatasetIds());
    when(institutionService.findInstitutionForEmail(any())).thenReturn(user.getInstitution());
    when(counterService.getNextDarSequence()).thenReturn(1);
    when(dataAccessRequestDAO.findByReferenceId("id")).thenReturn(null);
    when(dataAccessRequestDAO.findByReferenceId(argThat(new LongerThanTwo()))).thenReturn(dar);
    when(darCollectionDAO.insertDarCollection(anyString(), anyInt(), any(Date.class)))
        .thenReturn(99);
    when(dataAccessRequestDAO.insertDataAccessRequest(
            anyInt(),
            anyString(),
            anyInt(),
            any(Date.class),
            any(Date.class),
            any(Date.class),
            any(DataAccessRequestData.class),
            anyString()))
        .thenReturn(12);
    when(dataSetDAO.filterDatasetIdsByAutomationRuleType(
            dar.getDatasetIds(), DACAutomationRuleType.REQUIRE_SO_DAR_APPROVAL.name()))
        .thenReturn(List.of());
    when(daaDAO.findDaaIdsByDatasetIds(anyList())).thenReturn(Set.of());
    when(daaDAO.findCurrentDaaMappingsByDatasetIds(dar.getDatasetIds()))
        .thenReturn(List.of(new DatasetDaaMapping(1, 10), new DatasetDaaMapping(2, 20)));

    DataAccessRequest newDar = service.createDataAccessRequest(user, dar, request);

    assertNotNull(newDar);
    verify(daaDAO)
        .insertDarDatasetDaaSnapshots(
            argThat(
                snapshots ->
                    snapshots.size() == 2
                        && snapshots.stream().allMatch(snapshot -> snapshot.darId().equals(12))
                        && snapshots.stream()
                            .anyMatch(
                                snapshot ->
                                    snapshot.datasetId().equals(1)
                                        && snapshot.daaId().equals(10)
                                        && snapshot.capturedAt() != null)
                        && snapshots.stream()
                            .anyMatch(
                                snapshot ->
                                    snapshot.datasetId().equals(2)
                                        && snapshot.daaId().equals(20)
                                        && snapshot.capturedAt() != null)));
  }

  @Test
  void testCreateDataAccessRequest_Create_Missing_DAAs() {
    DataAccessRequest dar = generateDataAccessRequest();
    dar.addDatasetIds(List.of(1, 2, 3));
    dar.setCreateDate(new Timestamp(1000));
    dar.setReferenceId("id");
    User user = createUserWithPrerequisites();
    user.getLibraryCard().setDaaIds(List.of(2));
    dar.getData().setDaaIds(Set.of(1));
    mockApprovedDatasets(dar.getDatasetIds());
    when(institutionService.findInstitutionForEmail(any())).thenReturn(user.getInstitution());
    when(counterService.getNextDarSequence()).thenReturn(1);
    when(dataAccessRequestDAO.findByReferenceId("id")).thenReturn(null);
    when(dataAccessRequestDAO.findByReferenceId(argThat(new LongerThanTwo()))).thenReturn(dar);
    when(darCollectionDAO.insertDarCollection(anyString(), anyInt(), any(Date.class)))
        .thenReturn(randomInt(1, 100));
    when(dataAccessRequestDAO.insertDataAccessRequest(
            anyInt(),
            anyString(),
            anyInt(),
            any(Date.class),
            any(Date.class),
            any(Date.class),
            any(DataAccessRequestData.class),
            anyString()))
        .thenReturn(1);
    when(dataSetDAO.filterDatasetIdsByAutomationRuleType(
            dar.getDatasetIds(), DACAutomationRuleType.REQUIRE_SO_DAR_APPROVAL.name()))
        .thenReturn(List.of());
    when(daaDAO.findDaaIdsByDatasetIds(anyList())).thenReturn(Set.of(1));
    DataAccessRequest newDar = service.createDataAccessRequest(user, dar, request);
    assertNotNull(newDar);
    verify(dataAccessRequestDAO).updateRequiresSOApproval(eq(true), anyString());
  }

  @Test
  void testCreateDataAccessRequest_Create_SO_Approval_Required_By_Rule() {
    DataAccessRequest dar = generateDataAccessRequest();
    dar.addDatasetIds(List.of(1, 2, 3));
    dar.setCreateDate(new Timestamp(1000));
    dar.setReferenceId("id");
    User user = createUserWithPrerequisites();
    mockApprovedDatasets(dar.getDatasetIds());
    when(institutionService.findInstitutionForEmail(any())).thenReturn(user.getInstitution());
    when(counterService.getNextDarSequence()).thenReturn(1);
    when(dataAccessRequestDAO.findByReferenceId("id")).thenReturn(null);
    when(dataAccessRequestDAO.findByReferenceId(argThat(new LongerThanTwo()))).thenReturn(dar);
    when(darCollectionDAO.insertDarCollection(anyString(), anyInt(), any(Date.class)))
        .thenReturn(randomInt(1, 100));
    when(dataAccessRequestDAO.insertDataAccessRequest(
            anyInt(),
            anyString(),
            anyInt(),
            any(Date.class),
            any(Date.class),
            any(Date.class),
            any(DataAccessRequestData.class),
            anyString()))
        .thenReturn(1);
    when(dataSetDAO.filterDatasetIdsByAutomationRuleType(
            dar.getDatasetIds(), DACAutomationRuleType.REQUIRE_SO_DAR_APPROVAL.name()))
        .thenReturn(List.of(1, 2, 3));
    DataAccessRequest newDar = service.createDataAccessRequest(user, dar, request);
    assertNotNull(newDar);
    verify(dataAccessRequestDAO).updateRequiresSOApproval(eq(true), anyString());
  }

  @Test
  void testCreateDataAccessRequest_CreateWithSubmittedDar() {
    DataAccessRequest dar = generateDataAccessRequest();
    dar.addDatasetIds(List.of(1, 2, 3));
    dar.setCreateDate(new Timestamp(1000));
    dar.setReferenceId("id");
    dar.setSubmissionDate(FIXED_TIMESTAMP);
    User user = createUserWithPrerequisites();
    mockApprovedDatasets(dar.getDatasetIds());
    when(institutionService.findInstitutionForEmail(any())).thenReturn(user.getInstitution());
    when(dataAccessRequestDAO.findByReferenceId(any())).thenReturn(dar);
    assertThrows(
        SubmittedDARCannotBeEditedException.class,
        () -> service.createDataAccessRequest(user, dar, request));
  }

  @Test
  void testCreateDataAccessRequestCreateWithoutERACommons() {
    DataAccessRequest dar = generateDataAccessRequest();
    User user = createUserWithPrerequisites();
    mockApprovedDatasets(dar.getDatasetIds());
    doThrow(BadRequestException.class).when(userService).validateActiveERACredentials(user);
    assertThrows(
        BadRequestException.class, () -> service.createDataAccessRequest(user, dar, request));
  }

  @Test
  void testUpdateByReferenceIdThrowsOnDraft() {
    DataAccessRequest dar = generateDataAccessRequest();
    dar.setCollectionId(randomInt(0, 100));
    User user = new User(1, "email@test.org", "Display Name", FIXED_DATE);
    dar.addDatasetIds(List.of(1, 2, 3));
    dar.setSubmissionDate(FIXED_TIMESTAMP);
    assertThrows(
        SubmittedDARCannotBeEditedException.class, () -> service.updateByReferenceId(user, dar));
  }

  @Test
  void testCreateDataAccessRequest_FailsIfNoLibraryCard() {
    DataAccessRequest dar = generateDataAccessRequest();
    dar.addDatasetIds(List.of(1, 2, 3));
    dar.setCreateDate(new Timestamp(1000));
    dar.setReferenceId("id");
    User user = new User(1, "email@test.org", "Display Name", FIXED_DATE);
    assertThrows(
        NIHComplianceRuleException.class,
        () -> service.createDataAccessRequest(user, dar, request));
  }

  @Test
  void createProgressReport() {
    DataAccessRequest parentDar = generateDataAccessRequest();
    DataAccessRequest progressReport = generateProgressReport();
    progressReport.setParentId(parentDar.getId());
    progressReport.setCollectionId(parentDar.getCollectionId());
    parentDar.setSubmissionDate(FIXED_TIMESTAMP);
    User user = createUserWithPrerequisites();
    mockApprovedDatasets(progressReport.getDatasetIds());
    parentDar.setUserId(user.getUserId());
    when(dataAccessRequestDAO.findByReferenceId(progressReport.getReferenceId()))
        .thenReturn(progressReport);
    when(dataAccessRequestDAO.findDatasetApprovalsByDar(parentDar.getReferenceId()))
        .thenReturn(Set.copyOf(progressReport.getDatasetIds()));
    DataAccessRequest newDar =
        service.createProgressReport(user, progressReport, parentDar, request);
    assertNotNull(newDar);
    verify(dataAccessRequestDAO)
        .insertProgressReport(
            parentDar.getId(),
            progressReport.getCollectionId(),
            progressReport.getReferenceId(),
            user.getUserId(),
            progressReport.getData(),
            user.getEraCommonsId());
    verify(ruleService)
        .triggerDACRuleSettings(
            user, progressReport.getDatasetIds(), progressReport.getReferenceId(), request);
    verify(dataAccessRequestDAO)
        .insertAllDarDatasets(argThat(new DarDatasetMatcher(progressReport)));
  }

  @Test
  void createProgressReportCapturesDatasetDaaSnapshots() {
    DataAccessRequest parentDar = generateDataAccessRequest();
    DataAccessRequest progressReport = generateProgressReport();
    progressReport.setParentId(parentDar.getId());
    progressReport.setCollectionId(parentDar.getCollectionId());
    progressReport.setDatasetIds(List.of(1, 2));
    parentDar.setSubmissionDate(FIXED_TIMESTAMP);
    parentDar.setUserId(1);
    parentDar.setDatasetIds(List.of(1, 2));
    User user = createUserWithPrerequisites();
    mockApprovedDatasets(progressReport.getDatasetIds());
    when(dataAccessRequestDAO.findByReferenceId(progressReport.getReferenceId()))
        .thenReturn(progressReport);
    when(dataAccessRequestDAO.findDatasetApprovalsByDar(parentDar.getReferenceId()))
        .thenReturn(Set.copyOf(progressReport.getDatasetIds()));
    when(daaDAO.findCurrentDaaMappingsByDatasetIds(progressReport.getDatasetIds()))
        .thenReturn(List.of(new DatasetDaaMapping(1, 10), new DatasetDaaMapping(2, 20)));

    DataAccessRequest newDar =
        service.createProgressReport(user, progressReport, parentDar, request);

    assertNotNull(newDar);
    verify(daaDAO)
        .insertDarDatasetDaaSnapshots(
            argThat(
                snapshots ->
                    snapshots.size() == 2
                        && snapshots.stream().allMatch(snapshot -> snapshot.darId() != null)
                        && snapshots.stream()
                            .anyMatch(
                                snapshot ->
                                    snapshot.datasetId().equals(1)
                                        && snapshot.daaId().equals(10)
                                        && snapshot.capturedAt() != null)
                        && snapshots.stream()
                            .anyMatch(
                                snapshot ->
                                    snapshot.datasetId().equals(2)
                                        && snapshot.daaId().equals(20)
                                        && snapshot.capturedAt() != null)));
  }

  @Test
  void createProgressReportDmi() {
    DataAccessRequest parentDar = generateDataAccessRequest();
    DataAccessRequest progressReport = generateProgressReport();
    progressReport.setParentId(parentDar.getId());
    progressReport.setCollectionId(parentDar.getCollectionId());
    progressReport.getData().setDmi(new DataManagementIncident(List.of("incident 1"), "A bad day"));
    parentDar.setSubmissionDate(FIXED_TIMESTAMP);
    User user = createUserWithPrerequisites();
    mockApprovedDatasets(progressReport.getDatasetIds());
    parentDar.setUserId(user.getUserId());
    when(dataAccessRequestDAO.findByReferenceId(progressReport.getReferenceId()))
        .thenReturn(progressReport);
    when(dataAccessRequestDAO.findDatasetApprovalsByDar(parentDar.getReferenceId()))
        .thenReturn(Set.copyOf(progressReport.getDatasetIds()));
    when(daaDAO.findDaaIdsByDatasetIds(progressReport.getDatasetIds())).thenReturn(Set.of(1));
    DataAccessRequest newDar =
        service.createProgressReport(user, progressReport, parentDar, request);
    assertNotNull(newDar);
    verify(dataAccessRequestDAO)
        .insertProgressReport(
            parentDar.getId(),
            progressReport.getCollectionId(),
            progressReport.getReferenceId(),
            user.getUserId(),
            progressReport.getData(),
            user.getEraCommonsId());
    verify(ruleService, never())
        .triggerDACRuleSettings(
            user, progressReport.getDatasetIds(), progressReport.getReferenceId(), request);
    verify(dataAccessRequestDAO)
        .insertAllDarDatasets(argThat(new DarDatasetMatcher(progressReport)));
  }

  @Test
  void createProgressReportNotPreAuthed() {
    DataAccessRequest parentDar = generateDataAccessRequest();
    DataAccessRequest progressReport = generateProgressReport();
    progressReport.setParentId(parentDar.getId());
    progressReport.setCollectionId(parentDar.getCollectionId());
    progressReport.getData().setDaaIds(Set.of(1));
    parentDar.setSubmissionDate(FIXED_TIMESTAMP);
    User user = createUserWithPrerequisites();
    user.getLibraryCard().setDaaIds(List.of(2));
    mockApprovedDatasets(progressReport.getDatasetIds());
    parentDar.setUserId(user.getUserId());
    when(dataAccessRequestDAO.findByReferenceId(progressReport.getReferenceId()))
        .thenReturn(progressReport);
    when(dataAccessRequestDAO.findDatasetApprovalsByDar(parentDar.getReferenceId()))
        .thenReturn(Set.copyOf(progressReport.getDatasetIds()));
    when(daaDAO.findDaaIdsByDatasetIds(progressReport.getDatasetIds())).thenReturn(Set.of(1));
    DataAccessRequest newDar =
        service.createProgressReport(user, progressReport, parentDar, request);
    assertNotNull(newDar);
    verify(dataAccessRequestDAO)
        .insertProgressReport(
            parentDar.getId(),
            progressReport.getCollectionId(),
            progressReport.getReferenceId(),
            user.getUserId(),
            progressReport.getData(),
            user.getEraCommonsId());
    verify(ruleService, never())
        .triggerDACRuleSettings(
            user, progressReport.getDatasetIds(), progressReport.getReferenceId(), request);
    verify(dataAccessRequestDAO)
        .insertAllDarDatasets(argThat(new DarDatasetMatcher(progressReport)));
  }

  @Test
  void createCloseoutProgressReport() throws TemplateException, IOException {
    User user = createUserWithPrerequisites();
    User signingOfficial = createUserWithPrerequisites();
    signingOfficial.setInstitutionId(user.getInstitutionId());
    signingOfficial.setSigningOfficialRole();
    DataAccessRequest progressReport = generateProgressReport();
    DataAccessRequest parentDar = generateDataAccessRequest();
    parentDar.setSubmissionDate(FIXED_TIMESTAMP);
    parentDar.setUserId(user.getUserId());
    progressReport.setDatasetIds(List.of(3, 4, 5));
    parentDar.setDatasetIds(List.of(3, 4, 5));
    progressReport.setSubmissionDate(FIXED_TIMESTAMP);
    progressReport.setParentId(parentDar.getId());
    progressReport.setCollectionId(parentDar.getCollectionId());
    progressReport.getData().setCloseoutSupplement(new CloseoutSupplement(List.of("test"), "", 2));
    mockApprovedDatasets(progressReport.getDatasetIds());

    when(userService.findUserById(2)).thenReturn(signingOfficial);
    when(dataAccessRequestDAO.findByReferenceId(progressReport.getReferenceId()))
        .thenReturn(progressReport);
    when(dataAccessRequestDAO.findDatasetApprovalsByDar(parentDar.getReferenceId()))
        .thenReturn(Set.copyOf(progressReport.getDatasetIds()));

    DataAccessRequest newDar =
        service.createProgressReport(user, progressReport, parentDar, request);

    assertNotNull(newDar);
    verify(emailService).sendMessage(any(SubmittedCloseoutMessage.class), any());
    verify(dataAccessRequestDAO)
        .insertProgressReport(
            parentDar.getId(),
            progressReport.getCollectionId(),
            progressReport.getReferenceId(),
            user.getUserId(),
            progressReport.getData(),
            user.getEraCommonsId());
    verify(ruleService, never()).triggerDACRuleSettings(any(), any(), any(), any());
    verify(dataAccessRequestDAO)
        .insertAllDarDatasets(argThat(new DarDatasetMatcher(progressReport)));
    verify(dataAccessRequestDAO, never()).updateRequiresSOApproval(eq(true), anyString());
    verify(daaDAO, never()).insertDarDatasetDaaSnapshots(any());
  }

  @Test
  void findDatasetDaaSnapshotsByReferenceId() {
    DataAccessRequest dar = generateDataAccessRequest();
    Timestamp capturedAt = FIXED_TIMESTAMP;
    when(dataAccessRequestDAO.findByReferenceId(dar.getReferenceId())).thenReturn(dar);
    when(daaDAO.findDatasetDaaSnapshotsByReferenceId(dar.getReferenceId()))
        .thenReturn(
            List.of(
                new DatasetDaaSnapshotDetail(1, 10, capturedAt),
                new DatasetDaaSnapshotDetail(2, 20, capturedAt)));

    Map<Integer, DatasetDaaSnapshot> snapshots =
        service.findDatasetDaaSnapshotsByReferenceId(dar.getReferenceId());

    assertEquals(2, snapshots.size());
    assertEquals(new DatasetDaaSnapshot(10, capturedAt), snapshots.get(1));
    assertEquals(new DatasetDaaSnapshot(20, capturedAt), snapshots.get(2));
  }

  @Test
  void findDatasetDaaSnapshotsByReferenceIdNotFound() {
    DataAccessRequest dar = generateDataAccessRequest();
    when(dataAccessRequestDAO.findByReferenceId(dar.getReferenceId())).thenReturn(dar);
    when(daaDAO.findDatasetDaaSnapshotsByReferenceId(dar.getReferenceId())).thenReturn(List.of());

    assertThrows(
        NotFoundException.class,
        () -> service.findDatasetDaaSnapshotsByReferenceId(dar.getReferenceId()));
  }

  @Test
  void createProgressReportFailsIfNonApprovedDatasets() {
    DataAccessRequest parentDar = generateDataAccessRequest();
    DataAccessRequest progressReport = generateProgressReport();
    progressReport.setParentId(parentDar.getId());
    progressReport.setCollectionId(parentDar.getCollectionId());
    parentDar.setSubmissionDate(FIXED_TIMESTAMP);
    User user = createUserWithPrerequisites();
    mockApprovedDatasets(progressReport.getDatasetIds());
    parentDar.setUserId(user.getUserId());
    when(dataAccessRequestDAO.findDatasetApprovalsByDar(parentDar.getReferenceId()))
        .thenReturn(Set.of());
    assertThrows(
        BadRequestException.class,
        () -> service.createProgressReport(user, progressReport, parentDar, request));
  }

  @Test
  void createProgressReportFailsIfDAOOperationFails() {
    DataAccessRequest parentDar = generateDataAccessRequest();
    DataAccessRequest progressReport = generateProgressReport();
    progressReport.setParentId(parentDar.getId());
    progressReport.setCollectionId(parentDar.getCollectionId());
    parentDar.setSubmissionDate(FIXED_TIMESTAMP);
    User user = createUserWithPrerequisites();
    mockApprovedDatasets(progressReport.getDatasetIds());
    parentDar.setUserId(user.getUserId());
    when(dataAccessRequestDAO.findDatasetApprovalsByDar(parentDar.getReferenceId()))
        .thenReturn(Set.copyOf(progressReport.getDatasetIds()));
    doThrow(new UnableToExecuteStatementException("Test exception"))
        .when(dataAccessRequestDAO)
        .insertProgressReport(
            parentDar.getId(),
            parentDar.getCollectionId(),
            progressReport.referenceId,
            user.getUserId(),
            progressReport.data,
            user.getEraCommonsId());
    assertThrows(
        BadRequestException.class,
        () -> service.createProgressReport(user, progressReport, parentDar, request));
  }

  private User createRequestingUser() {
    User requestingUser = new User(1, "requestor@test.com", "Requestor", FIXED_DATE, roles);
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
    mockApprovedDatasets(progressReport.getDatasetIds());
    DataAccessRequest parentDar = generateDataAccessRequest();
    BadRequestException exception =
        assertThrows(
            BadRequestException.class,
            () -> service.validateProgressReport(user, progressReport, parentDar));
    assertThat(
        exception.getMessage(),
        containsString("Cannot create a progress report for a draft Data Access Request"));
  }

  @Test
  void validateProgressReportNoDatasetIds() {
    User user = createUserWithPrerequisites();
    DataAccessRequest progressReport = generateProgressReport();
    progressReport.setDatasetIds(Collections.emptyList());
    mockApprovedDatasets(progressReport.getDatasetIds());
    DataAccessRequest parentDar = generateDataAccessRequest();
    parentDar.setSubmissionDate(FIXED_TIMESTAMP);
    parentDar.setUserId(user.getUserId());
    BadRequestException exception =
        assertThrows(
            BadRequestException.class,
            () -> service.validateProgressReport(user, progressReport, parentDar));
    assertThat(exception.getMessage(), containsString("At least one dataset is required"));
  }

  @Test
  void validateProgressReportNoSummary() {
    User user = createUserWithPrerequisites();
    DataAccessRequest progressReport = generateProgressReport();
    progressReport.getData().setProgressReportSummary(null);
    mockApprovedDatasets(progressReport.getDatasetIds());
    DataAccessRequest parentDar = generateDataAccessRequest();
    parentDar.setSubmissionDate(FIXED_TIMESTAMP);
    parentDar.setUserId(user.getUserId());
    BadRequestException exception =
        assertThrows(
            BadRequestException.class,
            () -> service.validateProgressReport(user, progressReport, parentDar));
    assertThat(exception.getMessage(), containsString("Progress report summary is required"));
  }

  @Test
  void validateProgressReportInvalidDatasetIds() {
    User user = createUserWithPrerequisites();
    DataAccessRequest progressReport = generateProgressReport();
    progressReport.setDatasetIds(List.of(3, 4, 5)); // IDs not all in parent DAR
    mockApprovedDatasets(progressReport.getDatasetIds());
    DataAccessRequest parentDar = generateDataAccessRequest();
    parentDar.setSubmissionDate(FIXED_TIMESTAMP);
    parentDar.setDatasetIds(List.of(1, 2, 3));
    parentDar.setUserId(user.getUserId());
    BadRequestException exception =
        assertThrows(
            BadRequestException.class,
            () -> service.validateProgressReport(user, progressReport, parentDar));
    assertThat(
        exception.getMessage(),
        containsString("Progress report can only be created for datasets in the parent DAR"));
  }

  @Test
  void validateProgressReportCloseoutNotFoundSO() {
    User user = createUserWithPrerequisites();
    DataAccessRequest progressReport = generateProgressReport();
    DataAccessRequest parentDar = generateDataAccessRequest();
    parentDar.setSubmissionDate(FIXED_TIMESTAMP);
    parentDar.setUserId(user.getUserId());
    progressReport.setDatasetIds(List.of(3, 4, 5));
    parentDar.setDatasetIds(List.of(3, 4, 5));
    progressReport.setSubmissionDate(FIXED_TIMESTAMP);
    progressReport.setParentId(parentDar.getId());
    progressReport.getData().setCloseoutSupplement(new CloseoutSupplement(List.of("test"), "", 2));
    mockApprovedDatasets(progressReport.getDatasetIds());

    doThrow(NotFoundException.class).when(userService).findUserById(2);

    BadRequestException exception =
        assertThrows(
            BadRequestException.class,
            () -> service.validateProgressReport(user, progressReport, parentDar));
    assertThat(
        exception.getMessage(),
        containsString("The selected signing official in the closeout was not found."));
  }

  @Test
  void validateProgressReportCloseoutNotSameInstitutionSO() {
    User user = createUserWithPrerequisites();
    User signingOfficial = createUserWithPrerequisites();
    signingOfficial.setInstitutionId(user.getInstitutionId() + 1);
    DataAccessRequest progressReport = generateProgressReport();
    DataAccessRequest parentDar = generateDataAccessRequest();
    parentDar.setSubmissionDate(FIXED_TIMESTAMP);
    parentDar.setUserId(user.getUserId());
    progressReport.setDatasetIds(List.of(3, 4, 5));
    parentDar.setDatasetIds(List.of(3, 4, 5));
    progressReport.setSubmissionDate(FIXED_TIMESTAMP);
    progressReport.setParentId(parentDar.getId());
    progressReport.getData().setCloseoutSupplement(new CloseoutSupplement(List.of("test"), "", 2));
    mockApprovedDatasets(progressReport.getDatasetIds());

    when(userService.findUserById(2)).thenReturn(signingOfficial);

    BadRequestException exception =
        assertThrows(
            BadRequestException.class,
            () -> service.validateProgressReport(user, progressReport, parentDar));
    assertThat(
        exception.getMessage(),
        containsString(
            "The signing official selected in the closeout is not in the same institution as the submitter."));
  }

  @Test
  void validateProgressReportCloseoutSameInstitutionNotAnSO() {
    User user = createUserWithPrerequisites();
    User signingOfficial = createUserWithPrerequisites();
    signingOfficial.setInstitutionId(user.getInstitutionId());
    DataAccessRequest progressReport = generateProgressReport();
    DataAccessRequest parentDar = generateDataAccessRequest();
    parentDar.setSubmissionDate(FIXED_TIMESTAMP);
    parentDar.setUserId(user.getUserId());
    progressReport.setDatasetIds(List.of(3, 4, 5));
    parentDar.setDatasetIds(List.of(3, 4, 5));
    progressReport.setSubmissionDate(FIXED_TIMESTAMP);
    progressReport.setParentId(parentDar.getId());
    progressReport.getData().setCloseoutSupplement(new CloseoutSupplement(List.of("test"), "", 2));
    mockApprovedDatasets(progressReport.getDatasetIds());

    when(userService.findUserById(2)).thenReturn(signingOfficial);

    BadRequestException exception =
        assertThrows(
            BadRequestException.class,
            () -> service.validateProgressReport(user, progressReport, parentDar));
    assertThat(
        exception.getMessage(),
        containsString("The selected signing official is not a signing official"));
  }

  @Test
  void validateProgressReportCloseout() {
    User user = createUserWithPrerequisites();
    User signingOfficial = createUserWithPrerequisites();
    signingOfficial.setInstitutionId(user.getInstitutionId());
    signingOfficial.setSigningOfficialRole();
    DataAccessRequest progressReport = generateProgressReport();
    DataAccessRequest parentDar = generateDataAccessRequest();
    parentDar.setSubmissionDate(FIXED_TIMESTAMP);
    parentDar.setUserId(user.getUserId());
    progressReport.setDatasetIds(List.of(3, 4, 5));
    parentDar.setDatasetIds(List.of(3, 4, 5));
    progressReport.setSubmissionDate(FIXED_TIMESTAMP);
    progressReport.setParentId(parentDar.getId());
    progressReport.getData().setCloseoutSupplement(new CloseoutSupplement(List.of("test"), "", 2));
    mockApprovedDatasets(progressReport.getDatasetIds());

    when(userService.findUserById(2)).thenReturn(signingOfficial);

    assertDoesNotThrow(() -> service.validateProgressReport(user, progressReport, parentDar));
  }

  @Test
  void validateProgressReport() {
    User user = createUserWithPrerequisites();
    DataAccessRequest progressReport = generateProgressReport();
    progressReport.setDatasetIds(List.of(1, 2));
    mockApprovedDatasets(progressReport.getDatasetIds());
    DataAccessRequest parentDar = generateDataAccessRequest();
    parentDar.setSubmissionDate(FIXED_TIMESTAMP);
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
    mockApprovedDatasets(progressReport.getDatasetIds());
    DataAccessRequestData progressReportData = progressReport.getData();
    Collaborator collaborator1 = createCollaborator("1" + user.getEmail());
    progressReportData.setInternalCollaborators(Collections.singletonList(collaborator1));
    Collaborator collaborator2 = createCollaborator("2" + user.getEmail());
    progressReportData.setLabCollaborators(Collections.singletonList(collaborator2));
    User collaborator1User = createUserWithPrerequisites();
    collaborator1User.setEmail(collaborator1.email());
    collaborator1User.setInstitutionId(user.getInstitutionId());
    collaborator1User.setLibraryCard(new LibraryCard());
    User collaborator2User = createUserWithPrerequisites();
    collaborator2User.setEmail(collaborator2.email());
    collaborator2User.setInstitutionId(user.getInstitutionId());
    collaborator2User.setLibraryCard(new LibraryCard());
    DataAccessRequest parentDar = generateDataAccessRequest();
    parentDar.setSubmissionDate(FIXED_TIMESTAMP);
    parentDar.setDatasetIds(List.of(1, 2, 3));
    parentDar.setUserId(user.getUserId());
    when(userDAO.findUserByEmail(collaborator1.email())).thenReturn(collaborator1User);
    when(userDAO.findUserByEmail(collaborator2.email())).thenReturn(collaborator2User);
    when(institutionService.findInstitutionForEmail(any())).thenReturn(user.getInstitution());
    assertDoesNotThrow(() -> service.validateProgressReport(user, progressReport, parentDar));
  }

  @Test
  void validateProgressReportWithCollaboratorsAndStaffInInvalidInstitution() {
    User user = createUserWithPrerequisites();
    DataAccessRequest progressReport = generateProgressReport();
    progressReport.setDatasetIds(List.of(1, 2));
    mockApprovedDatasets(progressReport.getDatasetIds());
    DataAccessRequestData progressReportData = progressReport.getData();
    Collaborator collaborator1 = createCollaborator("alice@1otherdomain.org");
    progressReportData.setInternalCollaborators(Collections.singletonList(collaborator1));
    Collaborator collaborator2 = createCollaborator("eve@yetanotherdomain.org");
    progressReportData.setLabCollaborators(Collections.singletonList(collaborator2));
    DataAccessRequest parentDar = generateDataAccessRequest();
    parentDar.setSubmissionDate(FIXED_TIMESTAMP);
    parentDar.setDatasetIds(List.of(1, 2, 3));
    parentDar.setUserId(user.getUserId());
    when(userDAO.findUserByEmail(collaborator1.email())).thenReturn(user);
    when(institutionService.findInstitutionForEmail(any())).thenReturn(null);
    BadRequestException badRequestException =
        assertThrows(
            BadRequestException.class,
            () -> service.validateProgressReport(user, progressReport, parentDar));
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
  void validateCommonNullUserThrows() {
    DataAccessRequest dar = generateDataAccessRequest();
    assertThrows(
        IllegalArgumentException.class,
        () -> service.validateCommonDarAndProgressReportElements(null, dar));
  }

  @Test
  void validateCommonNullDarThrows() {
    User user = createUserWithPrerequisites();
    assertThrows(
        IllegalArgumentException.class,
        () -> service.validateCommonDarAndProgressReportElements(user, null));
  }

  @Test
  void validateCommonNullDarDataThrows() {
    User user = createUserWithPrerequisites();
    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(UUID.randomUUID().toString());
    dar.setData(null);
    assertThrows(
        IllegalArgumentException.class,
        () -> service.validateCommonDarAndProgressReportElements(user, dar));
  }

  @Test
  void validateCommonNullReferenceIdThrows() {
    User user = createUserWithPrerequisites();
    DataAccessRequest dar = generateDataAccessRequest();
    dar.setReferenceId(null);
    assertThrows(
        IllegalArgumentException.class,
        () -> service.validateCommonDarAndProgressReportElements(user, dar));
  }

  @Test
  void validateCommonDoesNotThrow() {
    User validUser = createUserWithPrerequisites();
    DataAccessRequest validDar = generateDataAccessRequest();
    when(dataSetDAO.findDatasetsByIdList(validDar.getDatasetIds()))
        .thenReturn(List.of(approvedDataset(validDar.getDatasetIds().getFirst())));
    assertDoesNotThrow(
        () -> service.validateCommonDarAndProgressReportElements(validUser, validDar));
  }

  @Test
  void validateCommonNoLibraryCard() {
    User validUser = createRequestingUser();
    DataAccessRequest validDar = generateDataAccessRequest();
    validDar.getData().setPiName(validUser.getDisplayName());
    validDar.getData().setPiEmail(validUser.getEmail());
    assertThrows(
        NIHComplianceRuleException.class,
        () -> service.validateCommonDarAndProgressReportElements(validUser, validDar));
  }

  @Test
  void validateCommonDatasetsNotApprovedThrows() {
    User validUser = createUserWithPrerequisites();
    DataAccessRequest validDar = generateDataAccessRequest();
    when(dataSetDAO.findDatasetsByIdList(validDar.getDatasetIds()))
        .thenReturn(List.of(unapprovedDataset(validDar.getDatasetIds().getFirst())));

    BadRequestException exception =
        assertThrows(
            BadRequestException.class,
            () -> service.validateCommonDarAndProgressReportElements(validUser, validDar));
    assertThat(
        exception.getMessage(),
        containsString(
            "All datasets in the DAR must be approved by their respective DAC to create a data access request or progress report."));
  }

  @Test
  void validateRequestDatasetsAreApprovedDoesNotThrowWhenAllApproved() {
    DataAccessRequest dar = generateDataAccessRequest();
    Integer datasetId = dar.getDatasetIds().getFirst();
    when(dataSetDAO.findDatasetsByIdList(dar.getDatasetIds()))
        .thenReturn(List.of(approvedDataset(datasetId)));

    assertDoesNotThrow(() -> service.validateRequestDatasetsAreApproved(dar));
  }

  @Test
  void validateRequestDatasetsAreApprovedThrowsWhenAnyDatasetUnapproved() {
    DataAccessRequest dar = generateDataAccessRequest();
    Integer datasetId = dar.getDatasetIds().getFirst();
    when(dataSetDAO.findDatasetsByIdList(dar.getDatasetIds()))
        .thenReturn(List.of(unapprovedDataset(datasetId)));

    BadRequestException exception =
        assertThrows(
            BadRequestException.class, () -> service.validateRequestDatasetsAreApproved(dar));
    assertThat(
        exception.getMessage(),
        containsString(
            "All datasets in the DAR must be approved by their respective DAC to create a data access request or progress report."));
  }

  @Test
  void validateDarNullUser() {
    DataAccessRequest dar = generateDataAccessRequest();
    assertThrows(IllegalArgumentException.class, () -> service.validateDar(null, dar));
  }

  @Test
  void validateDarNullDar() {
    User user = new User(1, "email@test.org", "Display Name", FIXED_DATE);
    assertThrows(IllegalArgumentException.class, () -> service.validateDar(user, null));
  }

  @Test
  void validateDarNullReferenceId() {
    DataAccessRequest dar = generateDataAccessRequest();
    dar.setReferenceId(null);
    User user = new User(1, "email@test.org", "Display Name", FIXED_DATE);
    assertThrows(IllegalArgumentException.class, () -> service.validateDar(user, dar));
  }

  @Test
  void validateDarNullData() {
    DataAccessRequest dar = generateDataAccessRequest();
    dar.setData(null);
    User user = new User(1, "email@test.org", "Display Name", FIXED_DATE);
    assertThrows(IllegalArgumentException.class, () -> service.validateDar(user, dar));
  }

  @Test
  void validateDarNoLibraryCards() {
    DataAccessRequest dar = generateDataAccessRequest();
    User user = new User(1, "email@test.org", "Display Name", FIXED_DATE);
    assertThrows(NIHComplianceRuleException.class, () -> service.validateDar(user, dar));
  }

  @Test
  void validateDarDifferentPiEmail() {
    User validUser = createUserWithPrerequisites();
    DataAccessRequest dar = generateDataAccessRequest();
    dar.getData().setPiEmail("otheremail@example.com");
    assertThrows(BadRequestException.class, () -> service.validateDar(validUser, dar));
  }

  @Test
  void validateDarDifferentPiName() {
    User validUser = createUserWithPrerequisites();
    DataAccessRequest dar = generateDataAccessRequest();
    dar.getData().setPiName("Other Name");
    assertThrows(BadRequestException.class, () -> service.validateDar(validUser, dar));
  }

  @Test
  void validateDar() {
    DataAccessRequest dar = generateDataAccessRequest();
    User user = createUserWithPrerequisites();
    mockApprovedDatasets(dar.getDatasetIds());
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
    User collaboratorUser =
        new User(2, validCollaborator.email(), "Collaborator", FIXED_DATE, roles);
    collaboratorUser.setInstitutionId(requestingUser.getInstitutionId());
    LibraryCard libraryCard = new LibraryCard();
    collaboratorUser.setLibraryCard(libraryCard);
    DataAccessRequest dar = createDataAccessRequest(List.of(validCollaborator));
    when(institutionService.findInstitutionForEmail(collaboratorUser.getEmail()))
        .thenReturn(requestingUser.getInstitution());
    when(userDAO.findUserByEmail(validCollaborator.email())).thenReturn(collaboratorUser);

    assertDoesNotThrow(() -> service.validateInternalCollaborators(requestingUser, dar));
  }

  @Test
  void testValidateInternalCollaboratorsLibraryCard_NoInstitution() {
    User requestingUser = createRequestingUser();
    Collaborator validCollaborator = createCollaborator();
    User collaboratorUser =
        new User(2, validCollaborator.email(), "Collaborator", FIXED_DATE, roles);
    collaboratorUser.setInstitutionId(requestingUser.getInstitutionId());
    LibraryCard libraryCard = new LibraryCard();
    collaboratorUser.setLibraryCard(libraryCard);
    DataAccessRequest dar = createDataAccessRequest(List.of(validCollaborator));
    when(userDAO.findUserByEmail(validCollaborator.email())).thenReturn(collaboratorUser);

    BadRequestException exception =
        assertThrows(
            BadRequestException.class,
            () -> service.validateInternalCollaborators(requestingUser, dar));
    assertEquals(
        """
 All listed personnel must share the same institutional affiliation and have a library card.  \
 The following list of roles and members must have email addresses associated with your \
 institution or library cards issued: Internal Collaborator member:  (missing institution) %s\
 """
            .formatted(collaboratorUser.getEmail()),
        exception.getMessage());
  }

  @Test
  void testValidateInternalCollaboratorsDoesNotExist() {
    User requestingUser = createRequestingUser();
    Collaborator invalidCollaborator = createCollaborator();
    DataAccessRequest dar = createDataAccessRequest(List.of(invalidCollaborator));
    when(userDAO.findUserByEmail(invalidCollaborator.email())).thenReturn(null);

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
            .formatted(invalidCollaborator.email(), invalidCollaborator.email()),
        exception.getMessage());
  }

  @Test
  void testValidateInternalCollaboratorsNoLibraryCard() {
    User requestingUser = createRequestingUser();
    Collaborator invalidCollaborator = createCollaborator();
    User collaboratorUser =
        new User(2, invalidCollaborator.email(), "Collaborator", FIXED_DATE, roles);
    collaboratorUser.setInstitutionId(requestingUser.getInstitutionId());
    DataAccessRequest dar = createDataAccessRequest(List.of(invalidCollaborator));
    when(userDAO.findUserByEmail(invalidCollaborator.email())).thenReturn(collaboratorUser);
    when(institutionService.findInstitutionForEmail(invalidCollaborator.email()))
        .thenReturn(requestingUser.getInstitution());

    BadRequestException exception =
        assertThrows(
            BadRequestException.class,
            () -> service.validateInternalCollaborators(requestingUser, dar));
    assertEquals(
"""
All listed personnel must share the same institutional affiliation and have a library card.  \
The following list of roles and members must have email addresses associated with your \
institution or library cards issued: Internal Collaborator member:  \
(missing library card) %s\
"""
            .formatted(collaboratorUser.getEmail()),
        exception.getMessage());
  }

  @Test
  void testUpdateByReferenceIdVersion2() throws Exception {
    DataAccessRequest dar = generateDataAccessRequest();
    dar.setCollectionId(randomInt(0, 100));
    User user = new User(1, "email@test.org", "Display Name", FIXED_DATE);
    dar.addDatasetIds(List.of(1, 2, 3));
    when(dataAccessRequestServiceDAO.updateByReferenceId(any(), any())).thenReturn(dar);
    DataAccessRequest newDar = service.updateByReferenceId(user, dar);
    assertNotNull(newDar);
  }

  @Test
  void testUpdateByReferenceIdVersion2_WithCollection() throws Exception {
    DataAccessRequest dar = generateDataAccessRequest();
    User user = new User(1, "email@test.org", "Display Name", FIXED_DATE);
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
    when(dataAccessRequestDAO.findApprovedDARsByDatasetId(d.getDatasetId()))
        .thenReturn(List.of(dar1, dar2));

    assertEquals(List.of(dar1, dar2), service.getApprovedDARsForDataset(d));
  }

  @Test
  void testHasAcknowledgedRequiredDaas() {
    Dataset d = new Dataset();
    d.setDatasetId(10);
    Integer daaId = 1;
    DataAccessRequest dar = new DataAccessRequest();
    dar.setDatasetIds(List.of(d.getDatasetId()));
    DataAccessRequestData darData = new DataAccessRequestData();
    darData.setDaaIds(Set.of(daaId));
    dar.setData(darData);
    when(daaDAO.findDaaIdsByDatasetIds(List.of(d.getDatasetId()))).thenReturn(Set.of(daaId));

    assertDoesNotThrow(() -> service.hasAcknowledgedRequiredDaas(dar));
  }

  @Test
  void testHasAcknowledgedRequiredDaas_No_DAA_Submitted() {
    Dataset d = new Dataset();
    d.setDatasetId(10);
    Integer daaId = 1;
    DataAccessRequest dar = new DataAccessRequest();
    dar.setDatasetIds(List.of(d.getDatasetId()));
    DataAccessRequestData darData = new DataAccessRequestData();
    darData.setDaaIds(Set.of());
    dar.setData(darData);
    when(daaDAO.findDaaIdsByDatasetIds(List.of(d.getDatasetId()))).thenReturn(Set.of(daaId));

    assertThrows(BadRequestException.class, () -> service.hasAcknowledgedRequiredDaas(dar));
  }

  @Test
  void testHasRequiredDaas_No_DAA_Acknowledged_Required() {
    Dataset d = new Dataset();
    d.setDatasetId(10);
    Integer daaId = 1;
    DataAccessRequest dar = new DataAccessRequest();
    dar.setDatasetIds(List.of(d.getDatasetId()));
    DataAccessRequestData darData = new DataAccessRequestData();
    darData.setDaaIds(Set.of(daaId));
    dar.setData(darData);
    when(daaDAO.findDaaIdsByDatasetIds(List.of(d.getDatasetId()))).thenReturn(Set.of());

    assertThrows(BadRequestException.class, () -> service.hasAcknowledgedRequiredDaas(dar));
  }

  @Test
  void testHasAcknowledgedRequiredDaas_No_Datasets() {
    DataAccessRequest dar = new DataAccessRequest();
    assertThrows(BadRequestException.class, () -> service.hasAcknowledgedRequiredDaas(dar));
  }

  @Test
  void testHasRequiredDaas_No_DAA_Submitted_OR_Acknowledged_Required_Allowed() {
    Dataset d = new Dataset();
    d.setDatasetId(10);
    DataAccessRequest dar = new DataAccessRequest();
    dar.setDatasetIds(List.of(d.getDatasetId()));
    DataAccessRequestData darData = new DataAccessRequestData();
    darData.setDaaIds(Set.of());
    dar.setData(darData);
    when(daaDAO.findDaaIdsByDatasetIds(List.of(d.getDatasetId()))).thenReturn(Set.of());

    assertDoesNotThrow(() -> service.hasAcknowledgedRequiredDaas(dar));
  }

  @Test
  void testInsertDraftDataAccessRequest() {
    User user = new User();
    user.setUserId(1);
    user.setLibraryCard(new LibraryCard());
    DataAccessRequest draft = generateDataAccessRequest();
    doNothing()
        .when(dataAccessRequestDAO)
        .insertDraftDataAccessRequest(any(), any(), any(), any(), any());
    when(dataAccessRequestDAO.findByReferenceId(any())).thenReturn(draft);
    DataAccessRequest dar = service.insertDraftDataAccessRequest(user, draft);
    assertNotNull(dar);
  }

  @Test
  void testInsertDraftDataAccessRequestFailure() {
    assertThrows(
        IllegalArgumentException.class, () -> service.insertDraftDataAccessRequest(null, null));
  }

  private DataAccessRequest generateProgressReport() {
    DataAccessRequest progressReport = generateDataAccessRequest();
    progressReport.getData().setProgressReportSummary("Progress Report Summary");
    progressReport
        .getData()
        .setIntellectualProperties(
            List.of(
                new IntellectualProperty(
                    "Patent",
                    "Description of patent",
                    "Test Assignee",
                    "US12345678",
                    "2024-01-01",
                    "Active",
                    "https://example.com",
                    "contact@example.com",
                    "ip-123",
                    "study-456",
                    List.of("tag1", "tag2"))));
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
    data.setPiCountryOfOperation("United States of America (the)");
    data.setPiName(USER_NAME);
    data.setPiEmail(USER_EMAIL);
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
    when(dataAccessRequestDAO.findAllDraftDataAccessRequests())
        .thenReturn(List.of(new DataAccessRequest()));
    List<DataAccessRequest> drafts = service.findAllDraftDataAccessRequests();
    assertEquals(1, drafts.size());
  }

  @Test
  void testFindAllDraftDataAccessRequestsByUser() {
    when(dataAccessRequestDAO.findAllDraftsByUserId(any()))
        .thenReturn(List.of(new DataAccessRequest()));
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
  void testDeleteDataAccessRequestAdmin() {
    String referenceId = UUID.randomUUID().toString();
    DataAccessRequest dataAccessRequest = new DataAccessRequest();
    dataAccessRequest.setReferenceId(referenceId);
    User adminUser = new User();
    adminUser.setAdminRole();
    Election election = new Election();
    election.setElectionId(1);
    election.setReferenceId(referenceId);
    when(electionDAO.findElectionsByReferenceId(any())).thenReturn(List.of(election));

    assertThrows(
        NotAcceptableException.class, () -> service.deleteDataAccessRequest(dataAccessRequest));
  }

  @Test
  void testDeleteDataAccessRequestResearcherSuccess() {
    String referenceId = UUID.randomUUID().toString();
    DataAccessRequest dataAccessRequest = new DataAccessRequest();
    dataAccessRequest.setReferenceId(referenceId);
    User user = new User();
    user.setResearcherRole();
    when(electionDAO.findElectionsByReferenceId(any())).thenReturn(List.of());
    doNothing().when(matchDAO).deleteMatchesByPurposeId(any());
    doNothing().when(dataAccessRequestDAO).deleteByReferenceId(any());
    doNothing().when(dataAccessRequestDAO).deleteDARDatasetRelationByReferenceId(any());

    assertDoesNotThrow(() -> service.deleteDataAccessRequest(dataAccessRequest));
  }

  @Test
  void testDeleteDataAccessRequestResearcherFailure() {
    String referenceId = UUID.randomUUID().toString();
    DataAccessRequest dataAccessRequest = new DataAccessRequest();
    dataAccessRequest.setReferenceId(referenceId);
    User user = new User();
    user.setResearcherRole();
    Election election = new Election();
    election.setElectionId(1);
    election.setReferenceId(referenceId);
    when(electionDAO.findElectionsByReferenceId(any())).thenReturn(List.of(election));

    assertThrows(
        NotAcceptableException.class, () -> service.deleteDataAccessRequest(dataAccessRequest));
  }

  @Test
  void testDeleteDataAccessRequestSubmittedDarFailure() {
    String referenceId = UUID.randomUUID().toString();
    DataAccessRequest dataAccessRequest = new DataAccessRequest();
    dataAccessRequest.setReferenceId(referenceId);
    dataAccessRequest.setSubmissionDate(FIXED_TIMESTAMP);

    assertThrows(
        BadRequestException.class, () -> service.deleteDataAccessRequest(dataAccessRequest));
  }

  @Test
  void testValidateNoKeyPersonnelDuplicates() {
    DataAccessRequestData data = new DataAccessRequestData();
    data.setPiEmail(PI_EMAIL);
    data.setItDirectorEmail(IT_EMAIL);
    data.setSigningOfficialEmail(SO_EMAIL);
    assertDoesNotThrow(() -> service.validateNoKeyPersonnelDuplicates(data));
  }

  @Test
  void testValidateNoKeyPersonnelDuplicatesBadPIEmail() {
    DataAccessRequestData data = new DataAccessRequestData();
    data.setPiEmail("invalid");
    data.setItDirectorEmail(IT_EMAIL);
    data.setSigningOfficialEmail(SO_EMAIL);
    assertThrows(
        IllegalArgumentException.class, () -> service.validateNoKeyPersonnelDuplicates(data));
  }

  @Test
  void testValidateNoKeyPersonnelDuplicatesBadITDirectorEmail() {
    DataAccessRequestData data = new DataAccessRequestData();
    data.setPiEmail(PI_EMAIL);
    data.setItDirectorEmail("invalid");
    data.setSigningOfficialEmail(SO_EMAIL);
    assertThrows(
        IllegalArgumentException.class, () -> service.validateNoKeyPersonnelDuplicates(data));
  }

  @Test
  void testValidateNoKeyPersonnelDuplicatesBadSO() {
    DataAccessRequestData data = new DataAccessRequestData();
    data.setPiEmail(PI_EMAIL);
    data.setItDirectorEmail(IT_EMAIL);
    data.setSigningOfficialEmail("invalid");
    assertThrows(
        IllegalArgumentException.class, () -> service.validateNoKeyPersonnelDuplicates(data));
  }

  @Test
  void testValidateNoKeyPersonnelDuplicatesItDirector() {
    DataAccessRequestData data = new DataAccessRequestData();
    data.setPiEmail(PI_EMAIL);
    data.setItDirectorEmail(PI_EMAIL);
    data.setSigningOfficialEmail(SO_EMAIL);
    assertThrows(
        IllegalArgumentException.class, () -> service.validateNoKeyPersonnelDuplicates(data));
  }

  @Test
  void testValidateNoKeyPersonnelDuplicatesSO() {
    DataAccessRequestData data = new DataAccessRequestData();
    data.setPiEmail(PI_EMAIL);
    data.setItDirectorEmail(IT_EMAIL);
    data.setSigningOfficialEmail(PI_EMAIL);
    assertThrows(
        IllegalArgumentException.class, () -> service.validateNoKeyPersonnelDuplicates(data));
  }

  @Test
  void testValidatePersonnelInstitutionAndLibraryCardRequirementsThrowsException() {
    String badEmailAddress = "j@example.com";
    User user = new User(1, "email@test.org", "Display Name", FIXED_DATE);
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
    User user = new User(1, "j@example.com", "Display Name", FIXED_DATE);
    Institution goodInstitution = new Institution();
    goodInstitution.setId(1);
    user.setInstitution(goodInstitution);
    user.setLibraryCard(new LibraryCard());
    DataAccessRequestData data = getDataAccessRequestData(goodEmailAddress);

    when(institutionService.findInstitutionForEmail(goodEmailAddress)).thenReturn(goodInstitution);
    when(userDAO.findUserByEmail(goodEmailAddress)).thenReturn(user);
    initService();
    assertDoesNotThrow(
        () -> service.validatePersonnelInstitutionAndLibraryCardRequirements(user, data));
  }

  @Test
  void testValidatePersonnelInstitutionAndLibraryCardRequirementsThrowsForBadPI() {
    String badEmailAddress = "bad@evil.com";
    User user = new User(1, "j@example.com", "Display Name", FIXED_DATE);
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
    User user = new User(1, "j@example.com", "Display Name", FIXED_DATE);
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
    User user = new User(1, "j@example.com", "Display Name", FIXED_DATE);
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
    User user = new User(1, "j@example.com", "Display Name", FIXED_DATE);
    Institution goodInstitution = new Institution();
    goodInstitution.setId(1);
    user.setInstitution(goodInstitution);
    DataAccessRequestData data = new DataAccessRequestData();
    data.setPiEmail(goodEmailAddress);
    data.setSigningOfficialEmail(goodEmailAddress);
    data.setItDirectorEmail(goodEmailAddress);
    Collaborator collaborator1 = createCollaborator(goodEmailAddress);
    Collaborator collaborator2 = createCollaborator(badEmailAddress);
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
  void testValidateGoodCountryOfOperationDoesNotThrow() {
    DataAccessRequestData data = new DataAccessRequestData();
    data.setPiEmail("j@example.com");
    data.setPiCountryOfOperation("Canada");

    Collaborator collaborator1 = createCollaborator("l@example.com");
    data.setInternalCollaborators(List.of(collaborator1));

    Collaborator collaborator2 =
        new Collaborator(null, "m@example.com", null, null, null, null, "Curaçao");
    data.setLabCollaborators(List.of(collaborator2));

    initService();
    assertDoesNotThrow(() -> service.validateCountryOfOperation(data, false));
  }

  @Test
  void testValidatePIAndCollaboratorCountryOfOperationThrowsForBadCountry() {
    DataAccessRequestData data = new DataAccessRequestData();
    data.setPiEmail("j@example.com");
    data.setPiCountryOfOperation("Atlantis");

    Collaborator collaborator1 =
        new Collaborator(null, "l@example.com", null, null, null, null, "Genovia");
    data.setInternalCollaborators(List.of(collaborator1));

    Collaborator collaborator2 =
        new Collaborator(null, "m@example.com", null, null, null, null, "Narnia");
    data.setLabCollaborators(List.of(collaborator2));

    initService();

    BadRequestException exception =
        assertThrows(
            BadRequestException.class, () -> service.validateCountryOfOperation(data, false));

    assertThat(exception.getMessage(), containsString("Principal Investigator"));
    assertThat(exception.getMessage(), containsString("Atlantis"));
    assertThat(exception.getMessage(), containsString(data.getPiEmail()));

    assertThat(exception.getMessage(), containsString("Collaborator"));
    assertThat(exception.getMessage(), containsString("Genovia"));
    assertThat(exception.getMessage(), containsString(collaborator1.email()));

    assertThat(exception.getMessage(), containsString("Lab Staff"));
    assertThat(exception.getMessage(), containsString("Narnia"));
    assertThat(exception.getMessage(), containsString(collaborator2.email()));
  }

  @Test
  void testValidatePIAndCollaboratorCountryOfOperationSkipPI() {
    DataAccessRequestData data = new DataAccessRequestData();
    data.setPiEmail("j@example.com");
    initService();

    assertDoesNotThrow(() -> service.validateCountryOfOperation(data, true));
  }

  @Test
  void testValidatePersonnelInstitutionAndLibraryCardRequirementsThrowsForBadLabStaffMember() {
    String goodEmailAddress = "j@example.com";
    String badEmailAddress = "bad@evil.com";
    User user = new User(1, "j@example.com", "Display Name", FIXED_DATE);
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
    User user = new User(1, "email@test.org", "Display Name", FIXED_DATE);
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
    User user = new User(1, USER_EMAIL, USER_NAME, FIXED_DATE);
    user.setInstitutionId(1);
    Institution institution = new Institution();
    institution.setId(1);
    user.setInstitution(institution);
    user.setLibraryCard(new LibraryCard());
    user.setEraCommonsId("eraCommonsId");
    return user;
  }

  private Dataset approvedDataset(Integer datasetId) {
    Dataset dataset = new Dataset();
    dataset.setDatasetId(datasetId);
    dataset.setDacApproval(true);
    return dataset;
  }

  private Dataset unapprovedDataset(Integer datasetId) {
    Dataset dataset = new Dataset();
    dataset.setDatasetId(datasetId);
    dataset.setDacApproval(false);
    return dataset;
  }

  private void mockApprovedDatasets(List<Integer> datasetIds) {
    when(dataSetDAO.findDatasetsByIdList(datasetIds))
        .thenReturn(datasetIds.stream().map(this::approvedDataset).toList());
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
    verify(emailService).sendMessage(any(ReminderMessage.class), any());
    verify(voteDAO).updateVoteReminderFlag(vote.getVoteId(), true);
  }

  @Test
  void testSendReminderMessageDirectly() throws TemplateException, IOException {
    User user = new User();
    user.setUserId(123);
    user.setDisplayName("John Doe");
    user.setEmail("jd@somewhere");

    Vote vote = new Vote();
    vote.setVoteId(randomInt(0, 100));

    String darCode = "DAR-12345";
    String url = "http://localhost/dar_collection/1";

    initService();
    service.sendReminderMessage(user, vote, darCode, url);
    verify(emailService).sendMessage(any(ReminderMessage.class), eq(user.getUserId()));
  }

  @ParameterizedTest
  @ValueSource(strings = {"RP", "TranslateDUL", "DataSet", "Unknown"})
  void testSendReminderMessage_NonDataAccessElectionType(String electionType)
      throws TemplateException, IOException {
    Election election = new Election();
    election.setElectionId(randomInt(0, 100));
    election.setReferenceId(UUID.randomUUID().toString());
    election.setElectionType(electionType);
    when(electionDAO.findElectionWithFinalVoteById(any())).thenReturn(election);

    Vote vote = new Vote();
    vote.setVoteId(randomInt(0, 100));
    vote.setElectionId(election.getElectionId());
    when(voteDAO.findVoteById(any())).thenReturn(vote);

    initService();
    assertThrows(
        IllegalArgumentException.class, () -> service.sendReminderMessage(vote.getVoteId()));
    verify(emailService, never()).sendMessage(any(ReminderMessage.class), any());
    verify(voteDAO, never()).updateVoteReminderFlag(any(), anyBoolean());
  }

  @Test
  void testSendDarExpirationReminderMessage() throws TemplateException, IOException {
    User user = new User();
    user.setUserId(123);
    user.setDisplayName("John Doe");
    user.setEmail("jd@somewhere");
    String darCode = "DAR-12345";
    Integer otherUserId = 456;
    String referenceId = UUID.randomUUID().toString();

    initService();
    service.sendDarExpirationReminderMessage(user, darCode, otherUserId, referenceId);
    verify(emailService).sendMessage(any(DarExpirationReminderMessage.class), eq(otherUserId));
  }

  @Test
  void testSendDarExpiredMessage() throws TemplateException, IOException {
    User user = new User();
    user.setUserId(123);
    user.setDisplayName("John Doe");
    user.setEmail("jd@somewhere");
    String darCode = "DAR-12345";
    Integer otherUserId = 456;
    String referenceId = UUID.randomUUID().toString();

    initService();
    service.sendDarExpiredMessage(user, darCode, otherUserId, referenceId);
    verify(emailService).sendMessage(any(DarExpiredMessage.class), eq(otherUserId));
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
    when(dataAccessRequestDAO.findAgedDARsByEmailTypeOlderThanInterval(any(), any(), any()))
        .thenReturn(dars);
    initService();
    assertDoesNotThrow(() -> service.sendExpirationNotices());
  }

  @Test
  void sendExpirationNoticesTestMissingEmailForOneUser() {
    ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
    ch.qos.logback.classic.Logger log =
        (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(DataAccessRequestService.class);
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

    when(dataAccessRequestDAO.findAgedDARsByEmailTypeOlderThanInterval(any(), any(), any()))
        .thenReturn(dars);
    initService();

    assertDoesNotThrow(() -> service.sendExpirationNotices());

    assertEquals(2, listAppender.list.size());
  }

  @Test
  void sendExpirationNoticesTestUnderlyingExceptionThrownSendingOneTypeOfMessage() {
    ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
    ch.qos.logback.classic.Logger log =
        (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(DataAccessRequestService.class);
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

    when(dataAccessRequestDAO.findAgedDARsByEmailTypeOlderThanInterval(any(), any(), any()))
        .thenReturn(dars);
    initService();
    assertDoesNotThrow(() -> service.sendExpirationNotices());

    assertEquals(2, listAppender.list.size());
  }

  @Test
  void testValidateCloseoutApproval_NonCloseout() {
    User user = new User();
    user.setUserId(123);
    DataAccessRequest dar = new DataAccessRequest();
    BadRequestException exception =
        assertThrows(BadRequestException.class, () -> service.validateCloseoutApproval(user, dar));
    assertThat(
        exception.getMessage(),
        containsString("Signing officials can only approve closeout progress reports."));
  }

  @Test
  void testValidateCloseoutApproval_AlreadyApproved() {
    User user = new User();
    user.setUserId(123);
    DataAccessRequest dar = new DataAccessRequest();
    dar.setSubmissionDate(FIXED_TIMESTAMP);
    dar.setParentId(1);
    dar.setApprovingSigningOfficialApprovedDate(FIXED_TIMESTAMP);
    dar.setApprovingSigningOfficialUserId(1);
    DataAccessRequestData data = new DataAccessRequestData();
    data.setCloseoutSupplement(new CloseoutSupplement(List.of(""), "", 1));
    dar.setData(data);

    BadRequestException exception =
        assertThrows(BadRequestException.class, () -> service.validateCloseoutApproval(user, dar));
    assertThat(
        exception.getMessage(),
        containsString(
            "This progress report closeout has already been approved by a signing official."));
  }

  @Test
  void testValidateCloseoutApproval_NotTheSelectedSigningOfficial() {
    User user = new User();
    user.setUserId(123);
    DataAccessRequest dar = new DataAccessRequest();
    dar.setSubmissionDate(FIXED_TIMESTAMP);
    dar.setParentId(1);

    DataAccessRequestData data = new DataAccessRequestData();
    data.setCloseoutSupplement(new CloseoutSupplement(List.of(""), "", 1));
    dar.setData(data);

    BadRequestException exception =
        assertThrows(BadRequestException.class, () -> service.validateCloseoutApproval(user, dar));
    assertThat(
        exception.getMessage(),
        containsString(
            "This request can only be approved by the signing official selected in the closeout request."));
  }

  @Test
  void testValidateCloseoutApproval_NotInSameInstitution() {
    User actor = new User();
    actor.setUserId(123);
    actor.setInstitutionId(1);
    User darSubmitter = new User();
    darSubmitter.setUserId(124);
    darSubmitter.setInstitutionId(2);
    DataAccessRequest dar = new DataAccessRequest();
    dar.setUserId(darSubmitter.getUserId());
    dar.setSubmissionDate(FIXED_TIMESTAMP);
    dar.setParentId(1);

    DataAccessRequestData data = new DataAccessRequestData();
    data.setCloseoutSupplement(new CloseoutSupplement(List.of(""), "", actor.getUserId()));
    dar.setData(data);

    when(userService.findUserById(darSubmitter.getUserId())).thenReturn(darSubmitter);
    BadRequestException exception =
        assertThrows(BadRequestException.class, () -> service.validateCloseoutApproval(actor, dar));
    assertThat(
        exception.getMessage(),
        containsString(
            "Signing Officials must be in the same institution as the creator of the closeout request."));
  }

  @Test
  void testValidateCloseoutApproval() {
    CloseoutWithUserAndSigningOfficialApproval closeout =
        new CloseoutWithUserAndSigningOfficialApproval();

    when(userService.findUserById(closeout.submitter.getUserId())).thenReturn(closeout.submitter);
    assertDoesNotThrow(() -> service.validateCloseoutApproval(closeout.actor, closeout.dar));
  }

  @Test
  void approveDataAccessRequest() throws TemplateException, IOException {
    CloseoutWithUserAndSigningOfficialApproval closeout =
        new CloseoutWithUserAndSigningOfficialApproval();
    Dac dac = new Dac();
    User chair = new User(1, "chair@duos.org", "A Chair", FIXED_DATE);
    dac.setChairpersons(List.of(chair));
    when(userService.findUserById(closeout.submitter.getUserId())).thenReturn(closeout.submitter);
    when(dataAccessRequestDAO.findByReferenceId(closeout.dar.referenceId)).thenReturn(closeout.dar);
    when(dacService.findByDatasetId(closeout.dar().getDatasetIds())).thenReturn(Set.of(dac));
    assertDoesNotThrow(
        () ->
            service.approveDataAccessRequestCloseout(
                closeout.actor, closeout.dar.getReferenceId()));
    verify(dacService).findByDatasetId(closeout.dar().getDatasetIds());
    verify(emailService).sendMessage(any(SubmittedCloseoutMessage.class), any());
  }

  private DataAccessRequest getMockedDar(String darCode, String referenceId, User user) {
    DataAccessRequest dar = mock(DataAccessRequest.class);
    when(dar.getReferenceId()).thenReturn(referenceId);
    when(dar.getDarCode()).thenReturn(darCode);
    when(dar.getUserId()).thenReturn(user.getUserId());
    return dar;
  }

  record CloseoutWithUserAndSigningOfficialApproval(
      User actor, User submitter, DataAccessRequest dar) {
    public CloseoutWithUserAndSigningOfficialApproval() {
      this(new User(), new User(), new DataAccessRequest());
      actor.setUserId(123);
      actor.setInstitutionId(1);
      submitter.setUserId(124);
      submitter.setInstitutionId(1);
      dar.setUserId(submitter.getUserId());
      dar.setSubmissionDate(FIXED_TIMESTAMP);
      dar.setParentId(1);
      dar.setDatasetIds(List.of(1));
      dar.setDarCode("DAR-0001");
      dar.setCollectionId(4);
      dar.setReferenceId(UUID.randomUUID().toString());
      DataAccessRequestData data = new DataAccessRequestData();
      data.setCloseoutSupplement(new CloseoutSupplement(List.of(""), "", actor.getUserId()));
      dar.setData(data);
    }
  }

  static class DarDatasetMatcher implements ArgumentMatcher<List<DarDataset>> {

    private final DataAccessRequest progressReport;

    public DarDatasetMatcher(DataAccessRequest progressReport) {
      this.progressReport = progressReport;
    }

    @Override
    public boolean matches(List<DarDataset> darDatasets) {
      for (int i = 0; i < darDatasets.size(); i++) {
        if (!darDatasets.get(i).getReferenceId().equals(progressReport.getReferenceId())
            || !darDatasets.get(i).getDatasetId().equals(progressReport.getDatasetIds().get(i))) {
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
