package org.broadinstitute.consent.http.service;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import freemarker.template.TemplateException;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotAcceptableException;
import jakarta.ws.rs.NotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.validator.routines.EmailValidator;
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
import org.broadinstitute.consent.http.enumeration.EmailType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.exceptions.InvalidEmailAddressException;
import org.broadinstitute.consent.http.exceptions.LibraryCardRequiredException;
import org.broadinstitute.consent.http.exceptions.NIHComplianceRuleException;
import org.broadinstitute.consent.http.exceptions.SubmittedDARCannotBeEditedException;
import org.broadinstitute.consent.http.mail.message.DarExpirationReminderMessage;
import org.broadinstitute.consent.http.mail.message.DarExpiredMessage;
import org.broadinstitute.consent.http.mail.message.ReminderMessage;
import org.broadinstitute.consent.http.mail.message.SubmittedCloseoutMessage;
import org.broadinstitute.consent.http.models.Collaborator;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DarCollection;
import org.broadinstitute.consent.http.models.DarDataset;
import org.broadinstitute.consent.http.models.DarDatasetDaaSnapshot;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetDaaMapping;
import org.broadinstitute.consent.http.models.DatasetDaaSnapshot;
import org.broadinstitute.consent.http.models.DatasetDaaSnapshotDetail;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.rules.DACAutomationRuleType;
import org.broadinstitute.consent.http.service.dao.DataAccessRequestServiceDAO;
import org.broadinstitute.consent.http.util.ConsentLogger;
import org.broadinstitute.consent.http.util.CountryValidator;
import org.glassfish.jersey.server.ContainerRequest;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.JdbiException;
import org.jdbi.v3.core.statement.UnableToExecuteStatementException;

public class DataAccessRequestService implements ConsentLogger {
  public static final String EXPIRE_WARN_INTERVAL = "11 months";
  public static final String EXPIRE_NOTICE_INTERVAL = "1 year";
  public static final String ALL_LISTED_PERSONNEL_MUST_SHARE_THE_SAME_INSTITUTION =
      """
  All listed personnel must share the same institutional affiliation and have a library card.  The following list of \
  roles and members must have email addresses associated with your institution or library cards issued:\s""";
  protected static final Timestamp MINIMUM_SUBMITTED_DATE_FOR_DAR_EXPIRATIONS =
      Timestamp.from(
          Instant.ofEpochSecond(
              LocalDate.of(2024, 9, 30).toEpochSecond(LocalTime.of(0, 0, 0, 0), ZoneOffset.UTC)));
  private static final String MEMBER = "member";
  private static final String MEMBERS = MEMBER + "s: ";
  private static final String INTERNAL_COLLABORATOR = "Internal Collaborator";
  private static final String LAB_STAFF = "Lab staff";
  private final CounterService counterService;
  private final DataAccessRequestDAO dataAccessRequestDAO;
  private final DaaDAO daaDAO;
  private final DarCollectionDAO darCollectionDAO;
  private final ElectionDAO electionDAO;
  private final InstitutionService institutionService;
  private final EmailService emailService;
  private final MatchDAO matchDAO;
  private final VoteDAO voteDAO;
  private final UserDAO userDAO;
  private final UserService userService;
  private final DataAccessRequestServiceDAO dataAccessRequestServiceDAO;
  private final DatasetDAO datasetDAO;
  private final CountryValidator countryValidator;

  private final DacService dacService;
  private final DACAutomationRuleService ruleService;
  private final String serverUrl;

  @Inject
  public DataAccessRequestService(
      Jdbi jdbi,
      DataAccessRequestServiceDAO dataAccessRequestServiceDAO,
      CounterService counterService,
      DacService dacService,
      UserService userService,
      InstitutionService institutionService,
      EmailService emailService,
      DACAutomationRuleService ruleService,
      CountryValidator countryValidator,
      ConsentConfiguration config) {
    this.counterService = counterService;
    this.datasetDAO = jdbi.onDemand(DatasetDAO.class);
    this.dataAccessRequestDAO = jdbi.onDemand(DataAccessRequestDAO.class);
    this.darCollectionDAO = jdbi.onDemand(DarCollectionDAO.class);
    this.electionDAO = jdbi.onDemand(ElectionDAO.class);
    this.matchDAO = jdbi.onDemand(MatchDAO.class);
    this.voteDAO = jdbi.onDemand(VoteDAO.class);
    this.userDAO = jdbi.onDemand(UserDAO.class);
    this.daaDAO = jdbi.onDemand(DaaDAO.class);
    this.dacService = dacService;
    this.dataAccessRequestServiceDAO = dataAccessRequestServiceDAO;
    this.ruleService = ruleService;
    this.userService = userService;
    this.institutionService = institutionService;
    this.emailService = emailService;
    this.serverUrl = config.getServicesConfiguration().getLocalURL();
    this.countryValidator = countryValidator;
  }

