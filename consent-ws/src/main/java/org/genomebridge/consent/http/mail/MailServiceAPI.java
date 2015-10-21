package org.genomebridge.consent.http.mail;

import javax.mail.MessagingException;
import java.io.Writer;

public interface MailServiceAPI {

    void sendCollectMessage(String address, String referenceId, String type, Writer template) throws MessagingException;

    void sendNewCaseMessage(String address, String referenceId, String type, Writer template) throws MessagingException;

    void sendReminderMessage(String address, String referenceId, String type, Writer template) throws MessagingException;

}
