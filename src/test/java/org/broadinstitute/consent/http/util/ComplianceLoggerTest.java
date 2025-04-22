package org.broadinstitute.consent.http.util;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.google.api.client.http.HttpStatusCodes;
import io.netty.handler.codec.http.HttpHeaderNames;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import org.broadinstitute.consent.http.AbstractTestHelper;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.User;
import org.glassfish.jersey.server.ContainerRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;


@ExtendWith(MockitoExtension.class)
class ComplianceLoggerTest extends AbstractTestHelper {

  @Mock
  private ContainerRequest request;

  private TestAppender testAppender;

  @BeforeEach
  void setUp() {
    Logger testLogger = (Logger) LoggerFactory.getLogger(ComplianceLogger.class);
    testLogger.setLevel(Level.TRACE);
    testAppender = new TestAppender();
    testAppender.reset();
    testLogger.addAppender(testAppender);
    testAppender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
    testAppender.start();
  }

  @Test
  void testLogDARSubmission() throws Exception {
    User user = new User();
    user.setEraCommonsId("testUser");
    user.setEmail("test@test.com");
    user.setDisplayName("Test User");
    Institution institution = new Institution();
    institution.setName("testInstitution");
    user.setInstitution(institution);
    Dataset dataset = new Dataset();
    dataset.setName("testDataset");
    dataset.setAlias(randomInt(1, 1000));

    // Mock request
    when(request.getRequestUri()).thenReturn(new URI("http://example.com"));
    when(request.getHeaderString("oidc_claim_user_id")).thenReturn("testUserId");
    when(request.getHeaderString(HttpHeaderNames.USER_AGENT.toString())).thenReturn(
        "testUserAgent");

    // Log Event
    ComplianceLogger.getInstance()
        .logDARSubmission(user, List.of(dataset), request, HttpStatusCodes.STATUS_CODE_OK);

    // Verify log event
    assertEquals(1, testAppender.getSize());
    ILoggingEvent event = testAppender.getLoggedEvents().get(0);
    assertThat(event.getFormattedMessage(), containsString(user.getDisplayName()));
    assertThat(event.getFormattedMessage(), containsString(user.getEmail()));
    assertThat(event.getFormattedMessage(), containsString(user.getEraCommonsId()));
    assertThat(event.getFormattedMessage(), containsString(institution.getName()));
    assertThat(event.getFormattedMessage(), containsString(dataset.getDatasetIdentifier()));
  }

  private static class TestAppender extends ListAppender<ILoggingEvent> {

    public void reset() {
      this.list.clear();
    }

    public int getSize() {
      return this.list.size();
    }

    public List<ILoggingEvent> getLoggedEvents() {
      return Collections.unmodifiableList(this.list);
    }
  }

}
