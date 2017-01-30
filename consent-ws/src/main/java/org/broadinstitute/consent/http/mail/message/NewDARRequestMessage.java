package org.broadinstitute.consent.http.mail.message;

import com.sendgrid.Mail;

import javax.mail.MessagingException;
import java.io.Writer;
import java.util.Collection;
import java.util.List;

public class NewDARRequestMessage extends MailMessage{

    private final String NEW_DAR_REQUEST = "Create an election for Data Access Request id: %s.";

    public Collection<Mail> newDARRequestMessage(List<String> toAddresses, String fromAddress, Writer template, String referenceId, String type) throws MessagingException {
        return generateEmailMessage(toAddresses, fromAddress, template, referenceId, type);
    }

    @Override
    String assignSubject(String referenceId, String type) {
        return String.format(NEW_DAR_REQUEST, referenceId);
    }
}
