package org.broadinstitute.consent.http.mail.message;

import com.sendgrid.helpers.mail.Mail;
import java.io.Writer;
import javax.mail.MessagingException;

public class DaaRequestMessage extends MailMessage {
  private final String NEW_DAA_LIBRARY_CARD_REQUEST = "New DAA-Library Card Relationship Request in DUOS";

  public Mail newDaaRequestMessage(String toAddress, String fromAddress, Writer template, String daaId)
      throws MessagingException {
    return generateEmailMessage(toAddress, fromAddress, template, daaId, null);
  }

  @Override
  String assignSubject(String referenceId, String type) {
    return NEW_DAA_LIBRARY_CARD_REQUEST;
  }

}
