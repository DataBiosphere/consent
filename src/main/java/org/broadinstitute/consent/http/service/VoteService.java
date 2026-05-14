package org.broadinstitute.consent.http.service;

import static java.util.function.Predicate.not;

import com.google.api.client.http.HttpStatusCodes;
import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import freemarker.template.TemplateException;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.validator.routines.EmailValidator;
import org.broadinstitute.consent.http.db.DAOContainer;
import org.broadinstitute.consent.http.db.DacDAO;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.enumeration.DataUseTranslationType;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.exceptions.ConsentConflictException;
import org.broadinstitute.consent.http.mail.message.DACMembersDARRADARApprovedMessage;
import org.broadinstitute.consent.http.mail.message.ResearcherApprovedProgressReportMessage;
import org.broadinstitute.consent.http.mail.message.ResearcherDarApprovedMessage;
import org.broadinstitute.consent.http.mail.message.SoDARApproved;
import org.broadinstitute.consent.http.mail.message.SoPRApproved;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetProperty;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.Study;
import org.broadinstitute.consent.http.models.StudyProperty;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder;
import org.broadinstitute.consent.http.models.dto.DatasetMailDTO;
import org.broadinstitute.consent.http.service.dao.VoteServiceDAO;
import org.broadinstitute.consent.http.util.ComplianceLogger;
import org.broadinstitute.consent.http.util.ConsentLogger;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.glassfish.jersey.server.ContainerRequest;

public class VoteService implements ConsentLogger {

  private final UserDAO userDAO;
  private final DacDAO dacDAO;
  private final DataAccessRequestDAO dataAccessRequestDAO;
  private final DatasetDAO datasetDAO;
  private final ElectionDAO electionDAO;
  private final EmailService emailService;
  private final VoteDAO voteDAO;
  private final VoteServiceDAO voteServiceDAO;
  private final OntologyService ontologyService;

  @Inject
  public VoteService(
      DAOContainer daoContainer,
      EmailService emailService,
      VoteServiceDAO voteServiceDAO,
      OntologyService ontologyService) {
    this.userDAO = daoContainer.getUserDAO();
    this.dacDAO = daoContainer.getDacDAO();
    this.dataAccessRequestDAO = daoContainer.getDataAccessRequestDAO();
    this.datasetDAO = daoContainer.getDatasetDAO();
    this.electionDAO = daoContainer.getElectionDAO();
    this.emailService = emailService;
    this.voteDAO = daoContainer.getVoteDAO();
    this.voteServiceDAO = voteServiceDAO;
    this.ontologyService = ontologyService;
  }

  /**
   * Create votes for an election
   *
   * @param election The Election
   * @param electionType The Election type
   * @param isManualReview Is this a manual review election
   * @return List of votes
   */
  @SuppressWarnings("DuplicatedCode")
  public List<Vote> createVotes(
      Election election, ElectionType electionType, Boolean isManualReview) {
    Dac dac = electionDAO.findDacForElection(election.getElectionId());
    Set<User> users;
    if (dac != null) {
      users = userDAO.findUsersEnabledToVoteByDAC(dac.getDacId());
    } else {
      users = userDAO.findNonDacUsersEnabledToVote();
    }
    List<Vote> votes = new ArrayList<>();
    if (users != null) {
      for (User user : users) {
        votes.addAll(createVotesForUser(user, election, electionType, isManualReview));
      }
    }
    return votes;
  }

