package org.broadinstitute.consent.http.mail;

import com.google.api.client.http.HttpStatusCodes;
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
import org.broadinstitute.consent.http.mail.message.ReminderMessage;
import org.broadinstitute.consent.http.mail.message.ResearcherApprovedMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.Optional;

public class SendGridAPI {

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
    private final ResearcherApprovedMessage researcherApprovedMessage = new ResearcherApprovedMessage();
    private final DataCustodianApprovalMessage dataCustodianApprovalMessage = new DataCustodianApprovalMessage();

    public SendGridAPI(MailConfiguration config) {
        this.fromAccount = config.getGoogleAccount();
        this.sendGrid = new SendGrid(config.getSendGridApiKey());
        this.activateEmailNotifications = config.isActivateEmailNotifications();
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
                logger.error("Exception sending email via SendGrid: " + ex.getMessage());
                // Create a response that we can use to capture this failure.
                Response response = new Response(
                        HttpStatusCodes.STATUS_CODE_SERVER_ERROR,
                        ex.getMessage(),
                        Map.of()
                );
                return Optional.of(response);
            }
        }
        return Optional.empty();
    }

    public Optional<Response> sendCollectMessage(String toAddress, String referenceId, String type, Writer template) {
        Mail message = collectMessageCreator.collectMessage(toAddress, fromAccount, template, referenceId, type);
        return sendMessage(message);
    }

    public Optional<Response> sendNewCaseMessage(String toAddress, String referenceId, String type, Writer template) {
        Mail message = newCaseMessageCreator.newCaseMessage(toAddress, fromAccount, template, referenceId, type);
        return sendMessage(message);
    }

    public Optional<Response> sendReminderMessage(String toAddress, String referenceId, String type, Writer template) {
        Mail message = reminderMessageCreator.reminderMessage(toAddress, fromAccount, template, referenceId, type);
        return sendMessage(message);
    }

    public Optional<Response> sendDisabledDatasetMessage(String toAddress, String referenceId, String type, Writer template) {
        Mail message = disabledDatasetCreator.disabledDatasetMessage(toAddress, fromAccount, template, referenceId, type);
        return sendMessage(message);
    }

    public Optional<Response> sendNewDARRequests(String toAddress, String referenceId, String type, Writer template) {
        Mail message =  newDARMessageCreator.newDARRequestMessage(toAddress, fromAccount, template, referenceId, type);
        return sendMessage(message);
    }

    public Optional<Response> sendCancelDARRequestMessage(String toAddress, String dataAccessRequestId, String type, Writer template) {
        Mail message =  darCancelMessageCreator.cancelDarMessage(toAddress, fromAccount, template, dataAccessRequestId, type);
        return sendMessage(message);
    }

    public Optional<Response> sendClosedDatasetElectionsMessage(String toAddress, String dataAccessRequestId, String type, Writer template) {
        Mail message =  closedDatasetElections.closedDatasetElectionMessage(toAddress, fromAccount, template, dataAccessRequestId, type);
        return sendMessage(message);
    }

    public Optional<Response> sendFlaggedDarAdminApprovedMessage(String toAddress, String dataAccessRequestId, String type, Writer template) {
        Mail message = adminApprovedDarMessageCreator.flaggedDarMessage(toAddress, fromAccount, template, dataAccessRequestId, type);
        return sendMessage(message);
    }

    public Optional<Response> sendDelegateResponsibilitiesMessage(String toAddress, Writer template) {
        Mail message = delegateResponsibilitesMessage.delegateResponsibilitiesMessage(toAddress, fromAccount, template);
        return sendMessage(message);
    }
    public Optional<Response> sendNewResearcherApprovedMessage(String toAddress, Writer template, String darCode) {
        Mail message = researcherApprovedMessage.researcherApprovedMessage(toAddress, fromAccount, template, darCode);
        return sendMessage(message);
    }

    public Optional<Response> sendDataCustodianApprovalMessage(String toAddress, String darCode, Writer template) {
        Mail message = dataCustodianApprovalMessage.dataCustodianApprovalMessage(toAddress, fromAccount, darCode, template);
        return sendMessage(message);
    }

}
