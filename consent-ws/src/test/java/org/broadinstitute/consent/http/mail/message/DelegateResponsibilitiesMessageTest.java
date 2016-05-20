package org.broadinstitute.consent.http.mail.message;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.Writer;

import static org.junit.Assert.assertTrue;

public class DelegateResponsibilitiesMessageTest extends SessionHolder{

    @Mock
    Writer template;

    @Before
    public void setUp(){
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testMessageSubject() throws MessagingException {
        MimeMessage message = new DelegateResponsibilitiesMessage().delegateResponsibilitiesMessage(getSession(), template);
        assertTrue(message.getSubject().equals("You have been assigned a New Role in DUOS."));
    }

}