package org.broadinstitute.consent.http.mail.message;

import com.sendgrid.Mail;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.Writer;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

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
        List<Mail> messages = new NewCaseMessage().newCaseMessage(new HashSet<>(Collections.singletonList("to@address.com")), "from@address.com", template, "DUL-123", "Data Use Limitations");
        assertTrue(messages.get(0).getSubject().equals("Log vote on Data Use Limitations case id: DUL-123."));
        messages = new NewCaseMessage().newCaseMessage(new HashSet<>(Collections.singletonList("to@address.com")), "from@address.com", template, "DAR-123", "Data Access");
        assertTrue(messages.get(0).getSubject().equals("Log votes on Data Access Request case id: DAR-123."));
    }

}
