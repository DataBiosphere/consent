package org.broadinstitute.consent.http.mail.message;

import com.sendgrid.helpers.mail.Mail;

import javax.mail.MessagingException;
import java.io.Writer;
import java.util.Collections;
import java.util.List;

public class DataCustodianApprovalMessage extends MailMessage {

    public List<Mail> dataCustodianApprovalMessage(
            String toAddress,
            String fromAddress,
            String darCode,
            Writer template) throws MessagingException {
        return generateEmailMessages(Collections.singleton(toAddress), fromAddress, template, darCode, null);
    }

    @Override
    String assignSubject(String darCode, String type) {
        return String.format("%s has been approved by the DAC", darCode);
    }

}
