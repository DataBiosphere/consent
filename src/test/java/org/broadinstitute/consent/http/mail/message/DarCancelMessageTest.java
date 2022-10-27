package org.broadinstitute.consent.http.mail.message;

import com.sendgrid.helpers.mail.Mail;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.mail.MessagingException;
import java.io.Writer;
import java.util.Collection;

import static org.junit.Assert.assertTrue;
import static org.mockito.MockitoAnnotations.openMocks;

public class DarCancelMessageTest {

    @Mock
    Writer template;

    @Before
    public void setUp(){
        openMocks(this);
    }

    @Test
    public void testMessageSubject() throws MessagingException {
        Collection<Mail> messages = new DarCancelMessage().cancelDarMessage("to@address.com", "from@address.com", template, "DAR-123", "Data Access");
        for (Mail message: messages) {
            assertTrue(message.getSubject().equals("The Data Access Request with ID DAR-123 has been cancelled."));
        }
    }

}