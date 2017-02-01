package org.broadinstitute.consent.http.mail.message;

import com.sendgrid.Mail;

import javax.mail.MessagingException;
import java.io.Writer;

public class CollectMessage extends MailMessage {

    private final String COLLECT_DUL = "Ready for vote collection on Data Use Limitations case id: %s.";
    private final String COLLECT_DAR = "Ready for votes collection on Data Access Request case id: %s.";

    public Mail collectMessage(String toAddress, String fromAddress, Writer template, String referenceId, String type) throws MessagingException {
        return generateEmailMessage(toAddress, fromAddress, template, referenceId, type);
    }

    @Override
    String assignSubject(String referenceId, String type) {
        if(type.equals("Data Use Limitations"))
            return String.format(COLLECT_DUL, referenceId);
        else
            return String.format(COLLECT_DAR, referenceId);
    }

}
