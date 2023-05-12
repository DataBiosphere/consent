package org.broadinstitute.consent.http.mail.message;

import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import java.io.Writer;

public abstract class MailMessage {

  protected Mail generateEmailMessage(String toAddress, String fromAddress, Writer template,
      String referenceId, String type) {
    Content content = new Content("text/html", template.toString());
    String subject = assignSubject(referenceId, type);
    return new Mail(new Email(fromAddress), subject, new Email(toAddress), content);
  }

  abstract String assignSubject(String referenceId, String type);

}