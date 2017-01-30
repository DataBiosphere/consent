package org.broadinstitute.consent.http.mail.message;

import com.sendgrid.Mail;

import javax.mail.MessagingException;
import java.io.Writer;

public class DarCancelMessage extends MailMessage {

    private final String CANCEL_DAR = "The Data Access Request with ID %s has been cancelled.";

    public Mail cancelDarMessage(String toAddress, String fromAddress, Writer template, String referenceId, String type) throws MessagingException {
        return generateEmailMessage(toAddress, fromAddress, template, referenceId, type);
    }

    @Override
    String assignSubject(String referenceId, String type) {
        return String.format(CANCEL_DAR, referenceId);
    }
}