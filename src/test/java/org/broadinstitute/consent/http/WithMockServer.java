package org.broadinstitute.consent.http;

import org.apache.commons.lang3.StringUtils;
import org.mockserver.configuration.ConfigurationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;
import java.util.logging.LogManager;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.mockserver.configuration.ConfigurationProperties.javaLoggerLogLevel;

public interface WithMockServer {

  Logger log = LoggerFactory.getLogger(WithMockServer.class);
  DockerImageName IMAGE =
      DockerImageName.parse("mockserver/mockserver:mockserver-" + getMockServerVersion());

  default void stop(MockServerContainer container) {
    if (Objects.nonNull(container) && container.isRunning()) {
      container.stop();
    }
  }

  default String getRootUrl(MockServerContainer container) {
    return container.getEndpoint() + "/";
  }

  /**
   * Call this method to log requests/responses. It adds a good amount of non-mock-server related
   * logging which can be ignored.
   */
  @SuppressWarnings("unused")
  default void setDebugLogging() {
    try {
      ConfigurationProperties.logLevel(Level.DEBUG.name());
      String loggingConfiguration =
          "handlers=org.mockserver.logging.StandardOutConsoleHandler\n"
              + "org.mockserver.logging.StandardOutConsoleHandler.level=ALL\n"
              + "org.mockserver.logging.StandardOutConsoleHandler.formatter=java.util.logging.SimpleFormatter\n"
              + "java.util.logging.SimpleFormatter.format=%1$tF %1$tT  %3$s  %4$s  %5$s %6$s%n\n"
              + ".level="
              + javaLoggerLogLevel()
              + "\n"
              + "io.netty.handler.ssl.SslHandler.level=WARNING";
      LogManager.getLogManager()
          .readConfiguration(new ByteArrayInputStream(loggingConfiguration.getBytes(UTF_8)));
    } catch (IOException ignore) {
      //
    }
  }

  static String getMockServerVersion() {
    String version = "5.12.0";
    String versionPropName = "mockserver.version";
    try (InputStream is = WithMockServer.class.getResourceAsStream("/mvn.properties")) {
      Properties p = new Properties();
      p.load(is);
      if (StringUtils.isNotEmpty(p.getProperty(versionPropName))) {
        version = p.getProperty(versionPropName);
      } else {
        log.warn(versionPropName + " is not configured correctly, defaulting to: " + version);
      }
    } catch (Exception e) {
      log.error(e.getMessage());
      log.warn("Defaulting to: " + version);
    }
    return version;
  }
}
