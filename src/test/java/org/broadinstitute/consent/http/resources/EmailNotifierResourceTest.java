package org.broadinstitute.consent.http.resources;

import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.service.EmailService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;

public class EmailNotifierResourceTest {

    @Mock
    private EmailService emailService;

    private EmailNotifierResource resource;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        doNothing().when(emailService).sendReminderMessage(any());
        resource = new EmailNotifierResource(emailService);
    }

    @Test
    public void testResourceSuccess() {
        Response response = resource.sendReminderMessage(String.valueOf(RandomUtils.nextInt(100, 1000)));
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testResourceFailure() {
        Response response = resource.sendReminderMessage("invalidVoteId");
        assertEquals(500, response.getStatus());
    }

}