  public List<DataAccessRequest> findAllDraftDataAccessRequests() {
    return dataAccessRequestDAO.findAllDraftDataAccessRequests();
  }

  public List<DataAccessRequest> findAllDraftDataAccessRequestsByUser(Integer userId) {
    return dataAccessRequestDAO.findAllDraftsByUserId(userId);
  }

  public void deleteDataAccessRequest(DataAccessRequest dataAccessRequest)
      throws NotAcceptableException {
    String referenceId = dataAccessRequest.getReferenceId();
    if (!dataAccessRequest.getDraft()) {
      throw new BadRequestException("Only draft data access requests can be deleted");
    }
    List<Election> elections = electionDAO.findElectionsByReferenceId(referenceId);
    if (!elections.isEmpty()) {
      String message =
          String.format(
              "Unable to delete DAR: '%s', there are existing elections that reference it.",
              referenceId);
      logWarn(message);
      throw new NotAcceptableException(message);
    }
    matchDAO.deleteRationalesByPurposeIds(List.of(referenceId));
    matchDAO.deleteMatchesByPurposeId(referenceId);
    dataAccessRequestDAO.deleteDARDatasetRelationByReferenceId(referenceId);
    dataAccessRequestDAO.deleteByReferenceId(referenceId);
  }

  public DataAccessRequest findByReferenceId(String referencedId) {
    DataAccessRequest dar = dataAccessRequestDAO.findByReferenceId(referencedId);
    if (Objects.isNull(dar)) {
      throw new NotFoundException("No data access request found for this reference Id");
    }
    return dar;
  }

  // NOTE: rewrite method into new service DAO method on another ticket
  public DataAccessRequest insertDraftDataAccessRequest(User user, DataAccessRequest dar) {
    if (Objects.isNull(user)
        || Objects.isNull(dar)
        || Objects.isNull(dar.getReferenceId())
        || Objects.isNull(dar.getData())) {
      throw new IllegalArgumentException("User and DataAccessRequest are required");
    }

    if (user.getLibraryCard() == null) {
      throw new LibraryCardRequiredException();
    }

    Date now = new Date();
    dataAccessRequestDAO.insertDraftDataAccessRequest(
        dar.getReferenceId(), user.getUserId(), now, now, dar.getData());
    syncDataAccessRequestDatasets(dar.getDatasetIds(), dar.getReferenceId());

    return findByReferenceId(dar.getReferenceId());
  }

  /**
   * First delete any rows with the current reference id. This will allow us to keep (referenceId,
   * dataset_id) unique Takes in a list of datasetIds and a referenceId and adds them to the
   * dar_dataset collection
   *
   * @param datasetIds List of Integers that represent the datasetIds
   * @param referenceId ReferenceId of the corresponding DAR
   */
  private void syncDataAccessRequestDatasets(List<Integer> datasetIds, String referenceId) {
    syncDataAccessRequestDatasets(datasetIds, referenceId, dataAccessRequestDAO);
  }

  private void syncDataAccessRequestDatasets(
      List<Integer> datasetIds, String referenceId, DataAccessRequestDAO targetDarDAO) {
    List<DarDataset> darDatasets =
        datasetIds.stream().map(datasetId -> new DarDataset(referenceId, datasetId)).toList();
    targetDarDAO.deleteDARDatasetRelationByReferenceId(referenceId);

    if (!darDatasets.isEmpty()) {
      targetDarDAO.insertAllDarDatasets(darDatasets);
    }
  }

  /**
   * @param user User
   * @return List<DataAccessRequest>
   */
  public List<DataAccessRequest> getDataAccessRequestsByUserRole(User user) {
    List<DataAccessRequest> dars = dataAccessRequestDAO.findAllDataAccessRequests();
    return dacService.filterDataAccessRequestsByDac(dars, user);
  }

