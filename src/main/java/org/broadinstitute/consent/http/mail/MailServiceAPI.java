package org.broadinstitute.consent.http.mail;

import freemarker.template.TemplateException;
import javax.mail.MessagingException;
import java.io.IOException;
import java.io.Writer;
import java.util.Set;

public interface MailServiceAPI {

    void sendCollectMessage(Set<String> addresses, String referenceId, String type, Writer template) throws MessagingException;

    void sendNewCaseMessage(Set<String> userAddress, String referenceId, String type, Writer template) throws MessagingException;

    void sendReminderMessage(Set<String> addresses, String referenceId, String type, Writer template) throws MessagingException;

    void sendDisabledDatasetMessage(Set<String> addresses, String referenceId, String type, Writer template) throws MessagingException;

    void sendNewDARRequests(Set<String> usersAddress, String referenceId, String type, Writer template) throws MessagingException;

    void sendCancelDARRequestMessage(Set<String> usersAddress, String dataAccessRequestId, String type, Writer template) throws MessagingException;

    void sendFlaggedDarAdminApprovedMessage(Set<String> userAddresses, String dataAccessRequestId, String type, Writer template) throws MessagingException;

    void sendClosedDatasetElectionsMessage(Set<String> usersAddress, String dataAccessRequestId, String type, Writer template) throws MessagingException;

    void sendDelegateResponsibilitiesMessage(Set<String> userAddresses, Writer template) throws MessagingException;

    void sendNewResearcherCreatedMessage(Set<String> admin, Writer template) throws IOException, TemplateException, MessagingException;

    void sendNewHelpReportMessage(Set<String> usersAddress, Writer template, String username) throws MessagingException;

    void sendNewResearcherApprovedMessage(Set<String> researcherEmails, Writer template, String darCode) throws MessagingException;

}
