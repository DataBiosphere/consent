package org.broadinstitute.consent.http.mail.message;

import com.sendgrid.Mail;

import javax.mail.MessagingException;
import java.io.Writer;
import java.util.List;
import java.util.Set;

public class ResearcherApprovedMessage extends MailMessage {

    private final String APPROVED_DAR = "Your DUOS Data Access Request Results";

    public List<Mail> researcherApprovedMessage(Set<String> toAddresses, String fromAddress, Writer template, String darCode) throws MessagingException {
        return generateEmailMessages(toAddresses, fromAddress, template, darCode, null);
    }

    @Override
    String assignSubject(String referenceId, String type) {
        return APPROVED_DAR;
    }
}