  /**
   * Generate a DataAccessRequest from the provided DAR. The provided DAR may or may not exist in
   * draft form, so it covers both cases of converting an existing draft to submitted and creating a
   * brand-new DAR from scratch.
   *
   * @param user The creating User
   * @param dataAccessRequest DataAccessRequest with populated DAR data
   * @return The created DAR.
   */
  public DataAccessRequest createDataAccessRequest(
      User user, DataAccessRequest dataAccessRequest, ContainerRequest request) {
    validateDar(user, dataAccessRequest);

    Date now = new Date();
    DataAccessRequestData darData = dataAccessRequest.getData();

    DataAccessRequest existingDar =
        dataAccessRequestDAO.findByReferenceId(dataAccessRequest.getReferenceId());
    if (existingDar != null && !existingDar.getDraft()) {
      throw new SubmittedDARCannotBeEditedException();
    }
    List<Integer> datasetIds = dataAccessRequest.getDatasetIds();
    boolean requiresSOApproval = requiresSOApproval(user, datasetIds);
    String referenceId =
        dataAccessRequestServiceDAO.inTransaction(
            transactionDAOs -> {
              DataAccessRequestDAO transactionalDarDAO = transactionDAOs.dataAccessRequestDAO();
              DaaDAO transactionalDaaDAO = transactionDAOs.daaDAO();
              Integer collectionId =
                  Objects.nonNull(existingDar) && Objects.nonNull(existingDar.getCollectionId())
                      ? existingDar.getCollectionId()
                      : transactionDAOs
                          .darCollectionDAO()
                          .insertDarCollection(
                              "DAR-"
                                  + counterService.getNextDarSequence(transactionDAOs.counterDAO()),
                              user.getUserId(),
                              now);
              String transactionReferenceId;
              Integer darId;
              if (Objects.nonNull(existingDar)) {
                transactionReferenceId = dataAccessRequest.getReferenceId();
                darId = existingDar.getId();
                transactionalDarDAO.updateDraftToSubmittedForCollection(
                    collectionId, transactionReferenceId);
                transactionalDarDAO.updateDataByReferenceId(
                    transactionReferenceId,
                    user.getUserId(),
                    now,
                    now,
                    darData,
                    user.getEraCommonsId());
                transactionalDaaDAO.deleteDarDAARelationship(darId);
              } else {
                transactionReferenceId = UUID.randomUUID().toString();
                darId =
                    transactionalDarDAO.insertDataAccessRequest(
                        collectionId,
                        transactionReferenceId,
                        user.getUserId(),
                        now,
                        now,
                        now,
                        darData,
                        user.getEraCommonsId());
              }
              transactionalDaaDAO.insertDarDAARelationship(
                  darId, dataAccessRequest.data.getDaaIds());
              syncDataAccessRequestDatasets(
                  datasetIds, transactionReferenceId, transactionalDarDAO);
              captureDatasetDaaSnapshots(
                  darId, datasetIds, new Timestamp(now.getTime()), transactionalDaaDAO);
              if (requiresSOApproval) {
                transactionalDarDAO.updateRequiresSOApproval(true, transactionReferenceId);
              }
              return transactionReferenceId;
            });
    if (!requiresSOApproval) {
      ruleService.triggerDACRuleSettings(user, datasetIds, referenceId, request);
    }
    return findByReferenceId(referenceId);
  }

  /**
   * Create a progress report for the given DataAccessRequest. The parent DAR is just passed in for
   * validation purposes.
   *
   * @param user The User
   * @param progressReport The DataAccessRequest
   * @param parentDar The parent DataAccessRequest
   * @return The created progress report.
   */
  public DataAccessRequest createProgressReport(
      User user,
      DataAccessRequest progressReport,
      DataAccessRequest parentDar,
      ContainerRequest request) {
    validateProgressReport(user, progressReport, parentDar);

    String referenceId = progressReport.getReferenceId();
    List<Integer> progressReportDatasetIds = progressReport.getDatasetIds();
    Set<Integer> darDatasetIds =
        dataAccessRequestDAO.findDatasetApprovalsByDar(parentDar.getReferenceId());

    if (!darDatasetIds.containsAll(progressReportDatasetIds)) {
      throw new BadRequestException(
          "Progress report can only be created for approved datasets in the parent DAR");
    }
    boolean userIsPreAuthedForDaas =
        isUserPreAuthorizedForAllDaas(user, progressReport.getDatasetIds());
    try {
      dataAccessRequestServiceDAO.inTransaction(
          transactionDAOs -> {
            DataAccessRequestDAO transactionalDarDAO = transactionDAOs.dataAccessRequestDAO();
            DaaDAO transactionalDaaDAO = transactionDAOs.daaDAO();
            Integer progressReportId =
                transactionalDarDAO.insertProgressReport(
                    progressReport.getParentId(),
                    progressReport.getCollectionId(),
                    referenceId,
                    user.getUserId(),
                    progressReport.getData(),
                    user.getEraCommonsId());
            if (!progressReport.getIsCloseoutProgressReport()) {
              transactionalDaaDAO.insertDarDAARelationship(
                  progressReportId, progressReport.getData().getDaaIds());
            }
            if (!progressReport.getIsCloseoutProgressReport() && !userIsPreAuthedForDaas) {
              transactionalDarDAO.updateRequiresSOApproval(true, referenceId);
            }
            syncDataAccessRequestDatasets(
                progressReportDatasetIds, referenceId, transactionalDarDAO);
            if (!progressReport.getIsCloseoutProgressReport()) {
              captureDatasetDaaSnapshots(
                  progressReportId,
                  progressReportDatasetIds,
                  Timestamp.from(Instant.now()),
                  transactionalDaaDAO);
            }
            return progressReportId;
          });
    } catch (JdbiException _) {
      throw new BadRequestException(
          "Unable to create progress report for Data Access Request " + parentDar.getReferenceId());
    }
    if (progressReport.getIsCloseoutProgressReport()) {
      try {
        User signingOfficialUser =
            userService.findUserById(
                progressReport.getData().getCloseoutSupplement().signingOfficialId());
        sendSubmittedCloseoutMessage(
            signingOfficialUser,
            parentDar.getDarCode(),
            referenceId,
            serverUrl + "dar_application_review/%d".formatted(parentDar.getCollectionId()));
      } catch (Exception e) {
        // Persistence has already committed, so a notification failure must not make the caller
        // treat creation as failed and compensate by deleting the progress report documents.
        logException("Unable to send submitted closeout message for " + referenceId, e);
      }
    }

    if (!progressReport.getIsCloseoutProgressReport()
        && !progressReport.getHasDMI()
        && userIsPreAuthedForDaas) {
      ruleService.triggerDACRuleSettings(user, progressReportDatasetIds, referenceId, request);
    }

    return findByReferenceId(referenceId);
  }

