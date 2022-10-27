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

public class DisabledDatasetMessageTest {

    @Mock
    Writer template;

    @Before
    public void setUp(){
        openMocks(this);
    }

    @Test
    public void testMessageSubject() throws MessagingException {
        List<Mail> messages = new DisabledDatasetMessage().disabledDatasetMessage("to@address.com", "from@address.com", template, "DAR-123", "SomeType");
        assertTrue(messages.get(0).getSubject().equals("Datasets not available for Data Access Request Application id: DAR-123."));
    }

}
