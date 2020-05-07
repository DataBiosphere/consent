package org.broadinstitute.consent.http.mail.message;

import com.sendgrid.helpers.mail.Mail;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataSet;

import javax.mail.MessagingException;
import java.io.Writer;
import java.util.List;
import java.util.Set;

public class DataCustodianApprovalMessage extends MailMessage {

    public List<Mail> dataCustodianApprovalMessage(
            Set<String> toAddress,
            String fromAddress,
            Writer template,
            DataAccessRequest dataAccessRequest,
            List<DataSet> datasets,
            String userName) throws MessagingException {
        return generateEmailMessages(toAddress, fromAddress, template, null, userName);
    }

    @Override
    String assignSubject(String referenceId, String type) {
        return null;
    }
}
