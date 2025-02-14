package org.broadinstitute.consent.http.service;

import com.google.common.collect.Streams;
import com.google.inject.Inject;
import com.sendgrid.Response;
import freemarker.template.TemplateException;
import jakarta.ws.rs.NotFoundException;
import java.io.IOException;
import java.io.Writer;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.broadinstitute.consent.http.db.DacDAO;
import org.broadinstitute.consent.http.db.DarCollectionDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.MailMessageDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.enumeration.EmailType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.mail.SendGridAPI;
import org.broadinstitute.consent.http.mail.freemarker.FreeMarkerTemplateHelper;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DarCollection;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.models.dto.DatasetMailDTO;
import org.broadinstitute.consent.http.models.mail.MailMessage;
import org.broadinstitute.consent.http.util.ConsentLogger;

public class EmailService implements ConsentLogger {

  private final DarCollectionDAO collectionDAO;
  private final UserDAO userDAO;
  private final ElectionDAO electionDAO;
  private final MailMessageDAO emailDAO;
  private final VoteDAO voteDAO;
  private final DatasetDAO datasetDAO;
  private final DacDAO dacDAO;
  private final FreeMarkerTemplateHelper templateHelper;
  private final SendGridAPI sendGridAPI;
  private final String SERVER_URL;

  @Inject
  public EmailService(
      DarCollectionDAO collectionDAO,
      VoteDAO voteDAO,
      ElectionDAO electionDAO,
      UserDAO userDAO,
      MailMessageDAO emailDAO,
      DatasetDAO datasetDAO,
      DacDAO dacDAO,
      SendGridAPI sendGridAPI,
      FreeMarkerTemplateHelper helper,
      String serverUrl
  ) {
    this.collectionDAO = collectionDAO;
    this.userDAO = userDAO;
    this.electionDAO = electionDAO;
    this.voteDAO = voteDAO;
    this.templateHelper = helper;
    this.emailDAO = emailDAO;
    this.datasetDAO = datasetDAO;
    this.dacDAO = dacDAO;
    this.sendGridAPI = sendGridAPI;
    this.SERVER_URL = serverUrl;
  }

  /**
   * This method saves an email (either sent or unsent) with all available metadata from the
   * SendGrid response.
   */
  private void saveEmailAndResponse(
      @Nullable Response response,
      @Nullable String entityReferenceId,
      @Nullable Integer voteId,
      Integer userId,
      EmailType emailType,
      Writer template) {
    Instant now = Instant.now();
    Instant dateSent = (Objects.nonNull(response) && response.getStatusCode() < 400) ? now : null;
    emailDAO.insert(
        entityReferenceId,
        voteId,
        userId,
        emailType.getTypeInt(),
        dateSent,
        template.toString(),
        Objects.nonNull(response) ? response.getBody() : null,
        Objects.nonNull(response) ? response.getStatusCode() : null,
        now);
  }

  public List<MailMessage> fetchEmailMessagesByType(EmailType emailType, Integer limit,
      Integer offset) {
    return emailDAO.fetchMessagesByType(emailType.getTypeInt(), limit, offset);
  }

  public List<MailMessage> fetchEmailMessagesByCreateDate(Date start, Date end, Integer limit,
      Integer offset) {
    return emailDAO.fetchMessagesByCreateDate(start, end, limit, offset);
  }

  public void sendNewDARCollectionMessage(Integer collectionId)
      throws IOException, TemplateException {
    DarCollection collection = collectionDAO.findDARCollectionByCollectionId(collectionId);
    if (collection == null) {
      logWarn("Sending new DAR Collection message: Could not find collection for specified collection id: " + collectionId);
      return;
    }
    List<User> distinctUsers = getDistinctAdminAndChairUsersForCollection(collection);
    User researcher = userDAO.findUserById(collection.getCreateUserId());
    if (researcher == null) {
      logWarn("Sending new DAR Collection message: Could not find researcher for specified user id: " + collection.getCreateUserId());
    }
    String researcherName = researcher == null ? "Unknown" : researcher.getDisplayName();
    Collection<Dac> dacsInDAR = dacDAO.findDacsForCollectionId(collectionId);
    List<Integer> datasetIds = collection.getDatasets().stream().map(Dataset::getDatasetId).toList();
    List<Dataset> datasetsInDAR = datasetIds.isEmpty() ? List.of() : datasetDAO.findDatasetsByIdList(datasetIds);

    Map<String, List<String>>  sendList = new HashMap<>();
    for (User user : distinctUsers) {
      List<Dac> matchingDacsForUser = getMatchingDacs(user, dacsInDAR);
      for (Dac dac : matchingDacsForUser) {
        List<String> matchingDatasetsForDac = getMatchingDatasets(dac, datasetsInDAR);
        if (matchingDatasetsForDac != null) {
          sendList.put(dac.getName(), matchingDatasetsForDac);
        }
      }
      sendNewDARRequestEmail(user, sendList, researcherName, collection.getDarCode());
    }
  }

