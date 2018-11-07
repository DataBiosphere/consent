package org.broadinstitute.consent.http.mail.message;

import com.sendgrid.Mail;

import javax.mail.MessagingException;
import java.io.Writer;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class DarCancelMessage extends MailMessage {

    private final String CANCEL_DAR = "The Data Access Request with ID %s has been cancelled.";

    public Collection<Mail> cancelDarMessage(Set<String> toAddresses, String fromAddress, Writer template, String referenceId, String type) throws MessagingException {
        return generateEmailMessages(toAddresses, fromAddress, template, referenceId, type);
    }

    @Override
    String assignSubject(String referenceId, String type) {
        return String.format(CANCEL_DAR, referenceId);
    }
}