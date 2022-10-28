package org.broadinstitute.consent.http.mail.message;

import com.sendgrid.helpers.mail.Mail;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.mail.MessagingException;
import java.io.Writer;

import static org.junit.Assert.assertEquals;
import static org.mockito.MockitoAnnotations.openMocks;

public class ReminderMessageTest {

    @Mock
    Writer template;

    @Before
    public void setUp(){
        openMocks(this);
    }

    @Test
    public void testMessageSubject() throws MessagingException {
        Mail message = new ReminderMessage().reminderMessage("to@address.com", "from@address.com", template, "DUL-123", "Data Use Limitations");
        assertEquals("Urgent: Log vote on Data Use Limitations case id: DUL-123.", message.getSubject());
        Mail message2 = new ReminderMessage().reminderMessage("to@address.com", "from@address.com", template, "DAR-123", "Data Access Request");
        assertEquals("Urgent: Log votes on Data Access Request case id: DAR-123.", message2.getSubject());
        Mail message3 = new ReminderMessage().reminderMessage("to@address.com", "from@address.com", template, "RP-123", "Research Purpose");
        assertEquals("Urgent: Log votes on Research Purpose Review case id: RP-123.", message3.getSubject());
    }

}
