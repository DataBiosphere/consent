package org.broadinstitute.consent.http.mail;

import com.sendgrid.*;
import org.apache.log4j.Logger;
import org.broadinstitute.consent.http.configurations.MailConfiguration;
import org.broadinstitute.consent.http.mail.message.*;

import javax.mail.MessagingException;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.List;

public class MailService extends AbstractMailServiceAPI {

    private String fromAccount;
    private SendGrid sendGrid;
    private Boolean activateEmailNotifications;
    private CollectMessage collectMessageCreator = new CollectMessage();
    private NewCaseMessage newCaseMessageCreator = new NewCaseMessage();
    private NewDARRequestMessage newDARMessageCreator = new NewDARRequestMessage();
    private ReminderMessage reminderMessageCreator = new ReminderMessage();
    private DisabledDatasetMessage disabledDatasetCreator = new DisabledDatasetMessage();
    private DarCancelMessage darCancelMessageCreator = new DarCancelMessage();
    private FlaggedDarApprovedMessage adminApprovedDarMessageCreator = new FlaggedDarApprovedMessage();
    private ClosedDatasetElectionMessage closedDatasetElections = new ClosedDatasetElectionMessage();
    private DelegateResponsibilitiesMessage delegateResponsibilitesMessage = new DelegateResponsibilitiesMessage();
    private NewResearcherCreatedMessage researcherCreatedMessage = new NewResearcherCreatedMessage();

    private Logger logger() {
        return Logger.getLogger("MailService");
    }

    public static void initInstance(MailConfiguration config) throws IOException {
        MailServiceAPIHolder.setInstance(new MailService(config));
    }

    private MailService(MailConfiguration config) throws IOException {
        this.fromAccount = config.getGoogleAccount();
        this.sendGrid = new SendGrid(config.getSendGridApiKey());
        this.activateEmailNotifications = config.isActivateEmailNotifications();
    }

    private void sendMessages(Collection<Mail> messages) throws MessagingException {
        for (Mail message: messages) {
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
                sendGrid.makeCall(request);
            } catch (IOException ex) {
                logger().error("Exception sending email: " + ex.getMessage());
                throw new MessagingException(ex.getMessage());
            }
        } else {
            logger().debug("Not configured to send email");
        }
    }

    public void sendCollectMessage(String toAddress, String referenceId, String type, Writer template) throws MessagingException {
        Mail message = collectMessageCreator.collectMessage(toAddress, fromAccount, template, referenceId, type);
        sendMessage(message);
    }

    public void sendNewCaseMessage(String toAddress, String referenceId, String type, Writer template) throws MessagingException {
        Mail message = newCaseMessageCreator.newCaseMessage(toAddress, fromAccount, template, referenceId, type);
        sendMessage(message);
    }

    public void sendReminderMessage(String toAddress, String referenceId, String type, Writer template) throws MessagingException {
        Mail message = reminderMessageCreator.reminderMessage(toAddress, fromAccount, template, referenceId, type);
        sendMessage(message);
    }

    @Override
    public void sendDisabledDatasetMessage(String toAddress, String referenceId, String type, Writer template) throws MessagingException {
        Mail message = disabledDatasetCreator.disabledDatasetMessage(toAddress, fromAccount, template, referenceId, type);
        sendMessage(message);
    }

    @Override
    public void sendNewDARRequests(List<String> toAddresses, String referenceId, String type, Writer template) throws MessagingException {
        Collection<Mail> messages = newDARMessageCreator.newDARRequestMessage(toAddresses, fromAccount, template, referenceId, type);
        sendMessages(messages);
    }

    @Override
    public void sendCancelDARRequestMessage(List<String> toAddresses, String dataAccessRequestId, String type, Writer template) throws MessagingException {
        Collection<Mail> messages = darCancelMessageCreator.cancelDarMessage(toAddresses, fromAccount, template, dataAccessRequestId, type);
        sendMessages(messages);
    }

    @Override
    public void sendClosedDatasetElectionsMessage(List<String> toAddresses, String dataAccessRequestId, String type, Writer template) throws MessagingException {
        Collection<Mail> messages = closedDatasetElections.closedDatasetElectionMessage(toAddresses, fromAccount, template, dataAccessRequestId, type);
        sendMessages(messages);
    }

    @Override
    public void sendFlaggedDarAdminApprovedMessage(String toAddress, String dataAccessRequestId, String type, Writer template) throws MessagingException {
        Mail message = adminApprovedDarMessageCreator.flaggedDarMessage(toAddress, fromAccount, template, dataAccessRequestId, type);
        sendMessage(message);
    }

    @Override
    public void sendDelegateResponsibilitiesMessage(String toAddress, Writer template) throws MessagingException {
        Mail message = delegateResponsibilitesMessage.delegateResponsibilitiesMessage(toAddress, fromAccount, template);
        sendMessage(message);
    }

    @Override
    public void sendNewResearcherCreatedMessage(String toAddress, Writer template) throws MessagingException {
        Mail message = researcherCreatedMessage.newResearcherCreatedMessage(toAddress, fromAccount, template, "", "");
        sendMessage(message);
    }

}