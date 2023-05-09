package org.broadinstitute.consent.http.mail.message;

import com.sendgrid.helpers.mail.Mail;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.mail.MessagingException;
import java.io.Writer;

import static org.junit.Assert.assertEquals;
import static org.mockito.MockitoAnnotations.openMocks;

public class DisabledDatasetMessageTest {

    @Mock
    Writer template;

    @Before
    public void setUp() {
        openMocks(this);
    }

    @Test
    public void testMessageSubject() throws MessagingException {
        Mail message = new DisabledDatasetMessage().disabledDatasetMessage("to@address.com", "from@address.com", template, "DAR-123", "SomeType");
        assertEquals("Datasets not available for Data Access Request Application id: DAR-123.", message.getSubject());
    }

}
