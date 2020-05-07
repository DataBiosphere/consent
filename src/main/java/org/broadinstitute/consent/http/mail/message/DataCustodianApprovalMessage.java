package org.broadinstitute.consent.http.mail.message;

import com.sendgrid.helpers.mail.Mail;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataSet;

import javax.mail.MessagingException;
import java.io.Writer;
import java.util.List;
import java.util.Set;

public class DataCustodianApprovalMessage extends MailMessage {

    /* This message is sent to the Dataset Custodian when a DAR is approved by te DAC.*/
    private final String DAC_APPROVED_DAR = "%s has been approved by the DAC.";

    public List<Mail> dataCustodianApprovalMessage(
            Set<String> toAddress,
            String fromAddress,
            Writer template,
            DataAccessRequest dataAccessRequest,
            List<DataSet> datasets,
            String userName) throws MessagingException {
        return generateEmailMessages(toAddress, fromAddress, template, dataAccessRequest.getData().getDarCode(), userName);
    }

    @Override
    String assignSubject(String darCode, String type) {
        return String.format("%s has been approved by the DAC.", darCode);
    }

}
