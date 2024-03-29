package org.broadinstitute.consent.http.mail.message;

import com.sendgrid.helpers.mail.Mail;
import java.io.Writer;
import javax.mail.MessagingException;

public class DatasetDeniedMessage extends MailMessage {

  private final String DATASET_DENIED = "Dataset denied for DUOS";

  public Mail datasetDeniedMessage(String toAddress, String fromAddress, Writer template)
      throws MessagingException {
    return generateEmailMessage(toAddress, fromAddress, template, null, null);
  }

  @Override
  String assignSubject(String referenceId, String type) {
    return DATASET_DENIED;
  }
}
