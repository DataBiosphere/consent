package org.broadinstitute.consent.http.mail.message;

import com.sendgrid.helpers.mail.Mail;

import javax.mail.MessagingException;
import java.io.Writer;
import java.util.Collection;
import java.util.Set;

public class NewDARRequestMessage extends MailMessage{

    private final String NEW_DAR_REQUEST = "Create an election for Data Access Request id: %s.";

    public Collection<Mail> newDARRequestMessage(Set<String> toAddresses, String fromAddress, Writer template, String referenceId, String type) throws MessagingException {
        return generateEmailMessages(toAddresses, fromAddress, template, referenceId, type);
    }

    @Override
    String assignSubject(String referenceId, String type) {
        return String.format(NEW_DAR_REQUEST, referenceId);
    }
}
