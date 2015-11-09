package org.genomebridge.consent.http.mail;

import javax.mail.Session;
import javax.mail.internet.MimeMessage;

public interface MailMessageAPI {

    MimeMessage createMessage(Session session);
}
