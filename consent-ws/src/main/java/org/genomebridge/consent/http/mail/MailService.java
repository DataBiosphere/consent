package org.genomebridge.consent.http.mail;

import org.genomebridge.consent.http.configurations.MailConfiguration;

import javax.mail.*;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.IOException;
import java.io.Writer;
import java.util.Properties;

public class MailService extends AbstractMailServiceAPI {

    private Properties mailServerProperties;
    private Session getMailSession;
    MailMessageAPI mailMessageService;
    MailConfiguration config;

    private final String NEWCASE_DUL = "Log vote on Data Use Limitations case id: %s.";
    private final String NEWCASE_DAR = "Log votes on Data Access Request case id: %s.";
    private final String REMINDER_DUL = "Urgent: Log vote on Data Use Limitations case id: %s.";
    private final String REMINDER_DAR = "Urgent: Log votes on Data Access Request case id: %s.";
    private final String REMINDER_RP = "Urgent: Log votes on Research Purpose Review case id: %s.";
    private final String COLLECT_DUL = "Ready for vote collection on Data Use Limitations case id: %s.";
    private final String COLLECT_DAR = "Ready for votes collection on Data Access Request case id: %s.";
    private String USERNAME;
    private String PASSWORD;

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
        mailMessageService = new MailMessage();
    }

    private void sendMessage(MimeMessage message, String address) throws MessagingException {
        message.addRecipients(Message.RecipientType.TO, address);
        Transport.send(message);
    }

    public void sendCollectMessage(String address, String referenceId, String type, Writer template) throws MessagingException {
        MimeMessage message = mailMessageService.createMessage(getMailSession);
        MimeMultipart multipart = new MimeMultipart();
        message.setSubject(assignCollectSubject(referenceId, type));
        BodyPart messageBodyPart = new MimeBodyPart();
        messageBodyPart.setContent(template.toString(), "text/html");
        multipart.addBodyPart(messageBodyPart);
        message.setContent(multipart);
        sendMessage(message, address);
    }

    public void sendNewCaseMessage(String address, String referenceId, String type, Writer template) throws MessagingException {
        MimeMessage message = mailMessageService.createMessage(getMailSession);
        MimeMultipart multipart = new MimeMultipart();
        message.setSubject(assignNewCaseSubject(referenceId, type));
        BodyPart messageBodyPart = new MimeBodyPart();
        messageBodyPart.setContent(template.toString(), "text/html");
        multipart.addBodyPart(messageBodyPart);
        message.setContent(multipart);
        sendMessage(message, address);
    }

    public void sendReminderMessage(String address, String referenceId, String type, Writer template) throws MessagingException {
        MimeMessage message = mailMessageService.createMessage(getMailSession);
        MimeMultipart multipart = new MimeMultipart();
        message.setSubject(assignReminderSubject(referenceId, type));
        BodyPart messageBodyPart = new MimeBodyPart();
        messageBodyPart.setContent(template.toString(), "text/html");
        multipart.addBodyPart(messageBodyPart);
        message.setContent(multipart);
        sendMessage(message, address);
    }

    private String assignNewCaseSubject(String referenceId, String type) throws MessagingException {
        if(type.equals("Data Use Limitations"))
            return String.format(NEWCASE_DUL, referenceId);
        else {
            return String.format(NEWCASE_DAR, referenceId);
        }
    }

    private String assignCollectSubject(String referenceId, String type) throws MessagingException {
        if(type.equals("Data Use Limitations"))
            return String.format(COLLECT_DUL, referenceId);
        else
            return String.format(COLLECT_DAR, referenceId);
    }

    private String assignReminderSubject(String referenceId, String type) throws MessagingException {
        if(type.equals("Data Use Limitations"))
            return String.format(REMINDER_DUL, referenceId);
        else {
            if (type.equals("Data Access Request")) {
                return String.format(REMINDER_DAR, referenceId);
            }
        }
        return String.format(REMINDER_RP, referenceId);
    }
}
