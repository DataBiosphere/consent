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

public class NewDARRequestMessageTest {

    @Mock
    Writer template;

    @Before
    public void setUp(){
        openMocks(this);
    }

    @Test
    public void testMessageSubject() throws MessagingException {
        Collection<Mail> messages = new NewDARRequestMessage().newDARRequestMessage("to@address.com", "from@address.com", template, "DAR-123", "Data Use Limitations");
        for (Mail message: messages) {
            assertTrue(message.getSubject().equals("Create an election for Data Access Request id: DAR-123."));
        }
    }

}