package org.broadinstitute.consent.http.mail.message;

import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;

import javax.mail.MessagingException;
import java.io.Writer;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class MailMessage {

    protected List<Mail> generateEmailMessages(Set<String> toAddresses, String fromAddress, Writer template, String referenceId, String type) throws MessagingException {
        if (toAddresses == null || toAddresses.isEmpty()) {
            throw new MessagingException("List of to-addresses cannot be empty");
        }
        Content content = new Content("text/html", template.toString());
        String subject = assignSubject(referenceId, type);
        return toAddresses.stream().map(
                address -> new Mail(new Email(fromAddress), subject, new Email(address), content)
            ).collect(Collectors.toList());
    }

    abstract String assignSubject(String referenceId, String type);

}