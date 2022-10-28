package org.broadinstitute.consent.http.mail.message;

import com.sendgrid.helpers.mail.Mail;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.mail.MessagingException;
import java.io.Writer;

import static org.junit.Assert.assertEquals;
import static org.mockito.MockitoAnnotations.openMocks;

public class NewCaseMessageTest {

    @Mock
    Writer template;

    @Before
    public void setUp(){
        openMocks(this);
    }

    @Test
    public void testMessageSubject() throws MessagingException {
        Mail message = new NewCaseMessage().newCaseMessage("to@address.com", "from@address.com", template, "DUL-123", "Data Use Limitations");
        assertEquals("Log vote on Data Use Limitations case id: DUL-123.", message.getSubject());
        Mail message2 = new NewCaseMessage().newCaseMessage("to@address.com", "from@address.com", template, "DAR-123", "Data Access");
        assertEquals("Log votes on Data Access Request case id: DAR-123.", message2.getSubject());
    }

}
