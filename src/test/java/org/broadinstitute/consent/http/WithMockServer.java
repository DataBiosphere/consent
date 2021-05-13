package org.broadinstitute.consent.http;

import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Objects;

public interface WithMockServer {

  DockerImageName IMAGE = DockerImageName.parse("mockserver/mockserver:mockserver-5.11.2");

  default void stop(MockServerContainer container) {
    if (Objects.nonNull(container) && container.isRunning()) {
      container.stop();
    }
  }

  default String getRootUrl(MockServerContainer container) {
    return container.getEndpoint() + "/";
  }
}
