package org.broadinstitute.consent.http.util;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import com.google.api.client.http.HttpStatusCodes;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.apache.commons.lang3.RandomUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.RequestFailedException;
import org.broadinstitute.consent.http.WithMockServer;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.util.HttpClientUtil.SimpleResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.Delay;
import org.mockserver.verify.VerificationTimes;
import org.testcontainers.containers.MockServerContainer;

public class HttpClientUtilTest implements WithMockServer {

    private HttpClientUtil clientUtil;

    private MockServerClient mockServerClient;

    private static final MockServerContainer container = new MockServerContainer(IMAGE);

    private final String statusUrl = String.format("http://%s:%s/", container.getHost(), container.getServerPort());

    @BeforeAll
    public static void setUp() {
        container.start();
    }

    @AfterAll
    public static void tearDown() {
        container.stop();
    }

    @BeforeEach
    public void init() {
        openMocks(this);
        mockServerClient = new MockServerClient(container.getHost(), container.getServerPort());
        mockServerClient.reset();
        ServicesConfiguration configuration = new ServicesConfiguration();
        configuration.setTimeoutSeconds(1);
        clientUtil = new HttpClientUtil(configuration);
    }

    /**
     * Test that the cache works normally
     */
    @Test
    public void testGetCachedResponse_case1() {
        mockServerClient.when(request())
                .respond(response()
                        .withStatusCode(200));
        IntStream.range(3, 10).forEach(i -> {
            try {
                clientUtil.getCachedResponse(new HttpGet(statusUrl));
            } catch (Exception e) {
                Assertions.fail(e.getMessage());
            }
        });
        mockServerClient.verify(request(), VerificationTimes.exactly(1));
    }

    /**
     * Test that when the cache is expired, all calls are made to external servers
     */
    @Test
    public void testGetCachedResponse_case2() {
        ServicesConfiguration configuration = new ServicesConfiguration();
        configuration.setTimeoutSeconds(1);
        // Setting the cache to 0 effectively means no caching
        configuration.setCacheExpireMinutes(0);
        clientUtil = new HttpClientUtil(configuration);
        mockServerClient.when(request())
                .respond(response()
                        .withStatusCode(200));

        int count = RandomUtils.nextInt(5, 10);
        IntStream.range(0, count).forEach(i -> {
            try {
                clientUtil.getCachedResponse(new HttpGet(statusUrl));
            } catch (Exception e) {
                Assertions.fail(e.getMessage());
            }
        });
        mockServerClient.verify(request(), VerificationTimes.exactly(count));
    }

    @Test
    public void testGetHttpResponseUnderTimeout() throws Exception {
        mockServerClient.when(request())
                .respond(response()
                        .withStatusCode(200));
        SimpleResponse response = clientUtil.getHttpResponse(new HttpGet(statusUrl));
        Assertions.assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.code());
    }

    @Test
    public void testGetHttpResponseOverTimeout() {
        mockServerClient.when(request())
                .respond(response()
                        .withStatusCode(200)
                        .withDelay(Delay.delay(TimeUnit.SECONDS, 3)));
        try {
            clientUtil.getHttpResponse(new HttpGet(statusUrl));
            Assertions.fail("The above request should have thrown an exception");
        } catch (Exception e) {
            assertTrue(e instanceof RequestFailedException);
        }
    }

}
