package org.broadinstitute.consent.http.mail.message;

import com.sendgrid.helpers.mail.Mail;

import javax.mail.MessagingException;
import java.io.Writer;
import java.util.Collections;
import java.util.List;

public class DatasetDeniedMessage extends MailMessage{

    private final String DATASET_DENIED = "Dataset denied for DUOS";

    public List<Mail> datasetDeniedMessage(String toAddress, String fromAddress, Writer template) throws MessagingException {
        return generateEmailMessages(Collections.singleton(toAddress), fromAddress, template, null, null);
    }

    @Override
    String assignSubject(String referenceId, String type) {
        return DATASET_DENIED;
    }
}
