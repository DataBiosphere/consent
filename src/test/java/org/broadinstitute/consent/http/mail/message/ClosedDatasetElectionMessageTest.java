package org.broadinstitute.consent.http.mail.message;

import static org.mockito.MockitoAnnotations.openMocks;

import com.sendgrid.helpers.mail.Mail;
import java.io.Writer;
import javax.mail.MessagingException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

public class ClosedDatasetElectionMessageTest {

    @Mock
    Writer template;

    @BeforeEach
    public void setUp() {
        openMocks(this);
    }

    @Test
    public void testMessageSubject() throws MessagingException {
        Mail message = new ClosedDatasetElectionMessage().closedDatasetElectionMessage("to@address.com", "from@address.com", template, "SomeReferenceId", "Some Type");
        Assertions.assertEquals("Report of closed Dataset elections.", message.getSubject());
    }

}