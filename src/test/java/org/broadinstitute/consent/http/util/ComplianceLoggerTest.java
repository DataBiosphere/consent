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
import jakarta.ws.rs.core.HttpHeaders;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import org.broadinstitute.consent.http.AbstractTestHelper;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.util.ComplianceLogger.ComplianceEvent;
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
  private final User user = new User();
  private final Institution institution = new Institution();
  private final Dataset dataset = new Dataset();

  @BeforeEach
  void setUp() throws URISyntaxException {
    Logger testLogger = (Logger) LoggerFactory.getLogger(ComplianceLogger.class);
    testLogger.setLevel(Level.TRACE);
    testAppender = new TestAppender();
    testAppender.reset();
    testLogger.addAppender(testAppender);
    testAppender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
    testAppender.start();
    user.setEraCommonsId("testUser");
    user.setEmail("test@test.com");
    user.setDisplayName("Test User");
    institution.setName("testInstitution");
    user.setInstitution(institution);
    dataset.setAlias(randomInt(1, 1000));
    when(request.getRequestUri()).thenReturn(new URI("http://example.com"));
    when(request.getHeaderString("oidc_claim_user_id")).thenReturn("testUserId");
    when(request.getHeaderString(HttpHeaders.USER_AGENT)).thenReturn("testUserAgent");
    when(request.getHeaderString("X-Forwarded-For")).thenReturn("1.2.3.4");
    when(request.getHeaderString("X-Forwarded-Server")).thenReturn("4.3.2.1");
  }

  private void assertMessageContainsValueFields(ILoggingEvent event) {
    assertThat(event.getFormattedMessage(), containsString(user.getDisplayName()));
    assertThat(event.getFormattedMessage(), containsString(user.getEmail()));
    assertThat(event.getFormattedMessage(), containsString(user.getEraCommonsId()));
    assertThat(event.getFormattedMessage(), containsString(institution.getName()));
    assertThat(event.getFormattedMessage(), containsString(dataset.getDatasetIdentifier()));
  }

  @Test
  void testLogDARSApproval() {
    ComplianceLogger.logDARApproval(user, List.of(dataset), request, HttpStatusCodes.STATUS_CODE_OK);
    assertEquals(1, testAppender.getSize());
    ILoggingEvent event = testAppender.getLoggedEvents().get(0);
    assertMessageContainsValueFields(event);
    assertThat(event.getFormattedMessage(), containsString(ComplianceEvent.DAR_APPROVAL.toString()));
  }

  @Test
  void testLogRadarApproval() {
    ComplianceLogger.logRadarApproval(user, List.of(dataset), request, HttpStatusCodes.STATUS_CODE_OK);
    assertEquals(1, testAppender.getSize());
    ILoggingEvent event = testAppender.getLoggedEvents().get(0);
    assertMessageContainsValueFields(event);
    assertThat(event.getFormattedMessage(), containsString(ComplianceEvent.RADAR_APPROVAL.toString()));
  }

  @Test
  void testLogDARRejection() {
    ComplianceLogger.logDARRejection(user, List.of(dataset), request, HttpStatusCodes.STATUS_CODE_OK);
    assertEquals(1, testAppender.getSize());
    ILoggingEvent event = testAppender.getLoggedEvents().get(0);
    assertMessageContainsValueFields(event);
    assertThat(event.getFormattedMessage(), containsString(ComplianceEvent.DAR_REJECTION.toString()));
  }

  @Test
  void testLogDARSubmission() {
    ComplianceLogger.logDARSubmission(user, List.of(dataset), request, HttpStatusCodes.STATUS_CODE_CREATED);
    assertEquals(1, testAppender.getSize());
    ILoggingEvent event = testAppender.getLoggedEvents().get(0);
    assertMessageContainsValueFields(event);
    assertThat(event.getFormattedMessage(), containsString(ComplianceEvent.DAR_SUBMISSION.toString()));
  }

  @Test
  void testLogDARCancellation() {
    ComplianceLogger.logDARCancellation(user, List.of(dataset), request, HttpStatusCodes.STATUS_CODE_OK);
    assertEquals(1, testAppender.getSize());
    ILoggingEvent event = testAppender.getLoggedEvents().get(0);
    assertMessageContainsValueFields(event);
    assertThat(event.getFormattedMessage(), containsString(ComplianceEvent.DAR_CANCELLATION.toString()));
  }

  @Test
  void testLogSOCloseoutApproval() {
    ComplianceLogger.logCloseoutApprovalBySigningOfficial(user, List.of(dataset), request, HttpStatusCodes.STATUS_CODE_OK);
    assertEquals(1, testAppender.getSize());
    ILoggingEvent event = testAppender.getLoggedEvents().get(0);
    assertMessageContainsValueFields(event);
    assertThat(event.getFormattedMessage(), containsString(ComplianceEvent.CLOSEOUT_SO_APPROVAL.toString()));
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
