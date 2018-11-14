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
import java.util.Set;

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
    private HelpReportMessage helpReportMessage = new HelpReportMessage();

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

    @Override
    public void sendDisabledDatasetMessage(Set<String> toAddresses, String referenceId, String type, Writer template) throws MessagingException {
        List<Mail> messages = disabledDatasetCreator.disabledDatasetMessage(toAddresses, fromAccount, template, referenceId, type);
        sendMessages(messages);
    }

    @Override
    public void sendNewDARRequests(Set<String> toAddresses, String referenceId, String type, Writer template) throws MessagingException {
        Collection<Mail> messages = newDARMessageCreator.newDARRequestMessage(toAddresses, fromAccount, template, referenceId, type);
        sendMessages(messages);
    }

    @Override
    public void sendCancelDARRequestMessage(Set<String> toAddresses, String dataAccessRequestId, String type, Writer template) throws MessagingException {
        Collection<Mail> messages = darCancelMessageCreator.cancelDarMessage(toAddresses, fromAccount, template, dataAccessRequestId, type);
        sendMessages(messages);
    }

    @Override
    public void sendClosedDatasetElectionsMessage(Set<String> toAddresses, String dataAccessRequestId, String type, Writer template) throws MessagingException {
        Collection<Mail> messages = closedDatasetElections.closedDatasetElectionMessage(toAddresses, fromAccount, template, dataAccessRequestId, type);
        sendMessages(messages);
    }

    @Override
    public void sendFlaggedDarAdminApprovedMessage(Set<String> toAddresses, String dataAccessRequestId, String type, Writer template) throws MessagingException {
        List<Mail> messages = adminApprovedDarMessageCreator.flaggedDarMessage(toAddresses, fromAccount, template, dataAccessRequestId, type);
        sendMessages(messages);
    }

    @Override
    public void sendDelegateResponsibilitiesMessage(Set<String> userAddresses, Writer template) throws MessagingException {
        List<Mail> messages = delegateResponsibilitesMessage.delegateResponsibilitiesMessage(userAddresses, fromAccount, template);
        sendMessages(messages);
    }

    @Override
    public void sendNewResearcherCreatedMessage(Set<String> toAddresses, Writer template) throws MessagingException {
        List<Mail> messages = researcherCreatedMessage.newResearcherCreatedMessage(toAddresses, fromAccount, template, "", "");
        sendMessages(messages);
    }

    @Override
    public void sendNewHelpReportMessage(Set<String> usersAddress,  Writer template, String username) throws MessagingException {
        Collection<Mail> messages = helpReportMessage.newHelpReportMessage(usersAddress, fromAccount, template, username);
        sendMessages(messages);
    }

}