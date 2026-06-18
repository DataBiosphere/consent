package org.broadinstitute.consent.http.service;

import com.google.inject.Inject;
import freemarker.template.TemplateException;
import jakarta.ws.rs.BadRequestException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.broadinstitute.consent.http.db.AcknowledgementDAO;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.mail.message.ResearcherCloseoutCompletedMessage;
import org.broadinstitute.consent.http.models.Acknowledgement;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.util.ConsentLogger;
import org.jdbi.v3.core.Jdbi;

public class AcknowledgementService implements ConsentLogger {

  public static final String DAR_CLOSEOUT_CHAIR_REF = "dar_closeout_chair_ref_";
  private final AcknowledgementDAO acknowledgementDAO;
  private final DataAccessRequestDAO dataAccessRequestDAO;
  private final EmailService emailService;

  @Inject
  public AcknowledgementService(Jdbi jdbi, EmailService emailService) {
    this.acknowledgementDAO = jdbi.onDemand(AcknowledgementDAO.class);
    this.dataAccessRequestDAO = jdbi.onDemand(DataAccessRequestDAO.class);
    this.emailService = emailService;
  }

  public Map<String, Acknowledgement> findAcknowledgementsForUser(User user) {
    return acknowledgementListToMap(
        acknowledgementDAO.findAcknowledgementsForUser(user.getUserId()));
  }

  public Acknowledgement findAcknowledgementForUserByKey(User user, String key) {
    return acknowledgementDAO.findAcknowledgementsByKeyForUser(key, user.getUserId());
  }

  public Map<String, Acknowledgement> makeAcknowledgements(List<String> keys, User user) {
    Integer userId = user.getUserId();
    keys.forEach(key -> handleCloseoutAcknowledgement(key, user));
    for (String key : keys) {
      acknowledgementDAO.upsertAcknowledgement(key, userId);
    }
    List<Acknowledgement> acknowledgementList =
        acknowledgementDAO.findAcknowledgementsForUser(keys, userId);
    return acknowledgementListToMap(acknowledgementList);
  }

  private void handleCloseoutAcknowledgement(String key, User user) {
    if (key.startsWith(DAR_CLOSEOUT_CHAIR_REF)) {
      String referenceId = key.replace(DAR_CLOSEOUT_CHAIR_REF, "");
      DataAccessRequest dar = dataAccessRequestDAO.findByReferenceId(referenceId);
      Acknowledgement existingAck =
          acknowledgementDAO.findAcknowledgementsByKeyForUser(key, user.getUserId());
      if (existingAck != null) {
        throw new BadRequestException(
            "Closeout acknowledgement already exists for %s".formatted(dar.getDarCode()));
      }
      if (!dar.getIsCloseoutProgressReport()) {
        throw new BadRequestException(
            "Closeout acknowledgement is only valid for closeout progress reports for DAR %s"
                .formatted(dar.getDarCode()));
      }
      try {
        sendResearcherCloseoutCompletedMessage(user, dar.getDarCode(), referenceId);
      } catch (IOException | TemplateException e) {
        logException(
            "Unable to send researcher closeout completed message for DAR %s"
                .formatted(dar.getDarCode()),
            e);
      }
    }
  }

  /**
   * Send a message to a user that their closeout has been completed.
   *
   * @param user the user to send the message to
   * @param darCode the data access request code for which closeout is completed
   * @param referenceId the data access request reference id for which closeout is completed
   */
  public void sendResearcherCloseoutCompletedMessage(User user, String darCode, String referenceId)
      throws TemplateException, IOException {
    emailService.sendMessage(
        new ResearcherCloseoutCompletedMessage(user, darCode, referenceId), user.getUserId());
  }

  private Map<String, Acknowledgement> acknowledgementListToMap(
      List<Acknowledgement> acknowledgements) {
    return acknowledgements.stream()
        .collect(Collectors.toMap(Acknowledgement::getAckKey, Function.identity()));
  }

  public void deleteAcknowledgementForUserByKey(User user, String key) {
    acknowledgementDAO.deleteAcknowledgement(key, user.getUserId());
  }
}
