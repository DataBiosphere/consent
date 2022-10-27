package org.broadinstitute.consent.http.mail.message;

import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;

import java.io.Writer;
import java.util.List;
import java.util.stream.Collectors;

public abstract class MailMessage {

    protected List<Mail> generateEmailMessage(String toAddress, String fromAddress, Writer template, String referenceId, String type) {
        Content content = new Content("text/html", template.toString());
        String subject = assignSubject(referenceId, type);
        return List.of(new Mail(new Email(fromAddress), subject, new Email(toAddress), content));
    }

    abstract String assignSubject(String referenceId, String type);

}