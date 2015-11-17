package org.genomebridge.consent.http.mail;

import javax.mail.MessagingException;
import java.io.Writer;
import java.util.List;

public interface MailServiceAPI {

    void sendCollectMessage(String address, String referenceId, String type, Writer template) throws MessagingException;

    void sendNewCaseMessage(String address, String referenceId, String type, Writer template) throws MessagingException;

    void sendNewCaseMessages(List<String> usersAddress, String electionType, String entityId, Writer template) throws MessagingException;

    void sendReminderMessage(String address, String referenceId, String type, Writer template) throws MessagingException;

}
