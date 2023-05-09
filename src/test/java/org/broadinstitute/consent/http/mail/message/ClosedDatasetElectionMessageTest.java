package org.broadinstitute.consent.http.mail.message;

import com.sendgrid.helpers.mail.Mail;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.mail.MessagingException;
import java.io.Writer;

import static org.junit.Assert.assertEquals;
import static org.mockito.MockitoAnnotations.openMocks;

public class ClosedDatasetElectionMessageTest {

    @Mock
    Writer template;

    @Before
    public void setUp() {
        openMocks(this);
    }

    @Test
    public void testMessageSubject() throws MessagingException {
        Mail message = new ClosedDatasetElectionMessage().closedDatasetElectionMessage("to@address.com", "from@address.com", template, "SomeReferenceId", "Some Type");
        assertEquals("Report of closed Dataset elections.", message.getSubject());
    }

}