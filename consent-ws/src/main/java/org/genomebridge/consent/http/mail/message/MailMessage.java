package org.genomebridge.consent.http.mail.message;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.Writer;

public abstract class MailMessage{

    public MimeMessage createMessage(Session session) {
        return new MimeMessage(session);
    }

    protected MimeMessage generateEmailMessage(Session session, Writer template, String referenceId, String type) throws MessagingException {
        MimeMessage m = createMessage(session);
        MimeMultipart multipart = new MimeMultipart();
        m.setSubject(assignSubject(referenceId, type));
        BodyPart messageBodyPart = new MimeBodyPart();
        messageBodyPart.setContent(template.toString(), "text/html");
        multipart.addBodyPart(messageBodyPart);
        m.setContent(multipart);
        return m;
    }

    abstract String assignSubject(String referenceId, String type);
}