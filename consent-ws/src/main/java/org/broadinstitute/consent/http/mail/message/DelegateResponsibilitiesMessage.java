package org.broadinstitute.consent.http.mail.message;

import com.sendgrid.Mail;

import javax.mail.MessagingException;
import java.io.Writer;
import java.util.List;

public class DelegateResponsibilitiesMessage extends MailMessage{

    private final String NEW_ROLES = "You have been assigned a New Role in DUOS.";

    public List<Mail> delegateResponsibilitiesMessage(List<String> toAddresses, String fromAddress, Writer template) throws MessagingException {
        return generateEmailMessage(toAddresses, fromAddress, template, null, null);
    }

    @Override
    String assignSubject(String referenceId, String type) {
        return NEW_ROLES;
    }
}
