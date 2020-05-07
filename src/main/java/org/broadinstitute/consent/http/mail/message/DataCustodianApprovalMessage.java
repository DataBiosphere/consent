package org.broadinstitute.consent.http.mail.message;

import com.sendgrid.helpers.mail.Mail;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.dto.DatasetMailDTO;

import javax.mail.MessagingException;
import java.io.Writer;
import java.util.List;
import java.util.Set;

public class DataCustodianApprovalMessage extends MailMessage {

    public List<Mail> dataCustodianApprovalMessage(
            Set<String> toAddress,
            String fromAddress,
            Writer template,
            DataAccessRequest dataAccessRequest) throws MessagingException {
        return generateEmailMessages(toAddress, fromAddress, template, dataAccessRequest.getData().getDarCode(), "");
    }

    @Override
    String assignSubject(String darCode, String type) {
        return String.format("%s has been approved by the DAC.", darCode);
    }

}
