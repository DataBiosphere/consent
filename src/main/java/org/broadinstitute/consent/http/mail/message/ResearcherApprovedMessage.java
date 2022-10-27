package org.broadinstitute.consent.http.mail.message;

import com.sendgrid.helpers.mail.Mail;

import java.io.Writer;
import java.util.List;

public class ResearcherApprovedMessage extends MailMessage {

    private final String APPROVED_DAR = "Your DUOS Data Access Request Results";

    public List<Mail> researcherApprovedMessage(String toAddress, String fromAddress, Writer template, String darCode) {
        return generateEmailMessage(toAddress, fromAddress, template, darCode, null);
    }

    @Override
    String assignSubject(String referenceId, String type) {
        return APPROVED_DAR;
    }
}
