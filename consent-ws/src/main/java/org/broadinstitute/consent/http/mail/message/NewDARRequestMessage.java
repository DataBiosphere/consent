package org.broadinstitute.consent.http.mail.message;

import com.sendgrid.Mail;

import javax.mail.MessagingException;
import java.io.Writer;

public class NewDARRequestMessage extends MailMessage{

    private final String NEW_DAR_REQUEST = "Create an election for Data Access Request id: %s.";

    public Mail newDARRequestMessage(String toAddress, String fromAddress, Writer template, String referenceId, String type) throws MessagingException {
        return generateEmailMessage(toAddress, fromAddress, template, referenceId, type);
    }

    @Override
    String assignSubject(String referenceId, String type) {
        return String.format(NEW_DAR_REQUEST, referenceId);
    }
}
