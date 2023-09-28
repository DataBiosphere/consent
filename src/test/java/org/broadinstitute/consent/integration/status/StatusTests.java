package org.broadinstitute.consent.integration.status;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.api.client.http.HttpStatusCodes;
import java.nio.charset.Charset;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.IOUtils;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.broadinstitute.consent.http.util.HttpClientUtil.SimpleResponse;
import org.broadinstitute.consent.integration.IntegrationTestHelper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class StatusTests implements IntegrationTestHelper {

  @Tag("IntegrationTest")
  @ParameterizedTest
  @ValueSource(strings = {"liveness", "status", "version"})
  void testStatus(String path) throws Exception {
    HttpClient client = HttpClients.createDefault();
    HttpGet request = new HttpGet(getBaseUrl() + path);
    final ScheduledExecutorService executor = Executors.newScheduledThreadPool(5);
    executor.schedule(request::cancel, 30, TimeUnit.SECONDS);
    SimpleResponse response = client.execute(request, httpResponse ->
        new SimpleResponse(
            httpResponse.getCode(),
            IOUtils.toString(httpResponse.getEntity().getContent(), Charset.defaultCharset())));
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.code());
  }

}
