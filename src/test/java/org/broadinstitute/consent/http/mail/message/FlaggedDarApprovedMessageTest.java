package org.broadinstitute.consent.http.mail.message;

import com.sendgrid.helpers.mail.Mail;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.mail.MessagingException;
import java.io.Writer;

import static org.junit.Assert.assertEquals;
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
        Mail message = new FlaggedDarApprovedMessage().flaggedDarMessage("to@address.com", "from@address.com", template, "DS-123", "SomeType");
        assertEquals("DS-123 that requires data owners reviewing approved.", message.getSubject());
    }

}
