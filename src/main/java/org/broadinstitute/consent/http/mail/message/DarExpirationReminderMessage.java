package org.broadinstitute.consent.http.mail.message;

import com.sendgrid.helpers.mail.Mail;
import java.io.Writer;

public class DarExpirationReminderMessage extends MailMessage {

  private final String DAR_EXPIRATION_REMINDER = "Remind user of expiring DAR id: %s";

  public Mail darExpirationReminderMessage(String toAddress, String fromAddress, Writer template,
      String referenceId, String type) {
    return generateEmailMessage(toAddress, fromAddress, template, referenceId, type);
  }

  @Override
  String assignSubject(String referenceId, String type) {
    return String.format(DAR_EXPIRATION_REMINDER, referenceId);
  }
}