  /**
   * Create election votes for a user
   *
   * @param user DACUser
   * @param election Election
   * @param electionType ElectionType
   * @param isManualReview Is election manual review
   * @return List of created votes
   */
  public List<Vote> createVotesForUser(
      User user, Election election, ElectionType electionType, Boolean isManualReview) {
    Dac dac = electionDAO.findDacForElection(election.getElectionId());
    List<Vote> votes = new ArrayList<>();
    Integer dacVoteId =
        voteDAO.insertVote(user.getUserId(), election.getElectionId(), VoteType.DAC.getValue());
    votes.add(voteDAO.findVoteById(dacVoteId));
    if (isDacChairPerson(dac, user)) {
      Integer chairVoteId =
          voteDAO.insertVote(
              user.getUserId(), election.getElectionId(), VoteType.CHAIRPERSON.getValue());
      votes.add(voteDAO.findVoteById(chairVoteId));
      // Requires Chairperson role to create a final and agreement vote in the Data Access case
      if (electionType.equals(ElectionType.DATA_ACCESS)) {
        Integer finalVoteId =
            voteDAO.insertVote(
                user.getUserId(), election.getElectionId(), VoteType.FINAL.getValue());
        votes.add(voteDAO.findVoteById(finalVoteId));
        if (Boolean.FALSE.equals(isManualReview)) {
          Integer agreementVoteId =
              voteDAO.insertVote(
                  user.getUserId(), election.getElectionId(), VoteType.AGREEMENT.getValue());
          votes.add(voteDAO.findVoteById(agreementVoteId));
        }
      }
    }
    return votes;
  }

  public List<Vote> findVotesByIds(List<Integer> voteIds) {
    if (voteIds.isEmpty()) {
      return Collections.emptyList();
    }
    return voteDAO.findVotesByIds(voteIds);
  }

  /**
   * Delete any votes in Open elections for the specified user in the specified Dac.
   *
   * @param dac The Dac we are restricting elections to
   * @param user The Dac member we are deleting votes for
   */
  public void deleteOpenDacVotesForUser(Dac dac, User user) {
    List<Integer> openElectionIds =
        electionDAO.findOpenElectionsByDacId(dac.getDacId()).stream()
            .map(Election::getElectionId)
            .toList();
    if (!openElectionIds.isEmpty()) {
      List<Integer> openUserVoteIds =
          voteDAO.findVotesByElectionIds(openElectionIds).stream()
              .filter(v -> v.getUserId().equals(user.getUserId()))
              .map(Vote::getVoteId)
              .toList();
      if (!openUserVoteIds.isEmpty()) {
        voteDAO.removeVotesByIds(openUserVoteIds);
      }
    }
  }

  /**
   * Update vote values. 'FINAL' votes impact elections so matching elections marked as
   * ElectionStatus.CLOSED as well. Approved 'FINAL' votes trigger an approval email to researchers.
   *
   * @param votes List of Votes to update
   * @param voteValue Value to update the votes to
   * @param rationale Value to update the rationales to. Only update if non-null.
   * @param user The user making the update
   * @return The updated Vote
   * @throws IllegalArgumentException when there are non-open, non-rp elections on any of the votes
   */
  public List<Vote> updateVotesWithValue(
      List<Vote> votes, boolean voteValue, String rationale, User user)
      throws IllegalArgumentException {
    validateVotesCanUpdate(votes);
    try {
      List<Vote> updatedVotes = voteServiceDAO.updateVotesWithValue(votes, voteValue, rationale);
      if (voteValue) {
        try {
          sendDatasetApprovalNotifications(updatedVotes, user);
        } catch (Exception e) {
          // We can recover from email errors, log it and don't fail the overall process.
          String voteIds =
              votes.stream()
                  .map(Vote::getVoteId)
                  .map(Object::toString)
                  .collect(Collectors.joining(","));
          String message =
              "Error notifying researchers and custodians for votes: ["
                  + voteIds
                  + "]: "
                  + e.getMessage();
          logException(message, e);
        }
      }
      return updatedVotes;
    } catch (Exception _) {
      throw new IllegalArgumentException("Unable to update election votes.");
    }
  }

