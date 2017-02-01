package org.broadinstitute.consent.http.mail.message;

import com.sendgrid.Mail;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.mail.MessagingException;
import java.io.Writer;

import static org.junit.Assert.assertTrue;

public class ReminderMessageTest {

    @Mock
    Writer template;

    @Before
    public void setUp(){
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testMessageSubject() throws MessagingException {
        Mail message = new ReminderMessage().reminderMessage("to@address.com", "from@address.com", template, "DUL-123", "Data Use Limitations");
        assertTrue(message.getSubject().equals("Urgent: Log vote on Data Use Limitations case id: DUL-123."));
        message = new ReminderMessage().reminderMessage("to@address.com", "from@address.com", template, "DAR-123", "Data Access Request");
        assertTrue(message.getSubject().equals("Urgent: Log votes on Data Access Request case id: DAR-123."));
        message = new ReminderMessage().reminderMessage("to@address.com", "from@address.com", template, "RP-123", "Research Purpose");
        assertTrue(message.getSubject().equals("Urgent: Log votes on Research Purpose Review case id: RP-123."));
    }

}