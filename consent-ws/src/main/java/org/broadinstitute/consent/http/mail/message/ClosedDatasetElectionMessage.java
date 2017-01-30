package org.broadinstitute.consent.http.mail.message;

import com.sendgrid.Mail;

import javax.mail.MessagingException;
import java.io.Writer;
import java.util.Collection;
import java.util.List;

public class ClosedDatasetElectionMessage extends MailMessage {

    private final String CLOSED_DATASET_ELECTIONS = "Report of closed Dataset elections.";

    public Collection<Mail> closedDatasetElectionMessage(List<String> toAddresses, String fromAddress, Writer template, String referenceId, String type) throws MessagingException {
        return generateEmailMessage(toAddresses, fromAddress, template, referenceId, type);
    }

    @Override
    String assignSubject(String referenceId, String type) {
        return CLOSED_DATASET_ELECTIONS;
    }
}