  /**
   * Review all positive, FINAL votes and send a notification to the researcher and data custodians
   * describing the approved access to datasets on their Data Access Request.
   *
   * @param votes List of Vote objects. In practice, this will be a batch of votes for a group of
   *     elections for datasets that all have the same data use restriction in a single
   *     DarCollection. This method is flexible enough to send email for any number of unrelated
   *     elections in various DarCollections.
   * @param user The user sending approval notifications
   */
  public void sendDatasetApprovalNotifications(List<Vote> votes, User user) {
    boolean radarApproved =
        votes.stream().anyMatch(v -> VoteType.RADAR_APPROVE.getValue().equals(v.getType()));
    List<Integer> finalElectionIds =
        votes.stream()
            .filter(
                Vote::getVote) // Safety check to ensure we're only emailing for approved election
            .filter(
                v ->
                    VoteType.FINAL.getValue().equalsIgnoreCase(v.getType())
                        || VoteType.RADAR_APPROVE.getValue().equalsIgnoreCase(v.getType()))
            .map(Vote::getElectionId)
            .distinct()
            .toList();

    List<Election> finalElections = electionDAO.findElectionsByIds(finalElectionIds);

    List<String> finalElectionReferenceIds =
        finalElections.stream().map(Election::getReferenceId).distinct().toList();

    List<DataAccessRequest> dars =
        dataAccessRequestDAO.findByReferenceIds(finalElectionReferenceIds);

    List<Integer> datasetIds = finalElections.stream().map(Election::getDatasetId).toList();
    List<Dataset> datasets =
        datasetIds.isEmpty() ? List.of() : datasetDAO.findDatasetsByIdList(datasetIds);

    // For each dar, email the researcher summarizing the approved datasets in that dar
    dars.forEach(
        dar -> {
          // Get the datasets in this collection that have been approved
          List<Dataset> approvedDatasetsInDar =
              datasets.stream()
                  .filter(d -> dar.getDatasetIds().contains(d.getDatasetId()))
                  .toList();

          if (!approvedDatasetsInDar.isEmpty()) {
            String darCode = dar.getDarCode();
            User researcher = userDAO.findUserById(dar.getUserId());
            List<DatasetMailDTO> datasetMailDTOs =
                approvedDatasetsInDar.stream()
                    .map(
                        d ->
                            new DatasetMailDTO(
                                d.getName(), d.getDatasetIdentifier(), getDataLocationUrl(d)))
                    .toList();

            // Get all Data Use translations, distinctly in the case that there are several with the
            // same
            // data use, and then conjoin them for email display.
            String translation =
                approvedDatasetsInDar.stream()
                    .map(
                        dataset ->
                            ontologyService.translateDataUse(
                                dataset.getDataUse(), DataUseTranslationType.DATASET))
                    .distinct()
                    .collect(Collectors.joining(";"));

            try {
              if (dar.getProgressReport()) {
                sendResearcherProgressReportApproved(
                    researcher, darCode, datasetMailDTOs, translation, radarApproved);
              } else {
                sendResearcherDarApproved(
                    researcher, darCode, datasetMailDTOs, translation, radarApproved);
              }
            } catch (Exception e) {
              logException("Error sending researcher dar approved email: " + e.getMessage(), e);
            }
            try {
              notifyCustodiansOfApprovedDatasets(
                  approvedDatasetsInDar, researcher, darCode, radarApproved);
            } catch (Exception e) {
              logException(
                  "Error notifying custodians of dar approved email: " + e.getMessage(), e);
            }
            try {
              notifySigningOfficialsOfApprovedDatasets(
                  approvedDatasetsInDar, researcher, dar, darCode, translation, radarApproved);
            } catch (Exception e) {
              logException(
                  "Error notifying signing officials of dar approved email: " + e.getMessage(), e);
            }
            try {
              notifyDACOfRadarApprovals(
                  approvedDatasetsInDar, researcher, dar.getReferenceId(), darCode, radarApproved);
            } catch (Exception e) {
              logException("Error notifying DAC of dar approved email: " + e.getMessage(), e);
            }
          }
        });
  }

  @VisibleForTesting
  protected void sendResearcherDarApproved(
      User researcher,
      String darCode,
      List<DatasetMailDTO> datasets,
      String dataUseRestriction,
      boolean radarApproved)
      throws TemplateException, IOException {
    emailService.sendMessage(
        new ResearcherDarApprovedMessage(
            researcher, darCode, datasets, dataUseRestriction, radarApproved),
        researcher.getUserId());
  }

  @VisibleForTesting
  protected void sendResearcherProgressReportApproved(
      User researcher,
      String darCode,
      List<DatasetMailDTO> datasets,
      String dataUseRestriction,
      boolean radarApproved)
      throws TemplateException, IOException {
    emailService.sendMessage(
        new ResearcherApprovedProgressReportMessage(
            researcher, darCode, datasets, dataUseRestriction, radarApproved),
        researcher.getUserId());
  }