  public Map<Integer, DatasetDaaSnapshot> findDatasetDaaSnapshotsByReferenceId(String referenceId) {
    findByReferenceId(referenceId);
    List<DatasetDaaSnapshotDetail> snapshots =
        Objects.requireNonNullElse(
            daaDAO.findDatasetDaaSnapshotsByReferenceId(referenceId), List.of());
    if (snapshots.isEmpty()) {
      throw new NotFoundException(
          "No dataset to DAA snapshot found for Data Access Request reference Id");
    }
    Map<Integer, DatasetDaaSnapshot> result = new LinkedHashMap<>();
    for (DatasetDaaSnapshotDetail snapshot : snapshots) {
      result.put(
          snapshot.datasetId(), new DatasetDaaSnapshot(snapshot.daaId(), snapshot.capturedAt()));
    }
    return result;
  }

  public void approveDataAccessRequestCloseout(User signingOfficial, String referenceId) {
    DataAccessRequest dar = dataAccessRequestDAO.findByReferenceId(referenceId);
    validateCloseoutApproval(signingOfficial, dar);
    dataAccessRequestDAO.updateDarApprovalSO(signingOfficial.getUserId(), referenceId);
    Set<User> chairs = new HashSet<>();
    Set<Dac> dacs = dacService.findByDatasetId(dar.getDatasetIds());
    dacs.forEach(dac -> chairs.addAll(dac.getChairpersons()));
    chairs.forEach(
        chairperson -> {
          try {
            sendSubmittedCloseoutMessage(
                chairperson,
                dar.getDarCode(),
                dar.getReferenceId(),
                serverUrl + "dar_application_review/%d".formatted(dar.getCollectionId()));
          } catch (Exception e) {
            logWarn("Unable to send close out message for Data Access Request " + referenceId, e);
          }
        });
  }

  @VisibleForTesting
  protected void validateCloseoutApproval(
      User signingOfficial, DataAccessRequest dataAccessRequest) {
    // Note: we will allow a signing official to approve their own closeout.

    if (!dataAccessRequest.getIsCloseoutProgressReport()) {
      throw new BadRequestException(
          "Signing officials can only approve closeout progress reports.");
    }

    if (dataAccessRequest.getApprovingSigningOfficialUserId() != null) {
      throw new BadRequestException(
          "This progress report closeout has already been approved by a signing official.");
    }

    if (!signingOfficial
        .getUserId()
        .equals(dataAccessRequest.getData().getCloseoutSupplement().signingOfficialId())) {
      throw new BadRequestException(
          "This request can only be approved by the signing official selected in the closeout request.");
    }

    try {
      User submitter = userService.findUserById(dataAccessRequest.getUserId());
      if (!submitter.getInstitutionId().equals(signingOfficial.getInstitutionId())) {
        throw new BadRequestException(
            "Signing Officials must be in the same institution as the creator of the closeout request.");
      }

    } catch (NotFoundException _) {
      // log the state.  we'll allow the SO to process a closeout even if the  user can't be found.
      logWarn(
          String.format(
              "Signing Official approving closeout %s for non-existent user %d",
              dataAccessRequest.getReferenceId(), dataAccessRequest.getUserId()));
    }
  }

