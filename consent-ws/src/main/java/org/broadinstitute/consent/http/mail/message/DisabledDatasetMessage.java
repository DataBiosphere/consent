package org.broadinstitute.consent.http.mail.message;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import java.io.Writer;

public class DisabledDatasetMessage extends MailMessage {

    private final static String MISSING_DATASET = "Datasets not available for Data Access Request Application id: %s.";

    public MimeMessage disabledDatasetMessage(Session session, Writer template, String referenceId, String type) throws MessagingException {
        return generateEmailMessage(session, template, referenceId, type);
    }

    @Override
    String assignSubject(String referenceId, String type) {
        return String.format(MISSING_DATASET, referenceId);
    }
}