  @VisibleForTesting
  protected void sendNewSoDARApprovedEmail(
      User so,
      String darCode,
      User researcher,
      String referenceId,
      List<Dataset> datasets,
      String dataUseRestriction,
      boolean radarApproved)
      throws TemplateException, IOException {
    emailService.sendMessage(
        new SoDARApproved(
            so, darCode, researcher, referenceId, datasets, dataUseRestriction, radarApproved),
        so.getUserId());
  }

  @VisibleForTesting
  protected void sendNewSoProgressReportApprovedEmail(
      User so,
      String darCode,
      User researcher,
      String referenceId,
      List<Dataset> datasets,
      String dataUseRestriction,
      boolean radarApproved)
      throws TemplateException, IOException {
    emailService.sendMessage(
        new SoPRApproved(
            so, darCode, researcher, referenceId, datasets, dataUseRestriction, radarApproved),
        so.getUserId());
  }

  @VisibleForTesting
  protected void sendNewDARRADARApprovalToDAC(
      User dacMember,
      String darCode,
      String referenceId,
      List<DatasetMailDTO> datasetList,
      User researcher)
      throws TemplateException, IOException {
    emailService.sendMessage(
        new DACMembersDARRADARApprovedMessage(
            dacMember, darCode, researcher, referenceId, datasetList),
        dacMember.getUserId());
  }

  @VisibleForTesting
  protected void notifyDACOfRadarApprovals(
      List<Dataset> approvedDatasets,
      User researcher,
      String referenceId,
      String darCode,
      boolean radarApproved) {
    if (!radarApproved) {
      return;
    }
    Map<Integer, Set<DatasetMailDTO>> dacIdToDatasetMap = new HashMap<>();
    approvedDatasets.forEach(
        approvedDataset ->
            dacIdToDatasetMap
                .computeIfAbsent(approvedDataset.getDacId(), d -> new HashSet<>())
                .add(
                    new DatasetMailDTO(
                        approvedDataset.getName(),
                        approvedDataset.getDatasetIdentifier(),
                        getDataLocationUrl(approvedDataset))));
    dacIdToDatasetMap.forEach(
        (dacId, datasets) -> {
          List<User> members = dacDAO.findMembersByDacId(dacId);
          members.forEach(
              member -> {
                try {
                  sendNewDARRADARApprovalToDAC(
                      member, darCode, referenceId, datasets.stream().toList(), researcher);
                } catch (TemplateException | IOException e) {
                  logWarn("Error sending DAR approval to DAC: " + e.getMessage(), e);
                }
              });
        });
  }

  @VisibleForTesting
  protected void notifySigningOfficialsOfApprovedDatasets(
      List<Dataset> datasets,
      User researcher,
      DataAccessRequest dar,
      String darCode,
      String translation,
      boolean radarApproved)
      throws TemplateException, IOException {
    if (researcher == null) {
      logWarn(
          "Unable to send new DAR/PR message to Signing Officials: Researcher does not exist: %s"
              .formatted(dar.getUserId()));
      return;
    }
    if (researcher.getInstitutionId() == null) {
      logWarn(
          "Unable to send new DAR/PR message to Signing Officials: Researcher does not have an institution id: %s"
              .formatted(dar.getUserId()));
      return;
    }
    List<User> signingOfficials = userDAO.getSOsByInstitution(researcher.getInstitutionId());
    for (User so : signingOfficials) {
      if (dar.getProgressReport()) {
        sendNewSoProgressReportApprovedEmail(
            so, darCode, researcher, dar.getReferenceId(), datasets, translation, radarApproved);
      } else {
        sendNewSoDARApprovedEmail(
            so, darCode, researcher, dar.getReferenceId(), datasets, translation, radarApproved);
      }
    }
  }

