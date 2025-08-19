package org.broadinstitute.consent.http.service;

import freemarker.template.TemplateException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.broadinstitute.consent.http.db.AcknowledgementDAO;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.models.Acknowledgement;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.util.ConsentLogger;

public class AcknowledgementService implements ConsentLogger {

  public static final String DAR_CLOSEOUT_CHAIR_REF = "dar_closeout_chair_ref_";
  private final AcknowledgementDAO acknowledgementDAO;
  private final DataAccessRequestDAO dataAccessRequestDAO;
  private final EmailService emailService;

  public AcknowledgementService(AcknowledgementDAO acknowledgementDAO, DataAccessRequestDAO dataAccessRequestDAO, EmailService emailService) {
    this.acknowledgementDAO = acknowledgementDAO;
    this.dataAccessRequestDAO = dataAccessRequestDAO;
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
    for (String key : keys) {
      acknowledgementDAO.upsertAcknowledgement(key, userId);
    }
    List<Acknowledgement> acknowledgementList = acknowledgementDAO.findAcknowledgementsForUser(keys,
        userId);
    acknowledgementList.forEach(
        ack -> {
          if (ack.getAckKey().startsWith(DAR_CLOSEOUT_CHAIR_REF)) {
            String referenceId = ack.getAckKey().replace(DAR_CLOSEOUT_CHAIR_REF, "");
            DataAccessRequest dar = dataAccessRequestDAO.findByReferenceId(referenceId);
            try {
              emailService.sendResearcherCloseoutCompletedMessage(user, dar.getDarCode(), referenceId);
            } catch (IOException | TemplateException e) {
              // Log the error but do not fail the acknowledgement creation
              logException("Unable to send researcher closeout completed message for DAR %s".formatted(dar.getDarCode()), e);
            }
          }
        }
    );
    return acknowledgementListToMap(acknowledgementList);
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
