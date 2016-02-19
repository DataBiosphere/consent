package org.broadinstitute.consent.http.mail;

import org.broadinstitute.consent.http.configurations.MailConfiguration;
import org.broadinstitute.consent.http.mail.message.*;

import javax.mail.*;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Properties;

public class MailService extends AbstractMailServiceAPI {

    private Properties mailServerProperties;
    private Session getMailSession;
    MailConfiguration config;
    private String USERNAME;
    private String PASSWORD;
    private CollectMessage collectMessageCreator = new CollectMessage();
    private NewCaseMessage newCaseMessageCreator = new NewCaseMessage();
    private NewDARRequestMessage newDARMessageCreator = new NewDARRequestMessage();
    private ReminderMessage reminderMessageCreator = new ReminderMessage();
    private DisabledDatasetMessage disabledDatasetCreator = new DisabledDatasetMessage();
    private DarCancelMessage darCancelMessageCreator = new DarCancelMessage();
    private FlaggedDarApprovedMessage adminApprovedDarMessageCreator = new FlaggedDarApprovedMessage();
    private ClosedDatasetElectionMessage closedDatasetElections = new ClosedDatasetElectionMessage();

    public static void initInstance(MailConfiguration config) throws IOException {
        MailServiceAPIHolder.setInstance(new MailService(config));
    }

    public MailService(MailConfiguration config) throws IOException {
        mailServerProperties = System.getProperties();
        this.config = config;
        this.USERNAME = config.getGoogleAccount();
        this.PASSWORD = config.getAccountPassword();
        mailServerProperties.put("mail.smtp.host", config.getHost());
        mailServerProperties.put("mail.smtp.socketFactory.port", config.getSmtpPort());
        mailServerProperties.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        mailServerProperties.put("mail.smtp.port", config.getSmtpPort());
        mailServerProperties.put("mail.smtp.auth", config.getSmtpAuth());
        mailServerProperties.put("mail.smtp.starttls.enable", config.getSmtpStartTlsEnable());
        getMailSession = Session.getDefaultInstance(mailServerProperties,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(USERNAME, PASSWORD);
                    }
                });
    }

    private void sendMessage(MimeMessage message, String address) throws MessagingException {
        message.addRecipients(Message.RecipientType.TO, address);
        Transport.send(message);
    }

    private void sendMessages(MimeMessage message, List<String> address) throws MessagingException {
        for (String userAddress : address) {
            message.addRecipients(Message.RecipientType.BCC, userAddress);
        }
        Transport.send(message, message.getRecipients(Message.RecipientType.BCC));
    }

    public void sendCollectMessage(String address, String referenceId, String type, Writer template) throws MessagingException {
        MimeMessage message = collectMessageCreator.collectMessage(getMailSession, template, referenceId, type);
        sendMessage(message, address);
    }

    public void sendNewCaseMessage(String userAddress, String referenceId, String type, Writer template) throws MessagingException {
        MimeMessage message = newCaseMessageCreator.newCaseMessage(getMailSession, template, referenceId, type);
        sendMessage(message, userAddress);
    }

    public void sendReminderMessage(String address, String referenceId, String type, Writer template) throws MessagingException {
        MimeMessage message = reminderMessageCreator.reminderMessage(getMailSession, template, referenceId, type);
        sendMessage(message, address);
    }

    @Override
    public void sendDisabledDatasetMessage(String address, String referenceId, String type, Writer template) throws MessagingException {
        MimeMessage message = disabledDatasetCreator.disabledDatasetMessage(getMailSession, template, referenceId, type);
        sendMessage(message, address);
    }

    @Override
    public void sendNewDARRequests(List<String> usersAddress, String referenceId, String type, Writer template) throws MessagingException {
        MimeMessage message = newDARMessageCreator.newDARRequestMessage(getMailSession, template, referenceId, type);
        sendMessages(message, usersAddress);
    }

    @Override
    public void sendCancelDARRequestMessage(List<String> usersAddress, String dataAccessRequestId, String type, Writer template) throws MessagingException {
        MimeMessage message = darCancelMessageCreator.cancelDarMessage(getMailSession, template, dataAccessRequestId, type);
        sendMessages(message, usersAddress);
    }

    @Override
    public void sendClosedDatasetElectionsMessage(List<String> usersAddress, String dataAccessRequestId, String type, Writer template) throws MessagingException {
        MimeMessage message = closedDatasetElections.closedDatasetElectionMessgae(getMailSession, template, dataAccessRequestId, type);
        sendMessages(message, usersAddress);
    }

    @Override
    public void sendFlaggedDarAdminApprovedMessage(String userAddress, String dataAccessRequestId, String type, Writer template) throws MessagingException {
        MimeMessage message = adminApprovedDarMessageCreator.flaggedDarMessage(getMailSession, template, dataAccessRequestId, type);
        sendMessage(message, userAddress);
    }

}