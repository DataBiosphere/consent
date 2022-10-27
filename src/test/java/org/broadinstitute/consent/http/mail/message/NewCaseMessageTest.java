package org.broadinstitute.consent.http.mail.message;

import com.sendgrid.helpers.mail.Mail;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.mail.MessagingException;
import java.io.Writer;
import java.util.List;

import static org.junit.Assert.assertTrue;
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
        List<Mail> messages = new NewCaseMessage().newCaseMessage("to@address.com", "from@address.com", template, "DUL-123", "Data Use Limitations");
        assertTrue(messages.get(0).getSubject().equals("Log vote on Data Use Limitations case id: DUL-123."));
        messages = new NewCaseMessage().newCaseMessage("to@address.com", "from@address.com", template, "DAR-123", "Data Access");
        assertTrue(messages.get(0).getSubject().equals("Log votes on Data Access Request case id: DAR-123."));
    }

}
