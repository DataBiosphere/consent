package org.broadinstitute.consent.http.mail.message;

import com.sendgrid.Mail;

import javax.mail.MessagingException;
import java.io.Writer;
import java.util.List;

public class CollectMessage extends MailMessage {

    private final String COLLECT_DUL = "Ready for vote collection on Data Use Limitations case id: %s.";
    private final String COLLECT_DAR = "Ready for votes collection on Data Access Request case id: %s.";

    public List<Mail> collectMessage(List<String> toAddresses, String fromAddress, Writer template, String referenceId, String type) throws MessagingException {
        return generateEmailMessage(toAddresses, fromAddress, template, referenceId, type);
    }

    @Override
    String assignSubject(String referenceId, String type) {
        if(type.equals("Data Use Limitations"))
            return String.format(COLLECT_DUL, referenceId);
        else
            return String.format(COLLECT_DAR, referenceId);
    }

}
