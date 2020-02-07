package org.broadinstitute.consent.http.mail.message;

import com.sendgrid.helpers.mail.Mail;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.mail.MessagingException;
import java.io.Writer;
import java.util.Collections;
import java.util.List;

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
        List<Mail> messages = new DelegateResponsibilitiesMessage().delegateResponsibilitiesMessage(Collections.singleton("to@address.com"), "from@address.com", template);
        assertTrue(messages.get(0).getSubject().equals("You have been assigned a New Role in DUOS."));
    }

}
