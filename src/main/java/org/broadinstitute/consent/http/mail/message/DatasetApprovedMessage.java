package org.broadinstitute.consent.http.mail.message;

import com.sendgrid.helpers.mail.Mail;
import java.io.Writer;
import javax.mail.MessagingException;

public class DatasetApprovedMessage extends MailMessage {

  private final String DATASET_APPROVED = "Dataset approved for DUOS";

  public Mail datasetApprovedMessage(String toAddress, String fromAddress, Writer template)
      throws MessagingException {
    return generateEmailMessage(toAddress, fromAddress, template, null, null);
  }

  @Override
  String assignSubject(String referenceId, String type) {
    return DATASET_APPROVED;
  }
}
