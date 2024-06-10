package org.broadinstitute.consent.http.mail.message;

import com.sendgrid.helpers.mail.Mail;
import java.io.Writer;
import javax.mail.MessagingException;

public class NewDAAUploadResearcherMessage extends MailMessage {
  private final String NEW_DAA_UPLOAD = "New DAA uploaded for DAC in DUOS";

  public Mail newDAAUploadResearcherMessage(String toAddress, String fromAddress, Writer template, String dacName)
      throws MessagingException {
    return generateEmailMessage(toAddress, fromAddress, template, dacName, null);
  }

  @Override
  String assignSubject(String referenceId, String type) {
    return NEW_DAA_UPLOAD;
  }

}