  /**
   * Notify all data submitters, custodians, depositors, and owners of a dataset approval.
   *
   * @param datasets Requested datasets
   * @param researcher The approved researcher
   * @param darCode The DAR Collection Code
   * @throws IllegalArgumentException when there are no custodians or depositors to notify
   */
  @VisibleForTesting
  protected void notifyCustodiansOfApprovedDatasets(
      List<Dataset> datasets, User researcher, String darCode, boolean radarApproved)
      throws IllegalArgumentException {
    Map<User, HashSet<Dataset>> custodianMap = new HashMap<>();

    // Find all the data custodians and submitters to notify for each dataset
    datasets.forEach(
        d -> {
          if (Objects.nonNull(d.getStudy())) {
            Study study = d.getStudy();

            // Data Submitter (study)
            if (Objects.nonNull(study.getCreateUserId())) {
              User submitter = userDAO.findUserById(study.getCreateUserId());
              if (Objects.nonNull(submitter)) {
                custodianMap.putIfAbsent(submitter, new HashSet<>());
                custodianMap.get(submitter).add(d);
              }
            }

            // Data Custodian (study)
            if (Objects.nonNull(study.getProperties())) {
              Type listOfStringType = new TypeToken<ArrayList<String>>() {}.getType();
              Gson gson = GsonUtil.gsonBuilderWithAdapters().create();
              Set<StudyProperty> props = study.getProperties();
              List<String> custodianEmails = new ArrayList<>();
              props.stream()
                  .filter(
                      p -> p.getKey().equals(DatasetRegistrationSchemaV1Builder.dataCustodianEmail))
                  .forEach(
                      p -> {
                        String propValue = p.getValue().toString();
                        try {
                          custodianEmails.addAll(gson.fromJson(propValue, listOfStringType));
                        } catch (Exception e) {
                          logException(
                              "Error finding data custodians for study: " + study.getStudyId(), e);
                        }
                      });
              if (!custodianEmails.isEmpty()) {
                List<User> custodianUsers = userDAO.findUsersByEmailList(custodianEmails);
                custodianUsers.forEach(
                    s -> {
                      custodianMap.putIfAbsent(s, new HashSet<>());
                      custodianMap.get(s).add(d);
                    });
              }
            }
          }

          // Data Submitter (dataset)
          if (Objects.nonNull(d.getCreateUserId())) {
            User submitter = userDAO.findUserById(d.getCreateUserId());
            if (Objects.nonNull(submitter)) {
              custodianMap.putIfAbsent(submitter, new HashSet<>());
              custodianMap.get(submitter).add(d);
            }
          }
        });

    // Filter out invalid emails in custodian map
    EmailValidator emailValidator = EmailValidator.getInstance();
    Map<User, HashSet<Dataset>> validCustodians =
        custodianMap.entrySet().stream()
            .filter(
                e -> e.getKey().getEmail() != null && emailValidator.isValid(e.getKey().getEmail()))
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, HashMap::new));

    if (validCustodians.isEmpty()) {
      String identifiers =
          datasets.stream().map(Dataset::getDatasetIdentifier).collect(Collectors.joining(", "));
      throw new IllegalArgumentException(
          "No submitters, custodians, owners, or depositors found for provided dataset identifiers: "
              + identifiers);
    }
    // For each custodian, notify them of their approved datasets
    for (Map.Entry<User, HashSet<Dataset>> entry : validCustodians.entrySet()) {
      List<DatasetMailDTO> datasetMailDTOs =
          entry.getValue().stream()
              .map(
                  d ->
                      new DatasetMailDTO(
                          d.getName(), d.getDatasetIdentifier(), getDataLocationUrl(d)))
              .toList();
      try {
        emailService.sendDataCustodianApprovalMessage(
            entry.getKey(),
            darCode,
            datasetMailDTOs,
            entry.getKey().getDisplayName(),
            researcher.getEmail(),
            radarApproved);
      } catch (Exception e) {
        logException("Error sending custodian approval email: " + e.getMessage(), e);
      }
    }
  }

  /**
   * The Rationale for RP Votes can be updated for any election status. The Rationale for DataAccess
   * Votes can only be updated for OPEN elections. Votes for elections of other types are not
   * updatable through this method.
   *
   * @param voteIds List of vote ids for DataAccess and RP elections
   * @param rationale The rationale to update
   * @return List of updated votes
   * @throws IllegalArgumentException when there are non-open, non-rp elections on any of the votes
   */
  public List<Vote> updateRationaleByVoteIds(List<Integer> voteIds, String rationale)
      throws IllegalArgumentException {
    List<Vote> votes = voteDAO.findVotesByIds(voteIds);
    validateVotesCanUpdate(votes);
    voteDAO.updateRationaleByVoteIds(voteIds, rationale);
    return findVotesByIds(voteIds);
  }

  private void validateVotesCanUpdate(List<Vote> votes) throws ConsentConflictException {
    List<Election> elections =
        electionDAO.findElectionsByIds(votes.stream().map(Vote::getElectionId).toList());

    // If there are any DataAccess elections in a non-open state, throw an error
    List<Election> nonOpenAccessElections =
        elections.stream()
            .filter(
                election -> election.getElectionType().equals(ElectionType.DATA_ACCESS.getValue()))
            .filter(election -> !election.getStatus().equals(ElectionStatus.OPEN.getValue()))
            .toList();
    if (!nonOpenAccessElections.isEmpty()) {
      throw new ConsentConflictException(
          "One or more of these votes are associated with elections not open for voting.");
    }

    // If there are non-DataAccess or non-RP elections, throw an error
    List<Election> disallowedElections =
        elections.stream()
            .filter(
                election -> !election.getElectionType().equals(ElectionType.DATA_ACCESS.getValue()))
            .filter(election -> !election.getElectionType().equals(ElectionType.RP.getValue()))
            .toList();
    if (!disallowedElections.isEmpty()) {
      throw new ConsentConflictException(
          "There are unsupported election types for the votes provided");
    }
  }

  private boolean isDacChairPerson(Dac dac, User user) {
    if (dac != null) {
      return user.getRoles().stream()
          .anyMatch(
              userRole ->
                  Objects.nonNull(userRole.getRoleId())
                      && Objects.nonNull(userRole.getDacId())
                      && userRole.getRoleId().equals(UserRoles.CHAIRPERSON.getRoleId())
                      && userRole.getDacId().equals(dac.getDacId()));
    }
    return user.getRoles().stream()
        .anyMatch(
            userRole ->
                Objects.nonNull(userRole.getRoleId())
                    && userRole.getRoleId().equals(UserRoles.CHAIRPERSON.getRoleId()));
  }

  public void logDARApprovalOrRejection(
      User user, List<Vote> updatedVotes, ContainerRequest request) {
    List<Integer> approvedElectionIds =
        updatedVotes.stream()
            .filter(v -> v.getType().equals(VoteType.FINAL.getValue()))
            .filter(Vote::getVote)
            .map(Vote::getElectionId)
            .toList();
    List<Integer> approvedDatasetIds =
        electionDAO.findElectionsByIds(approvedElectionIds).stream()
            .map(Election::getDatasetId)
            .toList();
    List<Dataset> approvedDatasets = datasetDAO.findDatasetsByIdList(approvedDatasetIds);
    ComplianceLogger.logDARApproval(
        user, approvedDatasets, request, HttpStatusCodes.STATUS_CODE_OK);

    List<Integer> rejectedElectionIds =
        updatedVotes.stream()
            .filter(v -> v.getType().equals(VoteType.FINAL.getValue()))
            .filter(not(Vote::getVote))
            .map(Vote::getElectionId)
            .toList();
    List<Integer> rejectedDatasetIds =
        electionDAO.findElectionsByIds(rejectedElectionIds).stream()
            .map(Election::getDatasetId)
            .toList();
    List<Dataset> rejectedDatasets = datasetDAO.findDatasetsByIdList(rejectedDatasetIds);
    ComplianceLogger.logDARRejection(
        user, rejectedDatasets, request, HttpStatusCodes.STATUS_CODE_OK);
  }

  private String getDataLocationUrl(Dataset dataset) {
    if (dataset.getProperties() == null || dataset.getProperties().isEmpty()) {
      return null;
    }
    return dataset.getProperties().stream()
        .filter(
            p ->
                p.getSchemaProperty() != null
                    && p.getSchemaProperty()
                        .equalsIgnoreCase(DatasetRegistrationSchemaV1Builder.url))
        .map(DatasetProperty::getPropertyValueAsString)
        .findFirst()
        .orElse(null);
  }
}