  public void validateProgressReport(
      User user, DataAccessRequest progressReport, DataAccessRequest parentDar) {
    validateCommonDarAndProgressReportElements(user, progressReport);
    validateInternalCollaborators(user, progressReport);
    validateCountryOfOperation(progressReport.data, true);

    if (parentDar.getDraft()) {
      throw new BadRequestException(
          "Cannot create a progress report for a draft Data Access Request");
    }
    if (progressReport.getDatasetIds().isEmpty()) {
      throw new BadRequestException("At least one dataset is required");
    }
    if (!Set.copyOf(parentDar.getDatasetIds()).containsAll(progressReport.getDatasetIds())) {
      throw new BadRequestException(
          "Progress report can only be created for datasets in the parent DAR");
    }
    if (progressReport.getData().getProgressReportSummary() == null
        || progressReport.getData().getProgressReportSummary().isEmpty()) {
      throw new BadRequestException("Progress report summary is required");
    }

    if (progressReport.getIsCloseoutProgressReport()) {
      Integer providedSigningOfficial =
          progressReport.getData().getCloseoutSupplement().signingOfficialId();
      try {
        User selectedSigningOfficial = userService.findUserById(providedSigningOfficial);
        if (!selectedSigningOfficial.getInstitutionId().equals(user.getInstitutionId())) {
          throw new BadRequestException(
              "The signing official selected in the closeout is not in the same institution as the submitter.");
        }
        if (!selectedSigningOfficial.hasUserRole(UserRoles.SIGNINGOFFICIAL)) {
          throw new BadRequestException("The selected signing official is not a signing official");
        }
      } catch (NotFoundException _) {
        throw new BadRequestException(
            "The selected signing official in the closeout was not found.");
      }
    }
    if (!progressReport.getIsCloseoutProgressReport() && !progressReport.getHasDMI()) {
      hasAcknowledgedRequiredDaas(progressReport);
    }
  }

  @VisibleForTesting
  protected void validateCommonDarAndProgressReportElements(User user, DataAccessRequest dar) {
    if (Objects.isNull(user)
        || Objects.isNull(dar)
        || Objects.isNull(dar.getReferenceId())
        || Objects.isNull(dar.getData())) {
      throw new IllegalArgumentException("User and DataAccessRequest are required");
    }
    if (user.getLibraryCard() == null) {
      throw new NIHComplianceRuleException();
    }
    validateRequestDatasetsAreApproved(dar);
    userService.validateActiveERACredentials(user);
  }

  @VisibleForTesting
  protected void hasAcknowledgedRequiredDaas(DataAccessRequest dar) {
    if (dar.getDatasetIds().isEmpty()) {
      throw new BadRequestException("At least one dataset is required");
    }

    Set<Integer> requiredDaas = daaDAO.findDaaIdsByDatasetIds(dar.getDatasetIds());

    if (!(requiredDaas.containsAll(dar.getData().getDaaIds())
        && requiredDaas.size() == dar.getData().getDaaIds().size())) {
      throw new BadRequestException("All of the DAAs required were not acknowledged.");
    }
  }

  private boolean isUserPreAuthorizedForAllDaas(User user, List<Integer> datasetIds) {
    Set<Integer> datasetDaas = new HashSet<>(daaDAO.findDaaIdsByDatasetIds(datasetIds));

    Set<Integer> userDaas = new HashSet<>(user.getLibraryCard().getDaaIds());

    return userDaas.containsAll(datasetDaas);
  }

  @VisibleForTesting
  protected void validateRequestDatasetsAreApproved(DataAccessRequest dar) {
    List<Integer> datasetIds = dar.getDatasetIds();
    Set<Integer> approvedDatasetIds =
        datasetDAO.findDatasetsByIdList(datasetIds).stream()
            .filter(dataset -> Boolean.TRUE.equals(dataset.getDacApproval()))
            .map(Dataset::getDatasetId)
            .collect(java.util.stream.Collectors.toSet());
    if (!approvedDatasetIds.containsAll(datasetIds)) {
      throw new BadRequestException(
          "All datasets in the DAR must be approved by their respective DAC to create a data access request or progress report.");
    }
  }

  public void validateDar(User user, DataAccessRequest dar) {
    validateCommonDarAndProgressReportElements(user, dar);

    if (!Objects.equals(user.getEmail(), dar.getData().getPiEmail())
        || !Objects.equals(user.getDisplayName(), dar.getData().getPiName())) {
      throw new BadRequestException(
          "The PI in the DAR must have the same name and email as the user submitting the DAR.");
    }

    validateNoKeyPersonnelDuplicates(dar.getData());
    validatePersonnelInstitutionAndLibraryCardRequirements(user, dar.getData());
    validateCountryOfOperation(dar.getData(), false);
    hasAcknowledgedRequiredDaas(dar);
  }

