package org.broadinstitute.consent.http.mail.message;

import com.sendgrid.helpers.mail.Mail;

import java.io.Writer;
import java.util.List;

public class DisabledDatasetMessage extends MailMessage {

    private final static String MISSING_DATASET = "Datasets not available for Data Access Request Application id: %s.";

    public List<Mail> disabledDatasetMessage(String toAddress, String fromAddress, Writer template, String referenceId, String type) {
        return generateEmailMessage(toAddress, fromAddress, template, referenceId, type);
    }

    @Override
    String assignSubject(String referenceId, String type) {
        return String.format(MISSING_DATASET, referenceId);
    }
}
