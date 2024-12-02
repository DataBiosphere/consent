package org.broadinstitute.consent.http.service;

import static org.broadinstitute.consent.http.WithMockServer.IMAGE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import com.google.api.client.http.HttpStatusCodes;
import jakarta.ws.rs.ServerErrorException;
import java.util.List;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.enumeration.SupportRequestType;
import org.broadinstitute.consent.http.models.support.CustomRequestField;
import org.broadinstitute.consent.http.models.support.SupportRequestComment;
import org.broadinstitute.consent.http.models.support.SupportTicket;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.Header;
import org.mockserver.model.HttpError;
import org.mockserver.model.HttpRequest;
import org.testcontainers.containers.MockServerContainer;

@ExtendWith(MockitoExtension.class)
class SupportRequestServiceTest {

  private SupportRequestService service;

  private MockServerClient mockServerClient;

  @Mock
  private UserDAO userDAO;

  @Mock
  private ServicesConfiguration config;

  private static final MockServerContainer container = new MockServerContainer(IMAGE);

  @BeforeAll
  static void setUp() {
    container.start();
  }

  @AfterAll
  static void tearDown() {
    container.stop();
  }

  @BeforeEach
  void init() {
    mockServerClient = new MockServerClient(container.getHost(), container.getServerPort());
    mockServerClient.reset();
    service = new SupportRequestService(config, userDAO);
  }

  @Test
  void testPostTicketToSupport() throws Exception {
    SupportTicket ticket = generateTicket();
    SupportTicket.SupportRequest supportRequest = ticket.getRequest();

    //simplifying comment body and custom fields for testing request body
    supportRequest.setComment(new SupportRequestComment(RandomStringUtils.secure().nextAlphabetic(10), null));
    CustomRequestField customField = new CustomRequestField(RandomUtils.secure().randomLong(),
        RandomStringUtils.secure().nextAlphabetic(10));
    supportRequest.setCustomFields(List.of(customField));

    String expectedBody = String.format("""
            {
              "request" : {
                "requester" : {
                  "name" : "%s",
                  "email" : "%s"
                },
                "subject" : "%s",
                "custom_fields" : [ {
                  "id" : %d,
                  "value" : "%s"
                } ],
                "comment" : {
                  "body" : "%s"
                },
                "ticket_form_id" : 360000669472
              }
            }""",
        supportRequest.getRequester().getName(),
        supportRequest.getRequester().getEmail(),
        supportRequest.getSubject(),
        customField.getId(),
        customField.getValue(),
        supportRequest.getComment().body());

    when(config.isActivateSupportNotifications()).thenReturn(true);
    when(config.postSupportRequestUrl()).thenReturn(
        "http://" + container.getHost() + ":" + container.getServerPort() + "/");
    mockServerClient.when(request().withMethod("POST"))
        .respond(response()
            .withHeader(Header.header("Content-Type", "application/json"))
            .withStatusCode(HttpStatusCodes.STATUS_CODE_CREATED));
    service.postTicketToSupport(ticket);

    HttpRequest[] requests = mockServerClient.retrieveRecordedRequests(null);
    assertEquals(1, requests.length);
    Object requestBody = requests[0].getBody().getValue();
    String requestBodyNormalizedNewLines = requestBody.toString()
        .replace("\r\n", "\n")
        .replace("\r", "\n");
    assertEquals(expectedBody, requestBodyNormalizedNewLines);
  }

  @Test
  void testPostTicketToSupportNotificationsNotActivated() throws Exception {
    SupportTicket ticket = generateTicket();
    when(config.isActivateSupportNotifications()).thenReturn(false);
    // verify no requests sent if activateSupportNotifications is false; throw error if post attempted
    mockServerClient.when(request()).error(new HttpError());
    service.postTicketToSupport(ticket);
  }

  @Test
  void testPostTicketToSupportServerError() {
    when(config.isActivateSupportNotifications()).thenReturn(true);
    when(config.postSupportRequestUrl()).thenReturn(
        "http://" + container.getHost() + ":" + container.getServerPort() + "/");
    mockServerClient.when(request())
        .respond(response()
            .withHeader(Header.header("Content-Type", "application/json"))
            .withStatusCode(HttpStatusCodes.STATUS_CODE_SERVER_ERROR));
    assertThrows(ServerErrorException.class, () -> service.postTicketToSupport(generateTicket()));
  }

  //creates support ticket with random values for testing postTicketToSupport
  private SupportTicket generateTicket() {
    String requesterName = RandomStringUtils.secure().nextAlphabetic(10);
    String requesterEmail = RandomStringUtils.secure().nextAlphabetic(10);
    String subject = RandomStringUtils.secure().nextAlphabetic(10);
    String description = RandomStringUtils.secure().nextAlphabetic(10);
    String url = RandomStringUtils.secure().nextAlphabetic(10);

    return new SupportTicket(requesterName, SupportRequestType.TASK, requesterEmail, subject,
        description, url, null);
  }
}
