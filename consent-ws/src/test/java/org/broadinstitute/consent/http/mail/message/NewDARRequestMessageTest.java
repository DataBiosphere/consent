package org.broadinstitute.consent.http.mail.message;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.Writer;

import static org.junit.Assert.assertTrue;

public class NewDARRequestMessageTest extends SessionHolder{

    @Mock
    Writer template;

    @Before
    public void setUp(){
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testMessageSubject() throws MessagingException {
        MimeMessage message = new NewDARRequestMessage().newDARRequestMessage(getSession(), template, "DAR-123", "Data Use Limitations");
        assertTrue(message.getSubject().equals("Create an election for Data Access Request id: DAR-123."));
    }

}