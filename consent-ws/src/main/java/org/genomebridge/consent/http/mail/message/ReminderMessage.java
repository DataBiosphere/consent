package org.genomebridge.consent.http.mail.message;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import java.io.Writer;

public class ReminderMessage extends MailMessage {

    private final String REMINDER_DUL = "Urgent: Log vote on Data Use Limitations case id: %s.";
    private final String REMINDER_DAR = "Urgent: Log votes on Data Access Request case id: %s.";
    private final String REMINDER_RP = "Urgent: Log votes on Research Purpose Review case id: %s.";

    public MimeMessage reminderMessage(Session session, Writer template, String referenceId, String type) throws MessagingException {
        return generateEmailMessage(session, template, referenceId, type);
    }

    @Override
    String assignSubject(String referenceId, String type) {
        if(type.equals("Data Use Limitations"))
            return String.format(REMINDER_DUL, referenceId);
        else {
            if (type.equals("Data Access Request")) {
                return String.format(REMINDER_DAR, referenceId);
            }
        }
        return String.format(REMINDER_RP, referenceId);
    }
}