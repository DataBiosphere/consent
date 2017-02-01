package org.broadinstitute.consent.http.mail.message;

import com.sendgrid.Mail;

import javax.mail.MessagingException;
import java.io.Writer;

public class NewCaseMessage extends MailMessage{

    private final String NEWCASE_DUL = "Log vote on Data Use Limitations case id: %s.";
    private final String NEWCASE_DAR = "Log votes on Data Access Request case id: %s.";

    public Mail newCaseMessage(String address, String fromAddress, Writer template, String referenceId, String type) throws MessagingException {
        return generateEmailMessage(address, fromAddress, template, referenceId, type);
    }

    @Override
    String assignSubject(String referenceId, String type) {
        if(type.equals("Data Use Limitations"))
            return String.format(NEWCASE_DUL, referenceId);
        else {
            return String.format(NEWCASE_DAR, referenceId);
        }
    }
}
