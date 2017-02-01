package org.broadinstitute.consent.http.mail.message;

import com.sendgrid.Mail;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.mail.MessagingException;
import java.io.Writer;
import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.assertTrue;

public class ClosedDatasetElectionMessageTest {

    @Mock
    Writer template;

    @Before
    public void setUp(){
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testMessageSubject() throws MessagingException {
        Collection<Mail> messages = new ClosedDatasetElectionMessage().closedDatasetElectionMessage(Collections.singletonList("to@address.com"), "from@address.com", template, "SomeReferenceId", "Some Type");
        for (Mail message: messages) {
            assertTrue(message.getSubject().equals("Report of closed Dataset elections."));
        }
    }

}