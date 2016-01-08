package org.broadinstitute.consent.http.mail.message;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import java.io.Writer;

public class DarCancelMessage extends MailMessage {

    private final String CANCEL_DAR = "The Data Access Request with ID %s has been cancelled.";

    public MimeMessage cancelDarMessage(Session session, Writer template, String referenceId, String type) throws MessagingException {
        return generateEmailMessage(session, template, referenceId, type);
    }

    @Override
    String assignSubject(String referenceId, String type) {
        return String.format(CANCEL_DAR, referenceId);
    }
}