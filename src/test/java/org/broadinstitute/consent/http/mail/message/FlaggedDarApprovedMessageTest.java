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

public class FlaggedDarApprovedMessageTest {

    @Mock
    Writer template;

    @Before
    public void setUp(){
        openMocks(this);
    }

    @Test
    public void testMessageSubject() throws MessagingException {
        List<Mail> messages = new FlaggedDarApprovedMessage().flaggedDarMessage("to@address.com", "from@address.com", template, "DS-123", "SomeType");
        assertTrue(messages.get(0).getSubject().equals("DS-123 that requires data owners reviewing approved."));
    }

}
