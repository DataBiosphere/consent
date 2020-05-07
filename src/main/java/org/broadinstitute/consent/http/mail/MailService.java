package org.broadinstitute.consent.http.mail;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import org.broadinstitute.consent.http.configurations.MailConfiguration;
import org.broadinstitute.consent.http.mail.message.ClosedDatasetElectionMessage;
import org.broadinstitute.consent.http.mail.message.CollectMessage;
import org.broadinstitute.consent.http.mail.message.DarCancelMessage;
import org.broadinstitute.consent.http.mail.message.DataCustodianApprovalMessage;
import org.broadinstitute.consent.http.mail.message.DelegateResponsibilitiesMessage;
import org.broadinstitute.consent.http.mail.message.DisabledDatasetMessage;
import org.broadinstitute.consent.http.mail.message.FlaggedDarApprovedMessage;
import org.broadinstitute.consent.http.mail.message.HelpReportMessage;
import org.broadinstitute.consent.http.mail.message.NewCaseMessage;
import org.broadinstitute.consent.http.mail.message.NewDARRequestMessage;
import org.broadinstitute.consent.http.mail.message.NewResearcherCreatedMessage;
import org.broadinstitute.consent.http.mail.message.ReminderMessage;
import org.broadinstitute.consent.http.mail.message.ResearcherApprovedMessage;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.MessagingException;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class MailService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final String fromAccount;
    private final SendGrid sendGrid;
    private final Boolean activateEmailNotifications;
    private final CollectMessage collectMessageCreator = new CollectMessage();
    private final NewCaseMessage newCaseMessageCreator = new NewCaseMessage();
    private final NewDARRequestMessage newDARMessageCreator = new NewDARRequestMessage();
    private final ReminderMessage reminderMessageCreator = new ReminderMessage();
    private final DisabledDatasetMessage disabledDatasetCreator = new DisabledDatasetMessage();
    private final DarCancelMessage darCancelMessageCreator = new DarCancelMessage();
    private final FlaggedDarApprovedMessage adminApprovedDarMessageCreator = new FlaggedDarApprovedMessage();
    private final ClosedDatasetElectionMessage closedDatasetElections = new ClosedDatasetElectionMessage();
    private final DelegateResponsibilitiesMessage delegateResponsibilitesMessage = new DelegateResponsibilitiesMessage();
    private final NewResearcherCreatedMessage researcherCreatedMessage = new NewResearcherCreatedMessage();
    private final HelpReportMessage helpReportMessage = new HelpReportMessage();
    private final ResearcherApprovedMessage researcherApprovedMessage = new ResearcherApprovedMessage();
    private final DataCustodianApprovalMessage dataCustodianApprovalMessage = new DataCustodianApprovalMessage();

    public MailService(MailConfiguration config) {
        this.fromAccount = config.getGoogleAccount();
        this.sendGrid = new SendGrid(config.getSendGridApiKey());
        this.activateEmailNotifications = config.isActivateEmailNotifications();
    }

    private void sendMessages(Collection<Mail> messages) throws MessagingException {
        for (Mail message : messages) {
            sendMessage(message);
        }
    }

    private void sendMessage(Mail message) throws MessagingException {
        if (activateEmailNotifications) {
            try {
                // See https://github.com/sendgrid/sendgrid-java/issues/163
                // for what actually works as compared to the documentation - which doesn't.
                Request request = new Request();
                request.setMethod(Method.POST);
                request.setBody(message.build());
                // make request
                request.setBaseUri(sendGrid.getHost());
                request.setEndpoint("/" + sendGrid.getVersion() + "/mail/send");
                for (String key : sendGrid.getRequestHeaders().keySet())
                    request.addHeader(key, sendGrid.getRequestHeaders().get(key));
                // send
                Response response = sendGrid.makeCall(request);
                if (response.getStatusCode() >= 400) {
                    throw new MessagingException(response.getBody());
                }
            } catch (IOException ex) {
                logger.error("Exception sending email: " + ex.getMessage());
                throw new MessagingException(ex.getMessage());
            }
        } else {
            logger.debug("Not configured to send email");
        }
    }

    public void sendCollectMessage(Set<String> toAddresses, String referenceId, String type, Writer template) throws MessagingException {
        List<Mail> messages = collectMessageCreator.collectMessage(toAddresses, fromAccount, template, referenceId, type);
        sendMessages(messages);
    }

    public void sendNewCaseMessage(Set<String> toAddress, String referenceId, String type, Writer template) throws MessagingException {
        List<Mail> messages = newCaseMessageCreator.newCaseMessage(toAddress, fromAccount, template, referenceId, type);
        sendMessages(messages);
    }

    public void sendReminderMessage(Set<String> addresses, String referenceId, String type, Writer template) throws MessagingException {
        List<Mail> messages = reminderMessageCreator.reminderMessage(addresses, fromAccount, template, referenceId, type);
        sendMessages(messages);
    }

    public void sendDisabledDatasetMessage(Set<String> toAddresses, String referenceId, String type, Writer template) throws MessagingException {
        List<Mail> messages = disabledDatasetCreator.disabledDatasetMessage(toAddresses, fromAccount, template, referenceId, type);
        sendMessages(messages);
    }

    public void sendNewDARRequests(Set<String> toAddresses, String referenceId, String type, Writer template) throws MessagingException {
        Collection<Mail> messages = newDARMessageCreator.newDARRequestMessage(toAddresses, fromAccount, template, referenceId, type);
        sendMessages(messages);
    }

    public void sendCancelDARRequestMessage(Set<String> toAddresses, String dataAccessRequestId, String type, Writer template) throws MessagingException {
        Collection<Mail> messages = darCancelMessageCreator.cancelDarMessage(toAddresses, fromAccount, template, dataAccessRequestId, type);
        sendMessages(messages);
    }

    public void sendClosedDatasetElectionsMessage(Set<String> toAddresses, String dataAccessRequestId, String type, Writer template) throws MessagingException {
        Collection<Mail> messages = closedDatasetElections.closedDatasetElectionMessage(toAddresses, fromAccount, template, dataAccessRequestId, type);
        sendMessages(messages);
    }

    public void sendFlaggedDarAdminApprovedMessage(Set<String> toAddresses, String dataAccessRequestId, String type, Writer template) throws MessagingException {
        List<Mail> messages = adminApprovedDarMessageCreator.flaggedDarMessage(toAddresses, fromAccount, template, dataAccessRequestId, type);
        sendMessages(messages);
    }

    public void sendDelegateResponsibilitiesMessage(Set<String> userAddresses, Writer template) throws MessagingException {
        List<Mail> messages = delegateResponsibilitesMessage.delegateResponsibilitiesMessage(userAddresses, fromAccount, template);
        sendMessages(messages);
    }

    public void sendNewResearcherCreatedMessage(Set<String> toAddresses, Writer template) throws MessagingException {
        List<Mail> messages = researcherCreatedMessage.newResearcherCreatedMessage(toAddresses, fromAccount, template, "", "");
        sendMessages(messages);
    }

    public void sendNewHelpReportMessage(Set<String> usersAddress, Writer template, String username) throws MessagingException {
        Collection<Mail> messages = helpReportMessage.newHelpReportMessage(usersAddress, fromAccount, template, username);
        sendMessages(messages);
    }

    public void sendNewResearcherApprovedMessage(Set<String> researcherEmails, Writer template, String darCode) throws MessagingException {
        Collection<Mail> messages = researcherApprovedMessage.researcherApprovedMessage(researcherEmails, fromAccount, template, darCode);
        sendMessages(messages);
    }

    public void sendDataCustodianApprovalMessage(String toAddress, Writer template,
                                                 String darCode) throws MessagingException {
        Collection<Mail> messages = dataCustodianApprovalMessage.dataCustodianApprovalMessage(toAddress, fromAccount, template,
                darCode);
        sendMessages(messages);
    }

}
