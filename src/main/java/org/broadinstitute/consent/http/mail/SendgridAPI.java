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
import org.broadinstitute.consent.http.mail.message.NewCaseMessage;
import org.broadinstitute.consent.http.mail.message.NewDARRequestMessage;
import org.broadinstitute.consent.http.mail.message.NewResearcherCreatedMessage;
import org.broadinstitute.consent.http.mail.message.ReminderMessage;
import org.broadinstitute.consent.http.mail.message.ResearcherApprovedMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class SendgridAPI {

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
    private final ResearcherApprovedMessage researcherApprovedMessage = new ResearcherApprovedMessage();
    private final DataCustodianApprovalMessage dataCustodianApprovalMessage = new DataCustodianApprovalMessage();

    public SendgridAPI(MailConfiguration config) {
        this.fromAccount = config.getGoogleAccount();
        this.sendGrid = new SendGrid(config.getSendGridApiKey());
        this.activateEmailNotifications = config.isActivateEmailNotifications();
    }

    private void sendMessages(Collection<Mail> messages) {
        for (Mail message : messages) {
            sendMessage(message);
        }
    }

    private Optional<Response> sendMessage(Mail message) {
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
                return Optional.of(sendGrid.makeCall(request));
            } catch (IOException ex) {
                logger.error("Exception sending email: " + ex.getMessage());
            }
        }
        return Optional.empty();
    }

    public void sendCollectMessage(String toAddress, String referenceId, String type, Writer template) {
        List<Mail> messages = collectMessageCreator.collectMessage(toAddress, fromAccount, template, referenceId, type);
        sendMessages(messages);
    }

    public void sendNewCaseMessage(String toAddress, String referenceId, String type, Writer template) {
        List<Mail> messages = newCaseMessageCreator.newCaseMessage(toAddress, fromAccount, template, referenceId, type);
        sendMessages(messages);
    }

    public void sendReminderMessage(String toAddress, String referenceId, String type, Writer template) {
        List<Mail> messages = reminderMessageCreator.reminderMessage(toAddress, fromAccount, template, referenceId, type);
        sendMessages(messages);
    }

    public void sendDisabledDatasetMessage(String toAddress, String referenceId, String type, Writer template) {
        List<Mail> messages = disabledDatasetCreator.disabledDatasetMessage(toAddress, fromAccount, template, referenceId, type);
        sendMessages(messages);
    }

    public void sendNewDARRequests(String toAddress, String referenceId, String type, Writer template) {
        Collection<Mail> messages = newDARMessageCreator.newDARRequestMessage(toAddress, fromAccount, template, referenceId, type);
        sendMessages(messages);
    }

    public void sendCancelDARRequestMessage(String toAddress, String dataAccessRequestId, String type, Writer template) {
        Collection<Mail> messages = darCancelMessageCreator.cancelDarMessage(toAddress, fromAccount, template, dataAccessRequestId, type);
        sendMessages(messages);
    }

    public void sendClosedDatasetElectionsMessage(String toAddress, String dataAccessRequestId, String type, Writer template) {
        Collection<Mail> messages = closedDatasetElections.closedDatasetElectionMessage(toAddress, fromAccount, template, dataAccessRequestId, type);
        sendMessages(messages);
    }

    public void sendFlaggedDarAdminApprovedMessage(String toAddress, String dataAccessRequestId, String type, Writer template) {
        List<Mail> messages = adminApprovedDarMessageCreator.flaggedDarMessage(toAddress, fromAccount, template, dataAccessRequestId, type);
        sendMessages(messages);
    }

    public void sendDelegateResponsibilitiesMessage(String toAddress, Writer template) {
        List<Mail> messages = delegateResponsibilitesMessage.delegateResponsibilitiesMessage(toAddress, fromAccount, template);
        sendMessages(messages);
    }

    public void sendNewResearcherCreatedMessage(String toAddress, Writer template) {
        List<Mail> messages = researcherCreatedMessage.newResearcherCreatedMessage(toAddress, fromAccount, template, "", "");
        sendMessages(messages);
    }

    public void sendNewResearcherApprovedMessage(String toAddress, Writer template, String darCode) {
        Collection<Mail> messages = researcherApprovedMessage.researcherApprovedMessage(toAddress, fromAccount, template, darCode);
        sendMessages(messages);
    }

    public void sendDataCustodianApprovalMessage(String toAddress, String darCode, Writer template) {
        Collection<Mail> messages = dataCustodianApprovalMessage.dataCustodianApprovalMessage(toAddress, fromAccount, darCode, template);
        sendMessages(messages);
    }

}
