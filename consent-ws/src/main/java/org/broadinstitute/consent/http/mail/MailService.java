package org.broadinstitute.consent.http.mail;

import com.sendgrid.*;
import org.apache.log4j.Logger;
import org.broadinstitute.consent.http.configurations.MailConfiguration;
import org.broadinstitute.consent.http.mail.message.*;

import javax.mail.MessagingException;
import java.io.IOException;
import java.io.Writer;
import java.util.List;

public class MailService extends AbstractMailServiceAPI {

    private String fromAccount;
    private SendGrid sendGrid;
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
    }

    private void sendMessage(Mail message) throws MessagingException {
        try {
            Request request = new Request();
            request.setBody(message.build());
            logger().debug(request.toString());
            Response response = sendGrid.api(request);
            logger().debug("Sendgrid Response Status" + response.statusCode);
            logger().debug("Sendgrid Response Body" + response.body);
            logger().debug("Sendgrid Response Headers" + response.headers);
        } catch (IOException ex) {
            logger().error("Exception sending email: " + ex.getMessage());
            throw new MessagingException(ex.getMessage());
        }
    }

    public void sendCollectMessage(String address, String referenceId, String type, Writer template) throws MessagingException {
        Mail message = collectMessageCreator.collectMessage(address, fromAccount, template, referenceId, type);
        sendMessage(message);
    }

    public void sendNewCaseMessage(String address, String referenceId, String type, Writer template) throws MessagingException {
        Mail message = newCaseMessageCreator.newCaseMessage(address, fromAccount, template, referenceId, type);
        sendMessage(message);
    }

    public void sendReminderMessage(String address, String referenceId, String type, Writer template) throws MessagingException {
        Mail message = reminderMessageCreator.reminderMessage(address, fromAccount, template, referenceId, type);
        sendMessage(message);
    }

    @Override
    public void sendDisabledDatasetMessage(String address, String referenceId, String type, Writer template) throws MessagingException {
        Mail message = disabledDatasetCreator.disabledDatasetMessage(address, fromAccount, template, referenceId, type);
        sendMessage(message);
    }

    @Override
    public void sendNewDARRequests(List<String> usersAddress, String referenceId, String type, Writer template) throws MessagingException {
        usersAddress.forEach(
            address -> {
                try {
                    Mail message = newDARMessageCreator.newDARRequestMessage(address, fromAccount, template, referenceId, type);
                    sendMessage(message);
                } catch (MessagingException e) {
                    logger().error(e);
                }
            }
        );
    }

    @Override
    public void sendCancelDARRequestMessage(List<String> usersAddress, String dataAccessRequestId, String type, Writer template) throws MessagingException {
        usersAddress.forEach(
            address -> {
                try {
                    Mail message = darCancelMessageCreator.cancelDarMessage(address, fromAccount, template, dataAccessRequestId, type);
                    sendMessage(message);
                } catch (MessagingException e) {
                    logger().error(e);
                }
            }
        );
    }

    @Override
    public void sendClosedDatasetElectionsMessage(List<String> usersAddress, String dataAccessRequestId, String type, Writer template) throws MessagingException {
        usersAddress.forEach(
            address -> {
                try {
                    Mail message = closedDatasetElections.closedDatasetElectionMessgae(address, fromAccount, template, dataAccessRequestId, type);
                    sendMessage(message);
                } catch (MessagingException e) {
                    logger().error(e);
                }
            }
        );
    }

    @Override
    public void sendFlaggedDarAdminApprovedMessage(String address, String dataAccessRequestId, String type, Writer template) throws MessagingException {
        Mail message = adminApprovedDarMessageCreator.flaggedDarMessage(address, fromAccount, template, dataAccessRequestId, type);
        sendMessage(message);
    }

    @Override
    public void sendDelegateResponsibilitiesMessage(String address, Writer template) throws MessagingException {
        Mail message = delegateResponsibilitesMessage.delegateResponsibilitiesMessage(address, fromAccount, template);
        sendMessage(message);
    }

    @Override
    public void sendNewResearcherCreatedMessage(String address, Writer template) throws MessagingException {
        Mail message = researcherCreatedMessage.newResearcherCreatedMessage(address, fromAccount, template, "", "");
        sendMessage(message);
    }

}