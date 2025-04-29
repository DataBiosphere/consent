package org.broadinstitute.consent.http.service;

import com.google.common.collect.Streams;
import com.google.inject.Inject;
import com.sendgrid.Response;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import jakarta.ws.rs.NotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.broadinstitute.consent.http.configurations.ConsentConfiguration;
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
import org.broadinstitute.consent.http.mail.message.DaaRequestMessage;
import org.broadinstitute.consent.http.mail.message.DarExpiredMessage;
import org.broadinstitute.consent.http.mail.message.DataCustodianApprovalMessage;
import org.broadinstitute.consent.http.mail.message.DatasetApprovedMessage;
import org.broadinstitute.consent.http.mail.message.DatasetDeniedMessage;
import org.broadinstitute.consent.http.mail.message.DatasetSubmittedMessage;
import org.broadinstitute.consent.http.mail.message.MailMessage;
import org.broadinstitute.consent.http.mail.message.NewCaseMessage;
import org.broadinstitute.consent.http.mail.message.NewDAAUploadResearcherMessage;
import org.broadinstitute.consent.http.mail.message.NewDAAUploadSOMessage;
import org.broadinstitute.consent.http.mail.message.NewDARRequestMessage;
import org.broadinstitute.consent.http.mail.message.NewResearcherLibraryRequestMessage;
import org.broadinstitute.consent.http.mail.message.ReminderMessage;
import org.broadinstitute.consent.http.mail.message.ResearcherApprovedMessage;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DarCollection;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.models.dto.DatasetMailDTO;
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
  private final String fromAccount;
  private final String serverUrl;

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
      ConsentConfiguration config) {
    this.collectionDAO = collectionDAO;
    this.userDAO = userDAO;
    this.electionDAO = electionDAO;
    this.voteDAO = voteDAO;
    this.templateHelper = helper;
    this.emailDAO = emailDAO;
    this.datasetDAO = datasetDAO;
    this.dacDAO = dacDAO;
    this.sendGridAPI = sendGridAPI;
    this.serverUrl = config.getServicesConfiguration().getLocalURL();
    this.fromAccount = config.getMailConfiguration().getGoogleAccount();
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
      String content) {
    Instant now = Instant.now();
    Instant dateSent = (Objects.nonNull(response) && response.getStatusCode() < 400) ? now : null;
    emailDAO.insert(
        entityReferenceId,
        voteId,
        userId,
        emailType.getTypeInt(),
        dateSent,
        content,
        Objects.nonNull(response) ? response.getBody() : null,
        Objects.nonNull(response) ? response.getStatusCode() : null,
        now);
  }

  private void sendMessage(MailMessage mailMessage, Integer userId) throws IOException, TemplateException {
    Writer out = new StringWriter();
    Template template = templateHelper.getTemplate(mailMessage.getTemplateName());
    template.process(mailMessage.createModel(serverUrl), out);
    String content = out.toString();
    Mail message = new Mail(new Email(fromAccount), mailMessage.createSubject(),
        new Email(mailMessage.toUser.getEmail()), new Content("text/html", content));
    Response response = sendGridAPI.sendMessage(message, mailMessage.toUser.getEmail());
    saveEmailAndResponse(
        response,
        mailMessage.getEntityReferenceId(),
        mailMessage.getVoteId(),
        userId,
        mailMessage.emailType,
        content);
  }

  public List<org.broadinstitute.consent.http.models.mail.MailMessage> fetchEmailMessagesByType(EmailType emailType, Integer limit,
      Integer offset) {
    return emailDAO.fetchMessagesByType(emailType.getTypeInt(), limit, offset);
  }

  public List<org.broadinstitute.consent.http.models.mail.MailMessage> fetchEmailMessagesByCreateDate(Date start, Date end, Integer limit,
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
      User user, Map<String, List<String>> sendList, String researcherName, String darCode)
      throws TemplateException, IOException {
    sendMessage(new NewDARRequestMessage(user, darCode, sendList, researcherName), user.getUserId());
  }

  public void sendReminderMessage(Integer voteId) throws IOException, TemplateException {
    Vote vote = voteDAO.findVoteById(voteId);
    Election election = electionDAO.findElectionWithFinalVoteById(vote.getElectionId());
    DarCollection collection = collectionDAO.findDARCollectionByReferenceId(election.getReferenceId());
    User user = findUserById(vote.getUserId());
    String voteUrl = serverUrl + "dar_collection/%d".formatted(collection.getDarCollectionId());
    sendMessage(new ReminderMessage(user, vote, collection.getDarCode(), election.getElectionType(), voteUrl), user.getUserId());
    voteDAO.updateVoteReminderFlag(voteId, true);
  }

  public void sendDarNewCollectionElectionMessage(List<User> users, DarCollection darCollection)
      throws IOException, TemplateException {
    String electionType = "Data Access Request";
    String darCode = darCollection.getDarCode();
    for (User user : users) {
      sendMessage(new NewCaseMessage(user, darCode, electionType), user.getUserId());
    }
  }

  public void sendResearcherDarApproved(
      String darCode,
      Integer researcherId,
      List<DatasetMailDTO> datasets,
      String dataUseRestriction)
      throws TemplateException, IOException {
    User user = userDAO.findUserById(researcherId);
    sendMessage(
        new ResearcherApprovedMessage(user, darCode, datasets, dataUseRestriction), researcherId);
  }

  public void sendDataCustodianApprovalMessage(
      User custodian,
      String darCode,
      List<DatasetMailDTO> datasets,
      String dataDepositorName,
      String researcherEmail)
      throws TemplateException, IOException {
    sendMessage(
        new DataCustodianApprovalMessage(
            custodian, darCode, datasets, dataDepositorName, researcherEmail),
        custodian.getUserId());
  }

  public void sendDatasetSubmittedMessage(
      User dacChair, User dataSubmitter, String dacName, String datasetName)
      throws TemplateException, IOException {
    sendMessage(
        new DatasetSubmittedMessage(dacChair, dataSubmitter.getDisplayName(), datasetName, dacName),
        dacChair.getUserId());
  }

  public void sendDatasetApprovedMessage(User user, String dacName, String datasetName)
      throws TemplateException, IOException {
    sendMessage(new DatasetApprovedMessage(user, dacName, datasetName), user.getUserId());
  }

  public void sendDatasetDeniedMessage(
      User user, String dacName, String datasetName, String dacEmail)
      throws TemplateException, IOException {
    sendMessage(new DatasetDeniedMessage(user, dacName, datasetName, dacEmail), user.getUserId());
  }

  public void sendNewResearcherMessage(User researcher, User signingOfficial)
      throws TemplateException, IOException {
    sendMessage(
        new NewResearcherLibraryRequestMessage(signingOfficial, researcher),
        researcher.getUserId());
  }

  public void sendDaaRequestMessage(
      User signingOfficial, User requestUser, String daaName, Integer daaId)
      throws TemplateException, IOException {
    sendMessage(
        new DaaRequestMessage(signingOfficial, requestUser, daaName, daaId),
        requestUser.getUserId());
  }

  public void sendNewDAAUploadSOMessage(
      User signingOfficial,
      String dacName,
      String previousDaaName,
      String newDaaName,
      Integer userId)
      throws TemplateException, IOException {
    sendMessage(
        new NewDAAUploadSOMessage(signingOfficial, dacName, previousDaaName, newDaaName), userId);
  }

  public void sendNewDAAUploadResearcherMessage(
      User researcher, String dacName, String previousDaaName, String newDaaName, Integer userId)
      throws TemplateException, IOException {
    sendMessage(
        new NewDAAUploadResearcherMessage(
            researcher, dacName, previousDaaName, newDaaName),
        userId);
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
   * @param researcher the researcher to send the message to
   * @param darCode the data access request code that's expired
   * @param userId the user id of the person sending the message
   */
  public void sendDarExpiredMessage(User researcher, String darCode, Integer userId)
      throws TemplateException, IOException {
    sendMessage(new DarExpiredMessage(researcher, darCode), userId);
  }
}
