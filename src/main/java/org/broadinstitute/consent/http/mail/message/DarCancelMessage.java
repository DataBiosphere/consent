package org.broadinstitute.consent.http.mail.message;

import com.sendgrid.helpers.mail.Mail;

import java.io.Writer;
import java.util.Collection;

public class DarCancelMessage extends MailMessage {

    private final String CANCEL_DAR = "The Data Access Request with ID %s has been cancelled.";

    public Mail cancelDarMessage(String toAddress, String fromAddress, Writer template, String referenceId, String type) {
        return generateEmailMessage(toAddress, fromAddress, template, referenceId, type);
    }

    @Override
    String assignSubject(String referenceId, String type) {
        return String.format(CANCEL_DAR, referenceId);
    }
}