package org.broadinstitute.consent.http.mail.message;

import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import java.util.Properties;

public abstract class SessionHolder {

    final String username = "username";

    public Session getSession(){
        Properties props = new Properties();
        props.put("mail.smtp.auth", "false");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "host");
        props.put("mail.smtp.port", "25");

        Session session = Session.getInstance(props,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, username);
                    }
                });
        return session;
    }

}
