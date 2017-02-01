package org.broadinstitute.consent.http.mail.message;

import com.sendgrid.Mail;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.Writer;

import static org.junit.Assert.assertTrue;

public class NewCaseMessageTest {

    @Mock
    Writer template;

    @Before
    public void setUp(){
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testMessageSubject() throws MessagingException {
        Mail message = new NewCaseMessage().newCaseMessage("to@address.com", "from@address.com", template, "DUL-123", "Data Use Limitations");
        assertTrue(message.getSubject().equals("Log vote on Data Use Limitations case id: DUL-123."));
        message = new NewCaseMessage().newCaseMessage("to@address.com", "from@address.com", template, "DAR-123", "Data Access");
        assertTrue(message.getSubject().equals("Log votes on Data Access Request case id: DAR-123."));
    }

}