  private List<User> getDistinctAdminAndChairUsersForCollection(DarCollection collection) {
    List<User> admins = userDAO.describeUsersByRoleAndEmailPreference(UserRoles.ADMIN.getRoleName(),
        true);
    List<Integer> datasetIds = collection.getDars().values().stream()
        .map(DataAccessRequest::getDatasetIds)
        .flatMap(List::stream)
        .collect(Collectors.toList());
    Set<User> chairPersons = userDAO.findUsersForDatasetsByRole(datasetIds,
        Collections.singletonList(UserRoles.CHAIRPERSON.getRoleName()));
    // Ensure that admins/chairs are not double emailed
    // and filter users that don't want to receive email
    return Streams.concat(admins.stream(), chairPersons.stream())
        .filter(u -> Boolean.TRUE.equals(u.getEmailPreference()))
        .distinct()
        .toList();
  }

  private List<Dac> getMatchingDacs(User user, Collection<Dac> dacsInDAR) {
    List<Integer> dacIDs = user.getRoles().stream()
        .filter(ur -> ur.getDacId() != null)
        .map(UserRole::getDacId)
        .toList();
    return dacsInDAR.stream()
        .filter(dac -> dacIDs.contains(dac.getDacId()))
        .toList();
  }

  private List<String> getMatchingDatasets(Dac dac, List<Dataset> datasetsInDAR) {
    return datasetsInDAR.stream()
        .filter(dataset -> dataset.getDacId() == dac.getDacId())
        .map(dataset -> dataset.getDatasetIdentifier())
        .toList();
  }

  private void sendNewDARRequestEmail(
      User user,
      Map<String, List<String>> sendList,
      String researcherName,
      String darCode
  ) throws TemplateException, IOException {
    Writer template = templateHelper.getNewDARRequestTemplate(
        SERVER_URL,
        user.getDisplayName(),
        sendList,
        researcherName,
        darCode
    );
    Map<String, String> data = retrieveForNewDAR(darCode, user);
    Optional<Response> response = sendGridAPI.sendNewDARRequests(
        user.getEmail(),
        data.get("entityId"),
        data.get("electionType"),
        template
    );
    saveEmailAndResponse(
        response.orElse(null),
        darCode,
        null,
        user.getUserId(),
        EmailType.NEW_DAR,
        template
    );
  }

  public void sendReminderMessage(Integer voteId) throws IOException, TemplateException {
    Vote vote = voteDAO.findVoteById(voteId);
    Election election = electionDAO.findElectionWithFinalVoteById(vote.getElectionId());
    DarCollection collection = collectionDAO.findDARCollectionByReferenceId(election.getReferenceId());
    User user = findUserById(vote.getUserId());
    String voteUrl = SERVER_URL + "dar_collection/%d".formatted(collection.getDarCollectionId());
    Writer template = templateHelper.getReminderTemplate(
        user.getDisplayName(),
        collection.getDarCode(),
        voteUrl);
    Optional<Response> response = sendGridAPI.sendReminderMessage(
        user.getEmail(),
        collection.getDarCode(),
        election.getElectionType(),
        template);
    voteDAO.updateVoteReminderFlag(voteId, true);
    saveEmailAndResponse(
        response.orElse(null),
        String.valueOf(vote.getElectionId()),
        voteId,
        user.getUserId(),
        EmailType.REMINDER,
        template
    );
  }

  public void sendDarNewCollectionElectionMessage(List<User> users, DarCollection darCollection)
      throws IOException, TemplateException {
    String electionType = "Data Access Request";
    String darCode = darCollection.getDarCode();
    for (User user : users) {
      Writer template = templateHelper.getNewCaseTemplate(user.getDisplayName(), electionType,
          darCode, SERVER_URL);
      Optional<Response> response = sendGridAPI.sendNewCaseMessage(user.getEmail(), darCode,
          electionType, template);
      saveEmailAndResponse(
          response.orElse(null),
          darCode,
          null,
          user.getUserId(),
          EmailType.NEW_CASE,
          template
      );
    }
  }

  public void sendResearcherDarApproved(String darCode, Integer researcherId,
      List<DatasetMailDTO> datasets, String dataUseRestriction) throws Exception {
    User user = userDAO.findUserById(researcherId);
    Writer template = templateHelper.getResearcherDarApprovedTemplate(darCode,
        user.getDisplayName(), datasets, dataUseRestriction, user.getEmail());
    Optional<Response> response = sendGridAPI.sendNewResearcherApprovedMessage(user.getEmail(),
        template, darCode);
    saveEmailAndResponse(
        response.orElse(null),
        darCode,
        null,
        user.getUserId(),
        EmailType.RESEARCHER_DAR_APPROVED,
        template
    );
  }

  public void sendDataCustodianApprovalMessage(User custodian,
      String darCode,
      List<DatasetMailDTO> datasets,
      String dataDepositorName,
      String researcherEmail) throws Exception {
    Writer template = templateHelper.getDataCustodianApprovalTemplate(datasets,
        dataDepositorName, darCode, researcherEmail);
    Optional<Response> response = sendGridAPI.sendDataCustodianApprovalMessage(custodian.getEmail(),
        darCode, template);
    saveEmailAndResponse(
        response.orElse(null),
        darCode,
        null,
        custodian.getUserId(),
        EmailType.DATA_CUSTODIAN_APPROVAL,
        template
    );
  }

