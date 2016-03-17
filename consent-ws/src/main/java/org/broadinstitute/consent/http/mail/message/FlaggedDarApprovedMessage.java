package org.broadinstitute.consent.http.mail.message;

import java.io.Writer;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

public class FlaggedDarApprovedMessage extends MailMessage{

    /* This message is sent to the Admin when a Dataset that requires owners Approval is approved by te DAC.*/
    private final String ADMIN_APPROVED_DAR = "%s that requires data owners reviewing approved.";

    public MimeMessage flaggedDarMessage(Session session, Writer template, String referenceId, String type) throws MessagingException {
        return generateEmailMessage(session, template, referenceId, type);
    }

    @Override
    String assignSubject(String referenceId, String type) {
        return String.format(ADMIN_APPROVED_DAR, referenceId);
    }
}