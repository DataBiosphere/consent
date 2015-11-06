package org.genomebridge.consent.http.mail;

import javax.mail.Session;
import javax.mail.internet.MimeMessage;


public class MailMessage implements MailMessageAPI{

    @Override
    public MimeMessage createMessage(Session session) {
        return new MimeMessage(session);
    }
}
