package org.broadinstitute.consent.http.mail.message;

import com.sendgrid.Content;
import com.sendgrid.Email;
import com.sendgrid.Mail;

import javax.mail.MessagingException;
import java.io.Writer;

public abstract class MailMessage {

    protected Mail generateEmailMessage(String toAddress, String fromAddress, Writer template, String referenceId, String type) throws MessagingException {
        Content content = new Content("text/html", template.toString());
        String subject = assignSubject(referenceId, type);
        return new Mail(new Email(fromAddress), subject, new Email(toAddress), content);
    }

    abstract String assignSubject(String referenceId, String type);

}