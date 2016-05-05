package org.broadinstitute.consent.http.mail.message;

import java.io.Writer;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

public class DelegateResponsibilitiesMessage extends MailMessage{

    private final String NEW_ROLES = "You have been assigned a New Role in DUOS.";

    public MimeMessage delegateResponsibilitiesMessage(Session session, Writer template) throws MessagingException {
        return generateEmailMessage(session, template, null, null);
    }

    @Override
    String assignSubject(String referenceId, String type) {
        return NEW_ROLES;
    }
}
