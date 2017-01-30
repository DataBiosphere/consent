package org.broadinstitute.consent.http.mail.message;

import com.sendgrid.Mail;

import javax.mail.MessagingException;
import java.io.Writer;

public class NewResearcherCreatedMessage extends MailMessage{

    private final String NEW_RESEARCHER_CREATED = "Review Researcher Profile.";

    public Mail newResearcherCreatedMessage(String toAddress, String fromAddress, Writer template, String referenceId, String type) throws MessagingException {
        return generateEmailMessage(toAddress, fromAddress, template, referenceId, type);
    }

    @Override
    String assignSubject(String referenceId, String type) {
        return NEW_RESEARCHER_CREATED;
    }
}
