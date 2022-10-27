package org.broadinstitute.consent.http.mail.message;

import com.sendgrid.helpers.mail.Mail;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.mail.MessagingException;
import java.io.Writer;
import java.util.Collection;

import static org.junit.Assert.assertTrue;
import static org.mockito.MockitoAnnotations.openMocks;

public class ClosedDatasetElectionMessageTest {

    @Mock
    Writer template;

    @Before
    public void setUp(){
        openMocks(this);
    }

    @Test
    public void testMessageSubject() throws MessagingException {
        Collection<Mail> messages = new ClosedDatasetElectionMessage().closedDatasetElectionMessage("to@address.com", "from@address.com", template, "SomeReferenceId", "Some Type");
        for (Mail message: messages) {
            assertTrue(message.getSubject().equals("Report of closed Dataset elections."));
        }
    }

}