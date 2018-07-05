package org.broadinstitute.consent.http.mail.message;

import com.sendgrid.Mail;

import javax.mail.MessagingException;
import java.io.Writer;
import java.util.List;

public class HelpReportMessage extends MailMessage {

    private final String HELP_REPORT = "New report has been created by %s.";

    public List<Mail> newHelpReportMessage(List<String> toAddress, String fromAddress, Writer template) throws MessagingException {
        return generateEmailMessage(toAddress, fromAddress, template, null, null);
    }

    @Override
    String assignSubject(String userName, String type) {
        return String.format(HELP_REPORT, userName);
    }
}
