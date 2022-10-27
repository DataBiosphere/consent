package org.broadinstitute.consent.http.mail.message;

import com.sendgrid.helpers.mail.Mail;

import java.io.Writer;
import java.util.List;

public class DelegateResponsibilitiesMessage extends MailMessage{

    private final String NEW_ROLES = "You have been assigned a New Role in DUOS.";

    public List<Mail> delegateResponsibilitiesMessage(String toAddress, String fromAddress, Writer template) {
        return generateEmailMessage(toAddress, fromAddress, template, null, null);
    }

    @Override
    String assignSubject(String referenceId, String type) {
        return NEW_ROLES;
    }
}
