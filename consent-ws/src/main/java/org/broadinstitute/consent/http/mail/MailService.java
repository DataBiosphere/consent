package org.broadinstitute.consent.http.mail;

import org.apache.log4j.Logger;
import org.broadinstitute.consent.http.configurations.MailConfiguration;
import org.broadinstitute.consent.http.mail.message.ClosedDatasetElectionMessage;
import org.broadinstitute.consent.http.mail.message.CollectMessage;
import org.broadinstitute.consent.http.mail.message.DarCancelMessage;
import org.broadinstitute.consent.http.mail.message.DelegateResponsibilitiesMessage;
import org.broadinstitute.consent.http.mail.message.DisabledDatasetMessage;
import org.broadinstitute.consent.http.mail.message.FlaggedDarApprovedMessage;
import org.broadinstitute.consent.http.mail.message.NewCaseMessage;
import org.broadinstitute.consent.http.mail.message.NewDARRequestMessage;
import org.broadinstitute.consent.http.mail.message.NewResearcherCreatedMessage;
import org.broadinstitute.consent.http.mail.message.ReminderMessage;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
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
    private DelegateResponsibilitiesMessage delegateResponsibilitesMessage = new DelegateResponsibilitiesMessage();
    private NewResearcherCreatedMessage researcherCreatedMessage = new NewResearcherCreatedMessage();

    private Logger logger() {
        return Logger.getLogger("MailService");
    }

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
        logger().info("mail.smtp.host: " + mailServerProperties.get("mail.smtp.host"));
        logger().info("mail.smtp.socketFactory.port: " + mailServerProperties.get("mail.smtp.socketFactory.port"));
        logger().info("mail.smtp.socketFactory.class: " + mailServerProperties.get("mail.smtp.socketFactory.class"));
        logger().info("mail.smtp.port: " + mailServerProperties.get("mail.smtp.port"));
        logger().info("mail.smtp.auth: " + mailServerProperties.get("mail.smtp.auth"));
        logger().info("mail.smtp.starttls.enable: " + mailServerProperties.get("mail.smtp.starttls.enable"));
        getMailSession = Session.getDefaultInstance(mailServerProperties,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(USERNAME, PASSWORD);
                    }
                });
        getMailSession.setDebug(true);
        logger().info("getMailSession: " + getMailSession.getProperties());
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

    @Override
    public void sendDelegateResponsibilitiesMessage(String userAddress, Writer template) throws MessagingException {
        MimeMessage message = delegateResponsibilitesMessage.delegateResponsibilitiesMessage(getMailSession, template);
        sendMessage(message, userAddress);
    }

    @Override
    public void sendNewResearcherCreatedMessage(String userAddress, Writer template) throws MessagingException {
        MimeMessage message = researcherCreatedMessage.newResearcherCreatedMessage(getMailSession, template, "", "");
        sendMessage(message, userAddress);
    }

}