package org.broadinstitute.consent.http.mail.message;

import com.sendgrid.Mail;

import javax.mail.MessagingException;
import java.io.Writer;
import java.util.List;
import java.util.Set;

public class HelpReportMessage extends MailMessage {

    private final String HELP_REPORT = "New report has been created by %s.";

    public List<Mail> newHelpReportMessage(Set<String> toAddress, String fromAddress, Writer template, String userName) throws MessagingException {
        return generateEmailMessages(toAddress, fromAddress, template, null, userName);
    }

    @Override
    String assignSubject(String type, String userName) {
        return String.format(HELP_REPORT, userName);
    }
}
