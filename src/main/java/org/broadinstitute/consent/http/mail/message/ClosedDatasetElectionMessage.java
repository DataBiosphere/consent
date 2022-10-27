package org.broadinstitute.consent.http.mail.message;

import com.sendgrid.helpers.mail.Mail;

import java.io.Writer;
import java.util.Collection;

public class ClosedDatasetElectionMessage extends MailMessage {

    private final String CLOSED_DATASET_ELECTIONS = "Report of closed Dataset elections.";

    public Collection<Mail> closedDatasetElectionMessage(String toAddress, String fromAddress, Writer template, String referenceId, String type) {
        return generateEmailMessage(toAddress, fromAddress, template, referenceId, type);
    }

    @Override
    String assignSubject(String referenceId, String type) {
        return CLOSED_DATASET_ELECTIONS;
    }
}
