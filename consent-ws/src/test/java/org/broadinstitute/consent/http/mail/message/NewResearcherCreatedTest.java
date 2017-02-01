package org.broadinstitute.consent.http.mail.message;

import com.sendgrid.Mail;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.mail.MessagingException;
import java.io.Writer;

import static org.junit.Assert.assertTrue;

public class NewResearcherCreatedTest {

    @Mock
    Writer template;

    @Before
    public void setUp(){
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testMessageSubject() throws MessagingException {
        Mail message = new NewResearcherCreatedMessage().newResearcherCreatedMessage("to@address.com", "from@address.com", template, "SomeReferenceId", "Some Type") ;
        assertTrue(message.getSubject().equals("Review Researcher Profile."));
    }
}
