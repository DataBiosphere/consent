package org.genomebridge.consent.http.mail.message;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import java.io.Writer;

public class NewDARRequestMessage extends MailMessage{

    private final String NEW_DAR_REQUEST = "Create an election for Data Access Request id: %s.";

    public MimeMessage newDARRequestMessage(Session session, Writer template, String referenceId, String type) throws MessagingException {
        return generateEmailMessage(session, template, referenceId, type);
    }

    @Override
    String assignSubject(String referenceId, String type) {
        return String.format(NEW_DAR_REQUEST, referenceId);
    }
}
