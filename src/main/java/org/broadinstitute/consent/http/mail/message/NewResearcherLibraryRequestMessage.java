package org.broadinstitute.consent.http.mail.message;

import com.sendgrid.helpers.mail.Mail;

import javax.mail.MessagingException;
import java.io.Writer;

public class NewResearcherLibraryRequestMessage extends MailMessage{

    private final String NEW_RESEARCHER = "New Library Card Request in DUOS";

    public Mail newResearcherLibraryRequestMessage(String toAddress, String fromAddress, Writer template) throws MessagingException {
        return generateEmailMessage(toAddress, fromAddress, template, null, null);
    }

    @Override
    String assignSubject(String referenceId, String type) {
        return NEW_RESEARCHER;
    }
}
