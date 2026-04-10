package org.broadinstitute.consent.http;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface WithWireMock {

  Logger log = LoggerFactory.getLogger(WithWireMock.class);

  default void stop(WireMockServer server) {
    if (Objects.nonNull(server) && server.isRunning()) {
      server.stop();
    }
  }

  default String getRootUrl(WireMockServer server) {
    Objects.requireNonNull(server, "WireMockServer must not be null");
    if (!server.isRunning()) {
      throw new IllegalStateException("WireMockServer must be started before calling getRootUrl");
    }
    return server.baseUrl() + "/";
  }
}