  protected void validateCountryOfOperation(DataAccessRequestData darData, boolean skipPI) {
    List<String> errorSummary = new ArrayList<>();
    // We will have progress reports that don't have country of operation set for the PI.
    if (!skipPI && !countryValidator.isInCountryList(darData.getPiCountryOfOperation())) {
      errorSummary.add(
          "Principal Investigator %s Country of Operation (%s) is not allowed"
              .formatted(darData.getPiEmail(), darData.getPiCountryOfOperation()));
    }

    List<Collaborator> collaborators = darData.getLabAndInternalCollaborators();
    collaborators.forEach(
        collaborator -> {
          if (!countryValidator.isInCountryList(collaborator.countryOfOperation())) {
            errorSummary.add(
                "Collaborator or Lab Staff Member %s Country of Operation (%s) is not allowed"
                    .formatted(collaborator.email(), collaborator.countryOfOperation()));
          }
        });

    if (!errorSummary.isEmpty()) {
      throw new BadRequestException(String.join(", ", errorSummary));
    }
  }

  @VisibleForTesting
  protected void validateInternalCollaborators(User user, DataAccessRequest progressReport) {
    List<String> errorSummary = getCollaboratorAndLibraryCardErrors(user, progressReport.getData());

    if (!errorSummary.isEmpty()) {
      throw new BadRequestException(
          ALL_LISTED_PERSONNEL_MUST_SHARE_THE_SAME_INSTITUTION + String.join(", ", errorSummary));
    }
  }

  private List<String> getCollaboratorAndLibraryCardErrors(
      User user, DataAccessRequestData darData) {
    List<String> errorSummary = new ArrayList<>();
    getErrorSummary(
        darData.getInternalCollaborators().stream().map(Collaborator::email).toList(),
        user.getInstitution(),
        INTERNAL_COLLABORATOR + " " + MEMBER + ": ",
        INTERNAL_COLLABORATOR + "  " + MEMBERS,
        errorSummary);
    getErrorSummary(
        darData.getLabCollaborators().stream().map(Collaborator::email).toList(),
        user.getInstitution(),
        LAB_STAFF + " " + MEMBER + ": ",
        LAB_STAFF + " " + MEMBERS,
        errorSummary);
    return errorSummary;
  }

  /**
   * Update an existing DataAccessRequest. Replaces DataAccessRequestData.
   *
   * @param user The User
   * @param dar The DataAccessRequest
   * @return The updated DataAccessRequest
   */
  public DataAccessRequest updateByReferenceId(User user, DataAccessRequest dar) {
    if (!dar.getDraft()) {
      throw new SubmittedDARCannotBeEditedException();
    }
    try {
      return dataAccessRequestServiceDAO.updateByReferenceId(user, dar);
    } catch (SQLException e) {
      // If I simply rethrow the error then I'll have to redefine any method that
      // calls this function to "throw SQLException"
      // Instead I'm going to throw an UnableToExecuteStatementException
      // Response class will catch it, log it, and throw a 500 through the
      // "unableToExecuteExceptionHandler"
      // on the Resource class, just like it would with a SQLException
      throw new UnableToExecuteStatementException(e.getMessage());
    }
  }

  /**
   * Validates that PI email is not duplicated with SO or IT Director emails
   *
   * @param darData The data access request data to validate
   * @throws IllegalArgumentException if duplicate emails are found
   */
  public void validateNoKeyPersonnelDuplicates(DataAccessRequestData darData) {
    EmailValidator emailValidator = EmailValidator.getInstance();

    String piEmail = darData.getPiEmail();
    String soEmail = darData.getSigningOfficialEmail();
    String itEmail = darData.getItDirectorEmail();

    if (!emailValidator.isValid(piEmail)
        || !emailValidator.isValid(soEmail)
        || !emailValidator.isValid(itEmail)) {
      throw new IllegalArgumentException(
          "Principal Investigator, Signing Official, and IT Director emails must be valid");
    }

    if (piEmail.equalsIgnoreCase(soEmail)) {
      throw new IllegalArgumentException(
          "Principal Investigator email cannot be the same as Signing Official email");
    }

    if (piEmail.equalsIgnoreCase(itEmail)) {
      throw new IllegalArgumentException(
          "Principal Investigator email cannot be the same as IT Director email");
    }
  }

