package org.broadinstitute.consent.http.mail.message;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import java.io.Writer;

public class FlaggedDarMessage extends MailMessage{

    /* This message is sent when a DAR is approved by the DAC and that DAR has datasets that require data owner's approval */

    private final String DO_APPROVED_DAR = "Data Access Request %s with datasets that require your review was approved.";

    public MimeMessage flaggedDarMessage(Session session, Writer template, String referenceId, String type) throws MessagingException {
        return generateEmailMessage(session, template, referenceId, type);
    }

    @Override
    String assignSubject(String referenceId, String type) {
        return String.format(DO_APPROVED_DAR, referenceId);
    }
}