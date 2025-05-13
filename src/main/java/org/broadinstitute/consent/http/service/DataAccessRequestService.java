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
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.validator.routines.EmailValidator;
import org.broadinstitute.consent.http.configurations.ConsentConfiguration;
import org.broadinstitute.consent.http.db.DAOContainer;
import org.broadinstitute.consent.http.db.DarCollectionDAO;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.MatchDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.enumeration.EmailType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.exceptions.LibraryCardRequiredException;
import org.broadinstitute.consent.http.exceptions.NIHComplianceRuleException;
import org.broadinstitute.consent.http.exceptions.SubmittedDARCannotBeEditedException;
import org.broadinstitute.consent.http.mail.message.DarExpirationReminderMessage;
import org.broadinstitute.consent.http.mail.message.DarExpiredMessage;
import org.broadinstitute.consent.http.mail.message.ReminderMessage;
import org.broadinstitute.consent.http.models.Collaborator;
import org.broadinstitute.consent.http.models.DarCollection;
import org.broadinstitute.consent.http.models.DarDataset;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.LibraryCard;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.service.dao.DataAccessRequestServiceDAO;
import org.broadinstitute.consent.http.util.ConsentLogger;
import org.jdbi.v3.core.statement.UnableToExecuteStatementException;

public class DataAccessRequestService implements ConsentLogger {
  public static final String EXPIRE_WARN_INTERVAL = "11 months";
  public static final String EXPIRE_NOTICE_INTERVAL = "1 year";
  public static final Timestamp MINIMUM_SUBMITTED_DATE_FOR_DAR_EXPIRATIONS = Timestamp.from(
      Instant.ofEpochSecond(
          LocalDate.of(2024, 9, 30).toEpochSecond(LocalTime.of(0, 0, 0, 0), ZoneOffset.UTC)));
  private final CounterService counterService;
  private final DataAccessRequestDAO dataAccessRequestDAO;
  private final DarCollectionDAO darCollectionDAO;
  private final ElectionDAO electionDAO;
  private final EmailService emailService;
  private final MatchDAO matchDAO;
  private final VoteDAO voteDAO;
  private final UserDAO userDAO;
  private final UserService userService;
  private final DataAccessRequestServiceDAO dataAccessRequestServiceDAO;

  private final DacService dacService;
  private final String serverUrl;

  @Inject
  public DataAccessRequestService(CounterService counterService, DAOContainer container,
      DacService dacService, DataAccessRequestServiceDAO dataAccessRequestServiceDAO, UserService userService, EmailService emailService, ConsentConfiguration config) {
    this.counterService = counterService;
    this.dataAccessRequestDAO = container.getDataAccessRequestDAO();
    this.darCollectionDAO = container.getDarCollectionDAO();
    this.electionDAO = container.getElectionDAO();
    this.matchDAO = container.getMatchDAO();
    this.voteDAO = container.getVoteDAO();
    this.userDAO = container.getUserDAO();
    this.dacService = dacService;
    this.dataAccessRequestServiceDAO = dataAccessRequestServiceDAO;
    this.userService = userService;
    this.emailService = emailService;
    this.serverUrl = config.getServicesConfiguration().getLocalURL();
  }

  public List<DataAccessRequest> findAllDraftDataAccessRequests() {
    return dataAccessRequestDAO.findAllDraftDataAccessRequests();
  }

  public List<DataAccessRequest> findAllDraftDataAccessRequestsByUser(Integer userId) {
    return dataAccessRequestDAO.findAllDraftsByUserId(userId);
  }

