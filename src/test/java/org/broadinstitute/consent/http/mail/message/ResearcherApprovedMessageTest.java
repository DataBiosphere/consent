package org.broadinstitute.consent.http.mail.message;

import com.sendgrid.helpers.mail.Mail;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.mail.MessagingException;
import java.io.Writer;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.MockitoAnnotations.openMocks;

public class ResearcherApprovedMessageTest {

    @Mock
    Writer template;

    @Before
    public void setUp() {
        openMocks(this);
    }

    @Test
    public void testMessageSubject() throws MessagingException {
        List<Mail> messages = new ResearcherApprovedMessage().researcherApprovedMessage("to@address.com", "from@address.com", template, "DAR-123");
        assertEquals("Your DUOS Data Access Request Results", messages.get(0).getSubject());
    }

    @Test(expected = MessagingException.class)
    public void testWithoutToAddress() throws MessagingException {
        new ResearcherApprovedMessage().researcherApprovedMessage(null, "from@address.com", template, "DAR-123");
    }

}
