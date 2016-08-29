package org.broadinstitute.consent.http.mail.message;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import java.io.Writer;

public class NewResearcherCreatedMessage extends MailMessage{

    private final String NEW_RESEARCHER_CREATED = "Review new Researcher Profile.";

    public MimeMessage newResearcherCreatedMessage(Session session, Writer template, String referenceId, String type) throws MessagingException {
        return generateEmailMessage(session, template, referenceId, type);
    }

    @Override
    String assignSubject(String referenceId, String type) {
        return NEW_RESEARCHER_CREATED;
    }
}
