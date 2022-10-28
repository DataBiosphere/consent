package org.broadinstitute.consent.http.mail.message;

import com.sendgrid.helpers.mail.Mail;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.mail.MessagingException;
import java.io.Writer;

import static org.junit.Assert.assertEquals;
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
        Mail message = new DarCancelMessage().cancelDarMessage("to@address.com", "from@address.com", template, "DAR-123", "Data Access");
        assertEquals("The Data Access Request with ID DAR-123 has been cancelled.", message.getSubject());
    }

}