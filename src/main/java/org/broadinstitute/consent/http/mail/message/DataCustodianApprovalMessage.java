package org.broadinstitute.consent.http.mail.message;

import com.sendgrid.helpers.mail.Mail;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.dto.DatasetMailDTO;

import javax.mail.MessagingException;
import java.io.Writer;
import java.util.Collections;
import java.util.List;
import java.util.Set;

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