  public void deleteByReferenceId(User user, String referenceId) throws NotAcceptableException {
    List<Election> elections = electionDAO.findElectionsByReferenceId(referenceId);
    if (!elections.isEmpty()) {
      // If the user is an admin, delete all votes and elections
      if (user.hasUserRole(UserRoles.ADMIN)) {
        voteDAO.deleteVotesByReferenceId(referenceId);
        List<Integer> electionIds = elections.stream().map(Election::getElectionId).toList();
        electionDAO.deleteElectionsByIds(electionIds);
      } else {
        String message = String.format(
            "Unable to delete DAR: '%s', there are existing elections that reference it.",
            referenceId);
        logWarn(message);
        throw new NotAcceptableException(message);
      }
    }
    matchDAO.deleteRationalesByPurposeIds(List.of(referenceId));
    matchDAO.deleteMatchesByPurposeId(referenceId);
    dataAccessRequestDAO.deleteDARDatasetRelationByReferenceId(referenceId);
    dataAccessRequestDAO.deleteByReferenceId(referenceId);
  }

  public DataAccessRequest findByReferenceId(String referencedId) {
    DataAccessRequest dar = dataAccessRequestDAO.findByReferenceId(referencedId);
    if (Objects.isNull(dar)) {
      throw new NotFoundException("There does not exist a DAR with the given reference Id");
    }
    return dar;
  }

  //NOTE: rewrite method into new servicedao method on another ticket
  public DataAccessRequest insertDraftDataAccessRequest(User user, DataAccessRequest dar) {
    if (Objects.isNull(user) || Objects.isNull(dar) || Objects.isNull(
        dar.getReferenceId()) || Objects.isNull(dar.getData())) {
      throw new IllegalArgumentException("User and DataAccessRequest are required");
    }

    if (user.getLibraryCards().isEmpty()) {
      throw new LibraryCardRequiredException();
    }

    Date now = new Date();
    dataAccessRequestDAO.insertDraftDataAccessRequest(
        dar.getReferenceId(),
        user.getUserId(),
        now,
        now,
        now,
        dar.getData()
    );
    syncDataAccessRequestDatasets(dar.getDatasetIds(), dar.getReferenceId());

    return findByReferenceId(dar.getReferenceId());
  }

