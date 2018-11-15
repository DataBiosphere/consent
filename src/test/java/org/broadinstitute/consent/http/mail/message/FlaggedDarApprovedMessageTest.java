package org.broadinstitute.consent.http.mail.message;

import com.sendgrid.Mail;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.mail.MessagingException;
import java.io.Writer;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class FlaggedDarApprovedMessageTest {

    @Mock
    Writer template;

    @Before
    public void setUp(){
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testMessageSubject() throws MessagingException {
        List<Mail> messages = new FlaggedDarApprovedMessage().flaggedDarMessage(Collections.singleton(("to@address.com")), "from@address.com", template, "DS-123", "SomeType");
        assertTrue(messages.get(0).getSubject().equals("DS-123 that requires data owners reviewing approved."));
    }

}