  @VisibleForTesting
  protected void validatePersonnelInstitutionAndLibraryCardRequirements(
      User user, DataAccessRequestData darData) {
    Institution submitterInstitution = user.getInstitution();
    String piEmail = darData.getPiEmail();
    String soEmail = darData.getSigningOfficialEmail();
    String itEmail = darData.getItDirectorEmail();

    List<String> invalidMembers = new ArrayList<>();

    verifyInstitution(submitterInstitution, piEmail, "Principal Investigator", invalidMembers);
    verifyInstitution(submitterInstitution, soEmail, "Signing Official", invalidMembers);
    verifyInstitution(submitterInstitution, itEmail, "IT Director", invalidMembers);
    invalidMembers.addAll(getCollaboratorAndLibraryCardErrors(user, darData));

    if (!invalidMembers.isEmpty()) {
      throw new IllegalArgumentException(
          ALL_LISTED_PERSONNEL_MUST_SHARE_THE_SAME_INSTITUTION + String.join(", ", invalidMembers));
    }
  }

  private void verifyInstitution(
      Institution submitterInstitution, String email, String role, List<String> invalidMembers) {
    if (emailDoesNotMatchInstitution(submitterInstitution, email)) {
      invalidMembers.add(role + ": " + email);
    }
  }

  private List<String> findCollaboratorsWithoutLibraryCards(List<String> usersToCheck) {
    List<String> usersWithoutLibraryCards = new ArrayList<>();
    usersToCheck.forEach(
        email -> {
          User collabUser = userDAO.findUserByEmail(email);
          if (collabUser == null || collabUser.getLibraryCard() == null) {
            usersWithoutLibraryCards.add(email);
          }
        });
    return usersWithoutLibraryCards;
  }

  private void getErrorSummary(
      List<String> emails,
      Institution institution,
      String categorySingular,
      String categoryPlural,
      List<String> invalidMembers) {
    List<String> institutionErrors = findEmailAddressesNotInInstitution(emails, institution);
    List<String> libraryCardErrors = findCollaboratorsWithoutLibraryCards(emails);

    if (!institutionErrors.isEmpty()) {
      String missingInstitution = " (missing institution) ";
      invalidMembers.add(
          buildSingleErrorFromErrorList(
              institutionErrors,
              categorySingular + missingInstitution,
              categoryPlural + missingInstitution));
    }

    if (!libraryCardErrors.isEmpty()) {
      String missingLibraryCard = " (missing library card) ";
      invalidMembers.add(
          buildSingleErrorFromErrorList(
              libraryCardErrors,
              categorySingular + missingLibraryCard,
              categoryPlural + missingLibraryCard));
    }
  }

  private List<String> findEmailAddressesNotInInstitution(
      List<String> emailAddresses, Institution institution) {
    ArrayList<String> errors = new ArrayList<>();
    emailAddresses.forEach(
        collaborator -> {
          if (emailDoesNotMatchInstitution(institution, collaborator)) {
            errors.add(collaborator);
          }
        });
    return errors;
  }

  private String buildSingleErrorFromErrorList(
      List<String> errors, String categorySingular, String categoryPlural) {
    StringBuilder msg = new StringBuilder();
    if (errors.size() == 1) {
      msg.append(categorySingular);
    } else if (errors.size() > 1) {
      msg.append(categoryPlural);
    }
    msg.append(String.join(", ", errors));
    return msg.toString();
  }

  private boolean emailDoesNotMatchInstitution(Institution institution, String email) {
    Institution foundInstitution = institutionService.findInstitutionForEmail(email);
    if (foundInstitution == null || institution == null) {
      return true;
    }
    return !institution.equals(foundInstitution);
  }

  public Collection<DataAccessRequest> getApprovedDARsForDataset(Dataset dataset) {
    return dataAccessRequestDAO.findApprovedDARsByDatasetId(dataset.getDatasetId());
  }

  public void sendExpirationNotices() {
    sendDARExpirationReminderNotices();
    sendDARExpirationNotices();
  }

  private void sendDARExpirationNotices() {
    EmailType emailType = EmailType.DAR_EXPIRED;
    sendDARMessageToList(emailType, EXPIRE_NOTICE_INTERVAL);
  }

  private void sendDARExpirationReminderNotices() {
    EmailType emailType = EmailType.DAR_EXPIRATION_REMINDER;
    sendDARMessageToList(emailType, EXPIRE_WARN_INTERVAL);
  }

