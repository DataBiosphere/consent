package org.broadinstitute.consent.http.service;

import com.google.common.collect.Streams;
import com.google.inject.Inject;
import com.sendgrid.Response;
import freemarker.template.TemplateException;
import jakarta.ws.rs.NotFoundException;
import java.io.IOException;
import java.io.Writer;
import java.time.Instant;
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
import org.broadinstitute.consent.http.db.ConsentDAO;
import org.broadinstitute.consent.http.db.DarCollectionDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.MailMessageDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.EmailType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.mail.SendGridAPI;
import org.broadinstitute.consent.http.mail.freemarker.FreeMarkerTemplateHelper;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.DarCollection;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.models.dto.DatasetMailDTO;
import org.broadinstitute.consent.http.models.mail.MailMessage;

public class EmailService {

  private final DarCollectionDAO collectionDAO;
  private final ConsentDAO consentDAO;
  private final UserDAO userDAO;
  private final ElectionDAO electionDAO;
  private final MailMessageDAO emailDAO;
  private final VoteDAO voteDAO;
  private final FreeMarkerTemplateHelper templateHelper;
  private final SendGridAPI sendGridAPI;
  private final String SERVER_URL;
  private static final String LOG_VOTE_DUL_URL = "dul_review";
  private static final String LOG_VOTE_ACCESS_URL = "access_review";

  public enum ElectionTypeString {

    DATA_ACCESS("Data Access Request"),
    TRANSLATE_DUL("Data Use Limitations"),
    RP("Research Purpose");

    private final String value;

    ElectionTypeString(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }

    public static String getValue(String value) {
      for (ElectionType e : ElectionType.values()) {
        if (e.getValue().equalsIgnoreCase(value)) {
          return e.getValue();
        }
      }
      return null;
    }
  }

  @Inject
  public EmailService(DarCollectionDAO collectionDAO, ConsentDAO consentDAO,
      VoteDAO voteDAO, ElectionDAO electionDAO,
      UserDAO userDAO, MailMessageDAO emailDAO, SendGridAPI sendGridAPI,
      FreeMarkerTemplateHelper helper, String serverUrl) {
    this.collectionDAO = collectionDAO;
    this.consentDAO = consentDAO;
    this.userDAO = userDAO;
    this.electionDAO = electionDAO;
    this.voteDAO = voteDAO;
    this.templateHelper = helper;
    this.emailDAO = emailDAO;
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
    List<User> distinctUsers = Streams.concat(admins.stream(), chairPersons.stream())
        .filter(u -> Boolean.TRUE.equals(u.getEmailPreference()))
        .distinct()
        .toList();
    for (User user : distinctUsers) {
      Writer template = templateHelper.getNewDARRequestTemplate(SERVER_URL, user.getDisplayName(),
          collection.getDarCode());
      Map<String, String> data = retrieveForNewDAR(collection.getDarCode(), user);
      Optional<Response> response = sendGridAPI.sendNewDARRequests(user.getEmail(),
          data.get("entityId"), data.get("electionType"), template);
      saveEmailAndResponse(
          response.orElse(null),
          collection.getDarCode(),
          null,
          user.getUserId(),
          EmailType.NEW_DAR,
          template
      );
    }
  }

