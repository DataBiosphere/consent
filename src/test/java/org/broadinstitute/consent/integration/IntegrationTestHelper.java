package org.broadinstitute.consent.integration;

import java.nio.charset.Charset;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import org.apache.commons.io.IOUtils;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.broadinstitute.consent.http.util.HttpClientUtil.SimpleResponse;

public interface IntegrationTestHelper {

  /**
   * Integration tests can pass in an alternative url to test against. By default, we'll test
   * against develop.
   *
   * @return Base URL string: `baseUrl`
   */
  default String getBaseUrl() {
    String baseUrl = System.getenv("baseUrl");
    return Optional.ofNullable(baseUrl)
        .filter(Predicate.not(String::isBlank))
        .orElse("https://consent.dsde-dev.broadinstitute.org/");
  }

  int poolSize = 5;

  long delay = 30;

  default SimpleResponse fetchGetResponse(String path) throws Exception {
    HttpClient client = HttpClients.createDefault();
    HttpGet request = new HttpGet(getBaseUrl() + path);
    final ScheduledExecutorService executor = Executors.newScheduledThreadPool(poolSize);
    executor.schedule(request::cancel, delay, TimeUnit.SECONDS);
    return client.execute(request, httpResponse ->
        new SimpleResponse(
            httpResponse.getCode(),
            IOUtils.toString(httpResponse.getEntity().getContent(), Charset.defaultCharset())));
  }

}