  private void sendDARMessageToList(EmailType type, String interval) {
    List<DataAccessRequest> expiredDars =
        dataAccessRequestDAO.findAgedDARsByEmailTypeOlderThanInterval(
            type.getTypeInt(), interval, MINIMUM_SUBMITTED_DATE_FOR_DAR_EXPIRATIONS);
    expiredDars.forEach(
        expiredDar -> {
          try {
            String referenceId = expiredDar.getReferenceId();
            User user = userDAO.findUserById(expiredDar.getUserId());
            String darCode = expiredDar.getDarCode();
            String userName = user.getDisplayName();
            if (user.getEmail() == null) {
              throw new InvalidEmailAddressException(
                  String.format(
                      "Email address for user %d (%s) not found for expiring warning.  DAR reference id: %s",
                      expiredDar.getUserId(), userName, referenceId));
            }
            switch (type) {
              case DAR_EXPIRATION_REMINDER:
                sendDarExpirationReminderMessage(user, darCode, user.getUserId(), referenceId);
                break;
              case DAR_EXPIRED:
                sendDarExpiredMessage(user, darCode, user.getUserId(), referenceId);
                break;
              default:
                break;
            }
          } catch (Exception e) {
            logException(e);
          }
        });
  }

  @VisibleForTesting
  protected void sendSubmittedCloseoutMessage(
      User toUser, String darId, String referenceId, String closeoutUrl)
      throws TemplateException, IOException {
    emailService.sendMessage(
        new SubmittedCloseoutMessage(toUser, darId, referenceId, closeoutUrl), toUser.getUserId());
  }

  @VisibleForTesting
  protected void sendDarExpirationReminderMessage(
      User user, String darCode, Integer userId, String referenceId)
      throws TemplateException, IOException {
    emailService.sendMessage(new DarExpirationReminderMessage(user, darCode, referenceId), userId);
  }

  @VisibleForTesting
  protected void sendDarExpiredMessage(
      User researcher, String darCode, Integer userId, String referenceId)
      throws TemplateException, IOException {
    emailService.sendMessage(new DarExpiredMessage(researcher, darCode, referenceId), userId);
  }

  @VisibleForTesting
  protected void sendReminderMessage(User user, Vote vote, String darCode, String url)
      throws TemplateException, IOException {
    emailService.sendMessage(new ReminderMessage(user, vote, darCode, url), user.getUserId());
  }

  public void sendReminderMessage(Integer voteId) throws IOException, TemplateException {
    Vote vote = voteDAO.findVoteById(voteId);
    Election election = electionDAO.findElectionWithFinalVoteById(vote.getElectionId());
    if (!ElectionType.DATA_ACCESS.getValue().equals(election.getElectionType())) {
      throw new IllegalArgumentException(
          "ElectionType must be '%s', but found '%s'"
              .formatted(ElectionType.DATA_ACCESS.getValue(), election.getElectionType()));
    }
    DarCollection collection =
        darCollectionDAO.findDARCollectionByReferenceId(election.getReferenceId());
    User user = findUserById(vote.getUserId());
    String voteUrl = serverUrl + "dar_collection/%d".formatted(collection.getDarCollectionId());
    sendReminderMessage(user, vote, collection.getDarCode(), voteUrl);
    voteDAO.updateVoteReminderFlag(voteId, true);
  }

  private User findUserById(Integer id) throws IllegalArgumentException {
    User user = userDAO.findUserById(id);
    if (user == null) {
      throw new NotFoundException("Could not find dacUser for specified id : " + id);
    }
    return user;
  }

  public List<Election> findOpenElectionsByReferenceId(String referenceId) {
    return electionDAO.findOpenElectionsByReferenceIds(List.of(referenceId));
  }

  private boolean requiresSOApproval(User user, List<Integer> datasetIds) {
    return !datasetDAO
            .filterDatasetIdsByAutomationRuleType(
                datasetIds, DACAutomationRuleType.REQUIRE_SO_DAR_APPROVAL.name())
            .isEmpty()
        || !isUserPreAuthorizedForAllDaas(user, datasetIds);
  }

  private void captureDatasetDaaSnapshots(
      Integer darId, List<Integer> datasetIds, Timestamp capturedAt, DaaDAO targetDaaDAO) {
    List<DatasetDaaMapping> datasetDaaMappings =
        Objects.requireNonNullElse(
            targetDaaDAO.findCurrentDaaMappingsByDatasetIds(datasetIds), List.of());
    if (datasetDaaMappings.isEmpty()) {
      return;
    }
    List<DarDatasetDaaSnapshot> snapshots =
        datasetDaaMappings.stream()
            .map(
                mapping ->
                    new DarDatasetDaaSnapshot(
                        darId, mapping.datasetId(), mapping.daaId(), capturedAt))
            .toList();
    targetDaaDAO.insertDarDatasetDaaSnapshots(snapshots);
  }
}