  /**
   * First delete any rows with the current reference id. This will allow us to keep (referenceId,
   * dataset_id) unique Takes in a list of datasetIds and a referenceId and adds them to the
   * dar_dataset collection
   *
   * @param datasetIds  List of Integers that represent the datasetIds
   * @param referenceId ReferenceId of the corresponding DAR
   */
  private void syncDataAccessRequestDatasets(List<Integer> datasetIds, String referenceId) {
    List<DarDataset> darDatasets = datasetIds.stream()
        .map(datasetId -> new DarDataset(referenceId, datasetId))
        .toList();
    dataAccessRequestDAO.deleteDARDatasetRelationByReferenceId(referenceId);

    if (!darDatasets.isEmpty()) {
      dataAccessRequestDAO.insertAllDarDatasets(darDatasets);
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
   * brand new DAR from scratch.
   *
   * @param user              The create User
   * @param dataAccessRequest DataAccessRequest with populated DAR data
   * @return The created DAR.
   */
  public DataAccessRequest createDataAccessRequest(User user, DataAccessRequest dataAccessRequest) {
    validateDar(user, dataAccessRequest);

    Date now = new Date();
    DataAccessRequestData darData = dataAccessRequest.getData();

    DataAccessRequest existingDar = dataAccessRequestDAO.findByReferenceId(
        dataAccessRequest.getReferenceId());
    if (existingDar != null && !existingDar.getDraft()) {
      throw new SubmittedDARCannotBeEditedException();
    }
    Integer collectionId;
    // Only create a new DarCollection if we haven't done so already
    if (Objects.nonNull(existingDar) && Objects.nonNull(existingDar.getCollectionId())) {
      collectionId = existingDar.getCollectionId();
    } else {
      String darCodeSequence = "DAR-" + counterService.getNextDarSequence();
      collectionId = darCollectionDAO.insertDarCollection(darCodeSequence, user.getUserId(), now);
    }
    String referenceId;
    List<Integer> datasetIds = dataAccessRequest.getDatasetIds();
    if (Objects.nonNull(existingDar)) {
      referenceId = dataAccessRequest.getReferenceId();
      dataAccessRequestDAO.updateDraftToSubmittedForCollection(collectionId,
          referenceId);
      dataAccessRequestDAO.updateDataByReferenceId(
          referenceId,
          user.getUserId(),
          now,
          now,
          now,
          darData,
          user.getEraCommonsId());
    } else {
      referenceId = UUID.randomUUID().toString();
      dataAccessRequestDAO.insertDataAccessRequest(
          collectionId,
          referenceId,
          user.getUserId(),
          now,
          now,
          now,
          now,
          darData,
          user.getEraCommonsId());
    }
    syncDataAccessRequestDatasets(datasetIds, referenceId);
    return findByReferenceId(referenceId);
  }

  /**
   * Create a progress report for the given DataAccessRequest.
   * The parent DAR is just passed in for validation purposes.
   *
   * @param user              The User
   * @param progressReport    The DataAccessRequest
   * @param parentDar         The parent DataAccessRequest
   * @return The created progress report.
   */
  public DataAccessRequest createProgressReport(User user, DataAccessRequest progressReport, DataAccessRequest parentDar) {
    validateProgressReport(user, progressReport, parentDar);

    String referenceId = progressReport.getReferenceId();
    List<Integer> progressReportDatasetIds = progressReport.getDatasetIds();
    Set<Integer> darDatasetIds = dataAccessRequestDAO.findApprovedDatasetsByDar(parentDar.getReferenceId());
    if (!darDatasetIds.containsAll(progressReportDatasetIds)) {
      throw new BadRequestException("Progress report can only be created for approved datasets in the parent DAR");
    }
    dataAccessRequestDAO.insertProgressReport(
          Integer.valueOf(progressReport.getParentId()),
          progressReport.getCollectionId(),
          referenceId,
          user.getUserId(),
          progressReport.getData());
    syncDataAccessRequestDatasets(progressReportDatasetIds, referenceId);
    return findByReferenceId(referenceId);
  }

  public void validateProgressReport(User user, DataAccessRequest progressReport, DataAccessRequest parentDar) {
    validateDar(user, progressReport);
    if (parentDar.getDraft()) {
      throw new BadRequestException(
          "Cannot create a progress report for a draft Data Access Request");
    }
    if (progressReport.getDatasetIds() == null || progressReport.getDatasetIds().isEmpty() ) {
      throw new BadRequestException("At least one dataset is required");
    }
    if (!parentDar.getDatasetIds().containsAll(progressReport.getDatasetIds())) {
      throw new BadRequestException("Progress report can only be created for datasets in the parent DAR");
    }
    if (progressReport.getData().getProgressReportSummary() == null ||
        progressReport.getData().getProgressReportSummary().isEmpty()) {
      throw new BadRequestException("Progress report summary is required");
    }
    if (progressReport.getData().getIntellectualPropertySummary() == null ||
        progressReport.getData().getIntellectualPropertySummary().isEmpty()) {
      throw new BadRequestException("Intellectual Property Summary is required");
    }
  }

  public void validateDar(User user, DataAccessRequest dar) {
    if (Objects.isNull(user) || Objects.isNull(dar) || Objects.isNull(
        dar.getReferenceId()) || Objects.isNull(dar.getData())) {
      throw new IllegalArgumentException("User and DataAccessRequest are required");
    }

    if (user.getLibraryCards().isEmpty()) {
      throw new NIHComplianceRuleException();
    }

    userService.hasValidActiveERACredentials(user);

    validateInternalCollaborators(dar, user);
    validateNoKeyPersonnelDuplicates(dar.getData());
  }

  @VisibleForTesting
  public void validateInternalCollaborators(DataAccessRequest payload, User requestingUser) {
    Integer institution = requestingUser.getInstitutionId();
    List<Collaborator> internalCollaborators = payload.getData().getInternalCollaborators();
    for (Collaborator collaborator : internalCollaborators) {
      User collabUser = userDAO.findUserByEmail(collaborator.getEmail());
      if (collabUser == null) {
        throw new NotFoundException(
            "Unable to find User with the provided email: " + collaborator.getEmail());
      }
      if (!Objects.equals(collabUser.getInstitutionId(), institution)) {
        throw new BadRequestException(
            "Collaborator " + collaborator.getEmail() + " is not part of the same institution, "
                + requestingUser.getInstitution().getName());
      }
      List<LibraryCard> libraryCards = collabUser.getLibraryCards();
      if (libraryCards.isEmpty()) {
        throw new BadRequestException(
            "Collaborator " + collaborator.getEmail() + " does not have a library card.");
      }
    }
  }

  /**
   * Update an existing DataAccessRequest. Replaces DataAccessRequestData.
   *
   * @param user The User
   * @param dar  The DataAccessRequest
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
      //Instead I'm going to throw an UnableToExecuteStatementException
      //Response class will catch it, log it, and throw a 500 through the "unableToExecuteExceptionHandler"
      //on the Resource class, just like it would with a SQLException
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

    if (!emailValidator.isValid(piEmail) || !emailValidator.isValid(soEmail)
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
    List<DataAccessRequest> expiredDars = dataAccessRequestDAO.findAgedDARsByEmailTypeOlderThanInterval(
        type.getTypeInt(), interval, MINIMUM_SUBMITTED_DATE_FOR_DAR_EXPIRATIONS);
    expiredDars.forEach(expiredDar -> {
      try {
        String referenceId = expiredDar.getReferenceId();
        User user = userDAO.findUserById(expiredDar.getUserId());
        String darCode = expiredDar.getDarCode();
        String userName = user.getDisplayName();
        if (user.getEmail() == null) {
          // Do not throw here.  Log information about the DAR since this will continue
          // to appear broken until manual intervention is taken to resolve the missing user
          // email address
          logException(new Exception(String.format(
              "Email address for user %d (%s) not found for expiring warning.  DAR reference id: %s",
              expiredDar.getUserId(), userName, referenceId)));
        } else {
          switch (type) {
            case DAR_EXPIRATION_REMINDER:
              sendDarExpirationReminderMessage(user, darCode, user.getUserId(), referenceId);
              break;
            case DAR_EXPIRED:
              sendDarExpiredMessage(user, darCode, user.getUserId(), referenceId);
          }
        }
      } catch (Exception e) {
        logException(e);
      }
    });
  }

  public void sendReminderMessage(Integer voteId) throws IOException, TemplateException {
    Vote vote = voteDAO.findVoteById(voteId);
    Election election = electionDAO.findElectionWithFinalVoteById(vote.getElectionId());
    DarCollection collection = darCollectionDAO.findDARCollectionByReferenceId(
        election.getReferenceId());
    User user = findUserById(vote.getUserId());
    String voteUrl = serverUrl + "dar_collection/%d".formatted(collection.getDarCollectionId());
    emailService.sendMessage(new ReminderMessage(user, vote, collection.getDarCode(), election.getElectionType(),
        voteUrl), user.getUserId());
    voteDAO.updateVoteReminderFlag(voteId, true);
  }

  private User findUserById(Integer id) throws IllegalArgumentException {
    User user = userDAO.findUserById(id);
    if (user == null) {
      throw new NotFoundException("Could not find dacUser for specified id : " + id);
    }
    return user;
  }

  /**
   * Send a message to a researcher that their data access request has expired.
   *
   * @param researcher  the researcher to send the message to
   * @param darCode     the data access request code that's expired
   * @param userId      the user id of the person sending the message
   * @param referenceId the data access request reference id that's expired
   */
  public void sendDarExpiredMessage(User researcher, String darCode, Integer userId,
      String referenceId)
      throws TemplateException, IOException {
    emailService.sendMessage(new DarExpiredMessage(researcher, darCode, referenceId), userId);
  }

  /**
   * Remind the user that their data access request is about to expire.
   *
   * @param user        the user to send the message to
   * @param darCode     the data access request code that's about to expire
   * @param userId      the user id of the person sending the message
   * @param referenceId the data access request reference id that is expiring
   */
  public void sendDarExpirationReminderMessage(User user, String darCode, Integer userId,
      String referenceId)
      throws TemplateException, IOException {
    emailService.sendMessage(new DarExpirationReminderMessage(user, darCode, referenceId), userId);
  }

}
