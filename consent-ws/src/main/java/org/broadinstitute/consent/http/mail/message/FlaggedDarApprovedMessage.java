package org.broadinstitute.consent.http.mail.message;

import com.sendgrid.Mail;

import javax.mail.MessagingException;
import java.io.Writer;

public class FlaggedDarApprovedMessage extends MailMessage{

    /* This message is sent to the Admin when a Dataset that requires owners Approval is approved by te DAC.*/
    private final String ADMIN_APPROVED_DAR = "%s that requires data owners reviewing approved.";

    public Mail flaggedDarMessage(String toAddress, String fromAddress, Writer template, String referenceId, String type) throws MessagingException {
        return generateEmailMessage(toAddress, fromAddress, template, referenceId, type);
    }

    @Override
    String assignSubject(String referenceId, String type) {
        return String.format(ADMIN_APPROVED_DAR, referenceId);
    }
}