package org.broadinstitute.consent.http.mail.message;

import com.sendgrid.Content;
import com.sendgrid.Email;
import com.sendgrid.Mail;
import com.sendgrid.Personalization;

import javax.mail.MessagingException;
import java.io.Writer;
import java.util.Collections;
import java.util.List;

public abstract class MailMessage {

    protected Mail generateEmailMessage(String toAddress, String fromAddress, Writer template, String referenceId, String type) throws MessagingException {
        return generateEmailMessage(Collections.singletonList(toAddress), fromAddress, template, referenceId, type);
    }

    protected Mail generateEmailMessage(List<String> toAddresses, String fromAddress, Writer template, String referenceId, String type) throws MessagingException {
        if (toAddresses == null || toAddresses.isEmpty()) {
            throw new MessagingException("List of to-addresses cannot be empty");
        }
        Content content = new Content("text/html", template.toString());
        String subject = assignSubject(referenceId, type);
        Mail mail = new Mail();
        mail.setFrom(new Email(fromAddress));
        mail.setSubject(subject);
        mail.addContent(content);
        for (String address: toAddresses) {
            Personalization personalization = new Personalization();
            personalization.addBcc(new Email(address));
            mail.addPersonalization(personalization);
        }
        return mail;
    }

    abstract String assignSubject(String referenceId, String type);

}