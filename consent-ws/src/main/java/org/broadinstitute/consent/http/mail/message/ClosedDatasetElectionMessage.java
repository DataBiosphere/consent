package org.broadinstitute.consent.http.mail.message;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import java.io.Writer;

public class ClosedDatasetElectionMessage extends MailMessage {

    private final String CLOSED_DATASET_ELECTIONS = "Report of closed Dataset elections.";

    public MimeMessage closedDatasetElectionMessgae(Session session, Writer template, String referenceId, String type) throws MessagingException {
        return generateEmailMessage(session, template, referenceId, type);
    }

    @Override
    String assignSubject(String referenceId, String type) {
        return CLOSED_DATASET_ELECTIONS;
    }
}
