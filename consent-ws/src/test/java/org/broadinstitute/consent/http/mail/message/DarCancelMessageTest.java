package org.broadinstitute.consent.http.mail.message;

import com.sendgrid.Mail;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.mail.MessagingException;
import java.io.Writer;
import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.assertTrue;

public class DarCancelMessageTest {

    @Mock
    Writer template;

    @Before
    public void setUp(){
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testMessageSubject() throws MessagingException {
        Collection<Mail> messages = new DarCancelMessage().cancelDarMessage(Collections.singletonList("to@address.com"), "from@address.com", template, "DAR-123", "Data Access");
        for (Mail message: messages) {
            assertTrue(message.getSubject().equals("The Data Access Request with ID DAR-123 has been cancelled."));
        }
    }

}