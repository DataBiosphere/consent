package org.broadinstitute.consent.http.service;

import static org.broadinstitute.consent.http.WithMockServer.IMAGE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import com.google.api.client.http.HttpStatusCodes;
import jakarta.ws.rs.ServerErrorException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import org.broadinstitute.consent.http.AbstractTest;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.enumeration.SupportRequestType;
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
class SupportRequestServiceTest extends AbstractTest {

  private SupportRequestService service;

  private MockServerClient mockServerClient;

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
    service = new SupportRequestService(config);
  }

  @Test
  void testPostTicketToSupport() throws Exception {
    SupportTicket ticket = generateTicket();
    String expectedBody = ticket.toString().replaceAll("\\s*", "");

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
    // Ensure that we really did send a ticket object in the POST request
    String requestBody = requests[0].getBody().getValue().toString().replaceAll("\\s*", "");
    assertEquals(expectedBody, requestBody);
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
    assertThrows(ServerErrorException.class, () -> {
      service.postTicketToSupport(generateTicket());
    });
  }

  // Creates support ticket with random values
  private SupportTicket generateTicket() {
    List<SupportRequestType> types = new ArrayList<>(EnumSet.allOf(SupportRequestType.class));
    Collections.shuffle(types);
    return new SupportTicket(
        randomAlphabetic(10),
        types.get(0),
        randomAlphabetic(10),
        randomAlphabetic(10),
        randomAlphabetic(10),
        randomAlphabetic(10),
        List.of(randomAlphanumeric(10)));
  }
}
