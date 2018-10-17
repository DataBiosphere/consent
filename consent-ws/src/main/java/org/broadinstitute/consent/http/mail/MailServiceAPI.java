package org.broadinstitute.consent.http.mail;

import freemarker.template.TemplateException;
import javax.mail.MessagingException;
import java.io.IOException;
import java.io.Writer;
import java.util.List;

public interface MailServiceAPI {

    void sendCollectMessage(List<String> addresses, String referenceId, String type, Writer template) throws MessagingException;

    void sendNewCaseMessage(List<String> userAddress, String referenceId, String type, Writer template) throws MessagingException;

    void sendReminderMessage(List<String> addresses, String referenceId, String type, Writer template) throws MessagingException;

    void sendDisabledDatasetMessage(List<String> addresses, String referenceId, String type, Writer template) throws MessagingException;

    void sendNewDARRequests(List<String> usersAddress, String referenceId, String type, Writer template) throws MessagingException;

    void sendCancelDARRequestMessage(List<String> usersAddress, String dataAccessRequestId, String type, Writer template) throws MessagingException;

    void sendFlaggedDarAdminApprovedMessage(List<String> userAddresses, String dataAccessRequestId, String type, Writer template) throws MessagingException;

    void sendClosedDatasetElectionsMessage(List<String> usersAddress, String dataAccessRequestId, String type, Writer template) throws MessagingException;

    void sendDelegateResponsibilitiesMessage(List<String> userAddresses, Writer template) throws MessagingException;

    void sendNewResearcherCreatedMessage(List<String> admin, Writer template) throws IOException, TemplateException, MessagingException;

    void sendNewHelpReportMessage(List<String> usersAddress, Writer template, String username) throws MessagingException;

}
