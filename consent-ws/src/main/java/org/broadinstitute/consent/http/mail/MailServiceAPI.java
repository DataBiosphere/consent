package org.broadinstitute.consent.http.mail;

import freemarker.template.TemplateException;
import javax.mail.MessagingException;
import java.io.IOException;
import java.io.Writer;
import java.util.List;

public interface MailServiceAPI {

    void sendCollectMessage(String address, String referenceId, String type, Writer template) throws MessagingException;

    void sendNewCaseMessage(String userAddress, String referenceId, String type, Writer template) throws MessagingException;

    void sendReminderMessage( String address, String referenceId, String type, Writer template) throws MessagingException;

    void sendDisabledDatasetMessage( String address, String referenceId, String type, Writer template) throws MessagingException;

    void sendNewDARRequests(List<String> usersAddress, String referenceId, String type, Writer template) throws MessagingException;

    void sendCancelDARRequestMessage(List<String> usersAddress, String dataAccessRequestId, String type, Writer template) throws MessagingException;

    void sendFlaggedDarAdminApprovedMessage(String userAddress, String dataAccessRequestId, String type, Writer template) throws MessagingException;

    void sendClosedDatasetElectionsMessage(List<String> usersAddress, String dataAccessRequestId, String type, Writer template) throws MessagingException;

    void sendDelegateResponsibilitiesMessage(String userAddress, Writer template) throws MessagingException;

    void sendNewResearcherCreatedMessage(String admin, Writer template) throws IOException, TemplateException, MessagingException;
}