  public void sendDatasetSubmittedMessage(User dacChair,
      User dataSubmitter,
      String dacName,
      String datasetName) throws Exception {
    Writer template = templateHelper.getDatasetSubmittedTemplate(dacChair.getDisplayName(),
        dataSubmitter.getDisplayName(),
        datasetName,
        dacName);
    Optional<Response> response = sendGridAPI.sendDatasetSubmittedMessage(dacChair.getEmail(),
        template);
    saveEmailAndResponse(
        response.orElse(null),
        datasetName,
        null,
        dacChair.getUserId(),
        EmailType.NEW_DATASET,
        template
    );
  }

  public void sendDatasetApprovedMessage(User user,
      String dacName,
      String datasetName) throws Exception {
    Writer template = templateHelper.getDatasetApprovedTemplate(user.getDisplayName(), datasetName,
        dacName);
    Optional<Response> response = sendGridAPI.sendDatasetApprovedMessage(user.getEmail(), template);
    saveEmailAndResponse(
        response.orElse(null),
        datasetName,
        null,
        user.getUserId(),
        EmailType.NEW_CASE,
        template
    );
  }

  public void sendDatasetDeniedMessage(User user,
      String dacName,
      String datasetName,
      String dacEmail) throws Exception {
    Writer template = templateHelper.getDatasetDeniedTemplate(user.getDisplayName(), datasetName,
        dacName, dacEmail);
    Optional<Response> response = sendGridAPI.sendDatasetDeniedMessage(user.getEmail(), template);
    saveEmailAndResponse(
        response.orElse(null),
        datasetName,
        null,
        user.getUserId(),
        EmailType.NEW_CASE,
        template
    );
  }

  public void sendNewResearcherMessage(User researcher,
      User signingOfficial) throws Exception {
    Writer template = templateHelper.getNewResearcherLibraryRequestTemplate(
        researcher.getDisplayName(), this.SERVER_URL);
    Optional<Response> response = sendGridAPI.sendNewResearcherLibraryRequestMessage(
        signingOfficial.getEmail(), template);
    saveEmailAndResponse(
        response.orElse(null),
        researcher.getUserId().toString(),
        null,
        researcher.getUserId(),
        EmailType.NEW_RESEARCHER,
        template
    );
  }

  public void sendDaaRequestMessage(
      String signingOfficialName,
      String signingOfficialEmail,
      String userName,
      String daaName,
      Integer daaId,
      Integer userId) throws Exception {
    Writer template = templateHelper.getDaaRequestTemplate(signingOfficialName, userName, daaName,
        this.SERVER_URL);
    Optional<Response> response = sendGridAPI.sendDaaRequestMessage(signingOfficialEmail, template, daaId.toString());
    saveEmailAndResponse(
        response.orElse(null),
        daaId.toString(),
        null,
        userId,
        EmailType.NEW_DAA_REQUEST,
        template
    );
  }

  public void sendNewDAAUploadSOMessage(
      String signingOfficialName,
      String signingOfficialEmail,
      String dacName,
      String previousDaaName,
      String newDaaName,
      Integer userId) throws Exception {
    Writer template = templateHelper.getNewDaaUploadSOTemplate(signingOfficialName, dacName,
        newDaaName, previousDaaName, this.SERVER_URL);
    Optional<Response> response = sendGridAPI.sendNewDAAUploadSOMessage(signingOfficialEmail, template, dacName);
    saveEmailAndResponse(
        response.orElse(null),
        dacName,
        null,
        userId,
        EmailType.NEW_DAA_UPLOAD_SO,
        template
    );
  }

  public void sendNewDAAUploadResearcherMessage(
      String researcherUserName,
      String researcherEmail,
      String dacName,
      String previousDaaName,
      String newDaaName,
      Integer userId) throws Exception {
    Writer template = templateHelper.getNewDaaUploadResearcherTemplate(researcherUserName, dacName,
        newDaaName, previousDaaName, this.SERVER_URL);
    Optional<Response> response = sendGridAPI.sendNewDAAUploadResearcherMessage(researcherEmail, template, dacName);
    saveEmailAndResponse(
        response.orElse(null),
        dacName,
        null,
        userId,
        EmailType.NEW_DAA_UPLOAD_RESEARCHER,
        template
    );
  }

  private User findUserById(Integer id) throws IllegalArgumentException {
    User user = userDAO.findUserById(id);
    if (user == null) {
      throw new NotFoundException("Could not find dacUser for specified id : " + id);
    }
    return user;
  }

  private Map<String, String> retrieveForNewDAR(String dataAccessRequestId, User user) {
    Map<String, String> dataMap = new HashMap<>();
    dataMap.put("userName", user.getDisplayName());
    dataMap.put("electionType", "New Data Access Request Case");
    dataMap.put("entityId", dataAccessRequestId);
    dataMap.put("dacUserId", user.getUserId().toString());
    dataMap.put("email", user.getEmail());
    return dataMap;
  }

}
