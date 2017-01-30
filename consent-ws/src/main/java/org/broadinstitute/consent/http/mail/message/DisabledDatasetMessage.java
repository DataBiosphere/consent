package org.broadinstitute.consent.http.mail.message;

import com.sendgrid.Mail;

import javax.mail.MessagingException;
import java.io.Writer;

public class DisabledDatasetMessage extends MailMessage {

    private final static String MISSING_DATASET = "Datasets not available for Data Access Request Application id: %s.";

    public Mail disabledDatasetMessage(String toAddress, String fromAddress, Writer template, String referenceId, String type) throws MessagingException {
        return generateEmailMessage(toAddress, fromAddress, template, referenceId, type);
    }

    @Override
    String assignSubject(String referenceId, String type) {
        return String.format(MISSING_DATASET, referenceId);
    }
}
