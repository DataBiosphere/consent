package org.broadinstitute.consent.http.resources;

import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.service.AbstractEmailNotifierAPI;
import org.broadinstitute.consent.http.service.EmailNotifierAPI;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
        AbstractEmailNotifierAPI.class
})
public class EmailNotifierResourceTest {

    @Mock
    private EmailNotifierAPI emailApi;

    private EmailNotifierResource resource;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(AbstractEmailNotifierAPI.class);
        when(AbstractEmailNotifierAPI.getInstance()).thenReturn(emailApi);
        doNothing().when(emailApi).sendReminderMessage(any());
        resource = new EmailNotifierResource();
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
