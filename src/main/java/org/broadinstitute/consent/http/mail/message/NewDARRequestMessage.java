package org.broadinstitute.consent.http.mail.message;

import com.sendgrid.helpers.mail.Mail;

import java.io.Writer;
import java.util.Collection;

public class NewDARRequestMessage extends MailMessage{

    private final String NEW_DAR_REQUEST = "Create an election for Data Access Request id: %s.";

    public Mail newDARRequestMessage(String toAddress, String fromAddress, Writer template, String referenceId, String type) {
        return generateEmailMessage(toAddress, fromAddress, template, referenceId, type);
    }

    @Override
    String assignSubject(String referenceId, String type) {
        return String.format(NEW_DAR_REQUEST, referenceId);
    }
}
