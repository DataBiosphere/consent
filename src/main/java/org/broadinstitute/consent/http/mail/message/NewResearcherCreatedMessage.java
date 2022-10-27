package org.broadinstitute.consent.http.mail.message;

import com.sendgrid.helpers.mail.Mail;

import java.io.Writer;
import java.util.List;

public class NewResearcherCreatedMessage extends MailMessage{

    private final String NEW_RESEARCHER_CREATED = "Review Researcher Profile.";

    public List<Mail> newResearcherCreatedMessage(String toAddress, String fromAddress, Writer template, String referenceId, String type) {
        return generateEmailMessage(toAddress, fromAddress, template, referenceId, type);
    }

    @Override
    String assignSubject(String referenceId, String type) {
        return NEW_RESEARCHER_CREATED;
    }
}
