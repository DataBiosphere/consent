package org.broadinstitute.consent.http.mail.message;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import java.io.Writer;

public class NewCaseMessage extends MailMessage{

    private final String NEWCASE_DUL = "Log vote on Data Use Limitations case id: %s.";
    private final String NEWCASE_DAR = "Log votes on Data Access Request case id: %s.";

    public MimeMessage newCaseMessage(Session session, Writer template, String referenceId, String type) throws MessagingException {
        return generateEmailMessage(session, template, referenceId, type);
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
