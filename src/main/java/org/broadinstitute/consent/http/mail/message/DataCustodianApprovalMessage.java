package org.broadinstitute.consent.http.mail.message;

import com.sendgrid.helpers.mail.Mail;
import java.io.Writer;

public class DataCustodianApprovalMessage extends MailMessage {

    public Mail dataCustodianApprovalMessage(
            String toAddress,
            String fromAddress,
            String darCode,
            Writer template) {
        return generateEmailMessage(toAddress, fromAddress, template, darCode, null);
    }

    @Override
    String assignSubject(String darCode, String type) {
        return String.format("%s has been approved by the DAC", darCode);
    }

}
