package org.broadinstitute.consent.http.health;

import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.broadinstitute.consent.http.configurations.MailConfiguration;
import org.broadinstitute.consent.http.util.HttpClientUtil;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class SendGridHealthCheckTest {
    @Mock
    private HttpClientUtil clientUtil;

    @Mock
    private CloseableHttpResponse response;

    @Mock
    private StatusLine statusLine;

    @Mock
    private MailConfiguration mailConfiguration;

    private SendGridHealthCheck healthCheck;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private void initHealthCheck() {
        
    }
}
