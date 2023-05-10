package org.broadinstitute.consent.http.mail.message;

import static org.junit.Assert.assertEquals;
import static org.mockito.MockitoAnnotations.openMocks;

import com.sendgrid.helpers.mail.Mail;
import java.io.Writer;
import javax.mail.MessagingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

public class ResearcherApprovedMessageTest {

    @Mock
    Writer template;

    @BeforeEach
    public void setUp() {
        openMocks(this);
    }

    @Test
    public void testMessageSubject() throws MessagingException {
        Mail message = new ResearcherApprovedMessage().researcherApprovedMessage("to@address.com", "from@address.com", template, "DAR-123");
        assertEquals("Your DUOS Data Access Request Results", message.getSubject());
    }

}
