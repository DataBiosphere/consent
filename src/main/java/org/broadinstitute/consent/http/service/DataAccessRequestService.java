package org.broadinstitute.consent.http.service;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotAcceptableException;
import jakarta.ws.rs.NotFoundException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.validator.routines.EmailValidator;
import org.broadinstitute.consent.http.db.DAOContainer;
import org.broadinstitute.consent.http.db.DarCollectionDAO;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.MatchDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.exceptions.LibraryCardRequiredException;
import org.broadinstitute.consent.http.exceptions.NIHComplianceRuleException;
import org.broadinstitute.consent.http.exceptions.SubmittedDARCannotBeEditedException;
import org.broadinstitute.consent.http.models.Collaborator;
import org.broadinstitute.consent.http.models.DarDataset;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.LibraryCard;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.dao.DataAccessRequestServiceDAO;
import org.broadinstitute.consent.http.util.ConsentLogger;
import org.jdbi.v3.core.JdbiException;
import org.jdbi.v3.core.statement.UnableToExecuteStatementException;

public class DataAccessRequestService implements ConsentLogger {
  public static final String EXPIRE_WARN_INTERVAL = "11 months";
  public static final String EXPIRE_NOTICE_INTERVAL = "1 year";

  private final CounterService counterService;
  private final DataAccessRequestDAO dataAccessRequestDAO;
  private final DarCollectionDAO darCollectionDAO;
  private final ElectionDAO electionDAO;
  private final MatchDAO matchDAO;
  private final VoteDAO voteDAO;
  private final UserDAO userDAO;
  private final UserService userService;
  private final DataAccessRequestServiceDAO dataAccessRequestServiceDAO;

  private final DacService dacService;

  @Inject
  public DataAccessRequestService(CounterService counterService, DAOContainer container,
      DacService dacService, DataAccessRequestServiceDAO dataAccessRequestServiceDAO, UserService userService) {
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
    Set<Integer> darDatasetIds = dataAccessRequestDAO.findDatasetApprovalsByDars(List.of(parentDar.getReferenceId()));
    if (!darDatasetIds.containsAll(progressReportDatasetIds)) {
      throw new BadRequestException("Progress report can only be created for approved datasets in the parent DAR");
    }
    try {
      dataAccessRequestDAO.insertProgressReport(
          Integer.valueOf(progressReport.getParentId()),
          progressReport.getCollectionId(),
          referenceId,
          user.getUserId(),
          progressReport.getData());
    } catch (JdbiException e) {
      throw new BadRequestException(
          "Unable to create progress report for Data Access Request " + parentDar.getReferenceId());
    }
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

}
