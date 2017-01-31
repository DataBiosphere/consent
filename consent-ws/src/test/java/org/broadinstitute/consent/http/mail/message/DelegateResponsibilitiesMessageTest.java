package org.broadinstitute.consent.http.mail.message;

import com.sendgrid.Mail;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.mail.MessagingException;
import java.io.Writer;

import static org.junit.Assert.assertTrue;

public class DelegateResponsibilitiesMessageTest {

    @Mock
    Writer template;

    @Before
    public void setUp(){
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testMessageSubject() throws MessagingException {
        Mail message = new DelegateResponsibilitiesMessage().delegateResponsibilitiesMessage("to@address.com", "from@address.com", template);
        assertTrue(message.getSubject().equals("You have been assigned a New Role in DUOS."));
    }

}