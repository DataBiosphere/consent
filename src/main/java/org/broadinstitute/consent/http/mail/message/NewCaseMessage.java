package org.broadinstitute.consent.http.mail.message;

import com.sendgrid.helpers.mail.Mail;

import java.io.Writer;
import java.util.List;

public class NewCaseMessage extends MailMessage{

    private final String NEWCASE_DUL = "Log vote on Data Use Limitations case id: %s.";
    private final String NEWCASE_DAR = "Log votes on Data Access Request case id: %s.";

    public Mail newCaseMessage(String toAddress, String fromAddress, Writer template, String referenceId, String type) {
        return generateEmailMessage(toAddress, fromAddress, template, referenceId, type);
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