  public void sendReminderMessage(Integer voteId) throws IOException, TemplateException {
    Map<String, String> data = retrieveForVote(voteId);
    String voteUrl = generateUserVoteUrl(SERVER_URL, data.get("electionType"), data.get("voteId"),
        data.get("entityId"), data.get("rpVoteId"));
    Writer template = templateHelper.getReminderTemplate(data.get("userName"),
        data.get("electionType"), data.get("entityName"), voteUrl);
    Optional<Response> response = sendGridAPI.sendReminderMessage(data.get("email"),
        data.get("entityName"), data.get("electionType"), template);
    voteDAO.updateVoteReminderFlag(voteId, true);
    saveEmailAndResponse(
        response.orElse(null),
        data.get("electionId"),
        voteId,
        Integer.valueOf(data.get("dacUserId")),
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

  public void sendDisabledDatasetsMessage(User user, List<String> disabledDatasets,
      String dataAccessRequestId) throws IOException, TemplateException {
    Writer template = templateHelper.getDisabledDatasetsTemplate(user.getDisplayName(),
        disabledDatasets, dataAccessRequestId, SERVER_URL);
    Optional<Response> response = sendGridAPI.sendDisabledDatasetMessage(user.getEmail(),
        dataAccessRequestId, null, template);
    saveEmailAndResponse(
        response.orElse(null),
        dataAccessRequestId,
        null,
        user.getUserId(),
        EmailType.DISABLED_DATASET,
        template
    );
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
      String datasetName) throws Exception {
    Writer template = templateHelper.getDatasetDeniedTemplate(user.getDisplayName(), datasetName,
        dacName);
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

  private User findUserById(Integer id) throws IllegalArgumentException {
    User user = userDAO.findUserById(id);
    if (user == null) {
      throw new NotFoundException("Could not find dacUser for specified id : " + id);
    }
    return user;
  }

  private String generateUserVoteUrl(String serverUrl, String electionType, String voteId,
      String entityId, String rpVoteId) {
    if (electionType.equals("Data Use Limitations")) {
      return serverUrl + LOG_VOTE_DUL_URL + "/" + voteId + "/" + entityId;
    } else {
      if (electionType.equals("Data Access Request") || electionType.equals("Research Purpose")) {
        return serverUrl + LOG_VOTE_ACCESS_URL + "/" + entityId + "/" + voteId + "/" + rpVoteId;
      }
    }
    return serverUrl;
  }

  private Map<String, String> retrieveForVote(Integer voteId) {
    Vote vote = voteDAO.findVoteById(voteId);
    Election election = electionDAO.findElectionWithFinalVoteById(vote.getElectionId());
    User user = findUserById(vote.getUserId());

    Map<String, String> dataMap = new HashMap<>();
    dataMap.put("userName", user.getDisplayName());
    dataMap.put("electionType", retrieveElectionTypeString(election.getElectionType()));
    dataMap.put("entityId", election.getReferenceId());
    dataMap.put("entityName",
        retrieveReferenceId(election.getElectionType(), election.getReferenceId()));
    dataMap.put("electionId", election.getElectionId().toString());
    dataMap.put("dacUserId", user.getUserId().toString());
    dataMap.put("email", user.getEmail());
    if (dataMap.get("electionType").equals(ElectionTypeString.DATA_ACCESS.getValue())) {
      dataMap.put("rpVoteId", findRpVoteId(election.getElectionId(), user.getUserId()));
    } else if (dataMap.get("electionType").equals(ElectionTypeString.RP.getValue())) {
      dataMap.put("voteId", findDataAccessVoteId(election.getElectionId(), user.getUserId()));
      dataMap.put("rpVoteId", voteId.toString());
    } else {
      dataMap.put("voteId", voteId.toString());
    }
    return dataMap;
  }

  private String findRpVoteId(Integer electionId, Integer userId) {
    Integer rpElectionId = electionDAO.findRPElectionByElectionAccessId(electionId);
    return (rpElectionId != null) ? ((voteDAO.findVoteByElectionIdAndUserId(rpElectionId, userId)
        .getVoteId()).toString()) : "";
  }

  private String findDataAccessVoteId(Integer electionId, Integer userId) {
    Integer dataAccessElectionId = electionDAO.findAccessElectionByElectionRPId(electionId);
    return (dataAccessElectionId != null) ? ((voteDAO.findVoteByElectionIdAndUserId(
        dataAccessElectionId, userId).getVoteId()).toString()) : "";
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

  private String retrieveReferenceId(String electionType, String referenceId) {
    if (electionType.equals(ElectionType.TRANSLATE_DUL.getValue())) {
      Consent consent = consentDAO.findConsentById(referenceId);
      return Objects.nonNull(consent) ? consent.getName() : " ";
    } else {
      DarCollection collection = collectionDAO.findDARCollectionByReferenceId(referenceId);
      return Objects.nonNull(collection) ? collection.getDarCode() : " ";
    }
  }

  private String retrieveElectionTypeString(String electionType) {
    if (electionType.equals(ElectionType.TRANSLATE_DUL.getValue())) {
      return ElectionTypeString.TRANSLATE_DUL.getValue();
    } else if (electionType.equals(ElectionType.DATA_ACCESS.getValue())) {
      return ElectionTypeString.DATA_ACCESS.getValue();
    }
    return ElectionTypeString.RP.getValue();
  }

}
