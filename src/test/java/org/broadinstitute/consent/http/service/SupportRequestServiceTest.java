package org.broadinstitute.consent.http.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import com.google.api.client.http.HttpStatusCodes;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ServerErrorException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import org.broadinstitute.consent.http.MockServerTestHelper;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.enumeration.SupportRequestType;
import org.broadinstitute.consent.http.exceptions.UnprocessableEntityException;
import org.broadinstitute.consent.http.models.support.DuosTicket;
import org.broadinstitute.consent.http.models.support.TicketFactory;
import org.broadinstitute.consent.http.models.support.TicketFields;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockserver.model.Header;
import org.mockserver.model.HttpError;
import org.mockserver.model.HttpRequest;
import org.mockserver.verify.VerificationTimes;

@ExtendWith(MockitoExtension.class)
class SupportRequestServiceTest extends MockServerTestHelper {

  private SupportRequestService service;
  @Mock private ServicesConfiguration config;

  @BeforeEach
  void init() {
    service = new SupportRequestService(config);
  }

  @Test
  void testPostTicketToSupport() throws Exception {
    DuosTicket ticket = generateTicket();
    String expectedBody = ticket.toString();

    when(config.isActivateSupportNotifications()).thenReturn(true);
    when(config.postSupportRequestUrl())
        .thenReturn("http://" + CONTAINER.getHost() + ":" + CONTAINER.getServerPort() + "/");
    mockServerClient
        .when(request().withMethod("POST"))
        .respond(
            response()
                .withHeader(Header.header("Content-Type", "application/json"))
                .withStatusCode(HttpStatusCodes.STATUS_CODE_CREATED)
                .withBody(expectedBody));

    service.postTicketToSupport(ticket);
    URL supportUrl = URI.create(config.postSupportRequestUrl()).toURL();
    mockServerClient.verify(request().withPath(supportUrl.getPath()).withBody(expectedBody), VerificationTimes.exactly(1));
  }

  @Test
  void testPostTicketToSupportNotificationsNotActivated() {
    DuosTicket ticket = generateTicket();
    when(config.isActivateSupportNotifications()).thenReturn(false);
    // verify no requests sent if activateSupportNotifications is false; throw error if post
    // attempted
    mockServerClient.when(request()).error(new HttpError());
    assertThrows(BadRequestException.class, () -> service.postTicketToSupport(ticket));
  }

  @Test
  void testPostTicketToSupportNotificationsUnprocessableEntity() {
    DuosTicket ticket = generateTicket();
    when(config.isActivateSupportNotifications()).thenReturn(true);
    when(config.postSupportRequestUrl())
        .thenReturn("http://" + CONTAINER.getHost() + ":" + CONTAINER.getServerPort() + "/");
    mockServerClient
        .when(request())
        .respond(
            response()
                .withHeader(Header.header("Content-Type", "application/json"))
                .withStatusCode(HttpStatusCodes.STATUS_CODE_UNPROCESSABLE_ENTITY));
    assertThrows(UnprocessableEntityException.class, () -> service.postTicketToSupport(ticket));
  }

  @Test
  void testPostTicketToSupportServerError() {
    DuosTicket ticket = generateTicket();
    when(config.isActivateSupportNotifications()).thenReturn(true);
    when(config.postSupportRequestUrl())
        .thenReturn("http://" + CONTAINER.getHost() + ":" + CONTAINER.getServerPort() + "/");
    mockServerClient
        .when(request())
        .respond(
            response()
                .withHeader(Header.header("Content-Type", "application/json"))
                .withStatusCode(HttpStatusCodes.STATUS_CODE_SERVER_ERROR));
    assertThrows(ServerErrorException.class, () -> service.postTicketToSupport(ticket));
  }

  @Test
  void testPostAttachmentToSupport() throws Exception {
    String expectedBody =
        """
        { "upload": { "token": "token string" } }
        """;
    when(config.isActivateSupportNotifications()).thenReturn(true);
    when(config.postSupportUploadUrl())
        .thenReturn("http://" + CONTAINER.getHost() + ":" + CONTAINER.getServerPort() + "/");
    mockServerClient
        .when(request().withMethod("POST"))
        .respond(
            response()
                .withHeader(Header.header("Content-Type", "application/json"))
                .withStatusCode(HttpStatusCodes.STATUS_CODE_CREATED)
                .withBody(expectedBody));
    service = new SupportRequestService(config);
    service.postAttachmentToSupport("Test".getBytes());
    URL supportUrl = URI.create(config.postSupportUploadUrl()).toURL();
    mockServerClient.verify(request().withPath(supportUrl.getPath()), VerificationTimes.exactly(1));
  }

  @Test
  void testPostTicketToSupportUnableToParseResponse() {
    // This case should never happen, but we do inspect the response for a valid "upload" object.
    // We need to ensure that the service handles invalid response formats correctly.
    String expectedBody =
        """
        { "invalid": { "missing_token": "token string" } }
        """;
    when(config.isActivateSupportNotifications()).thenReturn(true);
    when(config.postSupportUploadUrl())
        .thenReturn("http://" + CONTAINER.getHost() + ":" + CONTAINER.getServerPort() + "/");
    mockServerClient
        .when(request().withMethod("POST"))
        .respond(
            response()
                .withHeader(Header.header("Content-Type", "application/json"))
                .withStatusCode(HttpStatusCodes.STATUS_CODE_CREATED)
                .withBody(expectedBody));
    assertThrows(
        ServerErrorException.class, () -> service.postAttachmentToSupport("Test".getBytes()));
  }

  @Test
  void testPostAttachmentToSupportNotificationsNotActivated() {
    when(config.isActivateSupportNotifications()).thenReturn(false);
    // verify no requests sent if activateSupportNotifications is false; throw error if post
    // attempted
    mockServerClient.when(request()).error(new HttpError());
    assertThrows(
        BadRequestException.class, () -> service.postAttachmentToSupport("Test".getBytes()));
  }

  @Test
  void testPostAttachmentToSupportServerError() {
    when(config.isActivateSupportNotifications()).thenReturn(true);
    when(config.postSupportUploadUrl())
        .thenReturn("http://" + CONTAINER.getHost() + ":" + CONTAINER.getServerPort() + "/");
    mockServerClient
        .when(request())
        .respond(
            response()
                .withHeader(Header.header("Content-Type", "application/json"))
                .withStatusCode(HttpStatusCodes.STATUS_CODE_SERVER_ERROR));
    assertThrows(
        ServerErrorException.class, () -> service.postAttachmentToSupport("Test".getBytes()));
  }

  @Test
  void testPostAttachmentToSupportUnprocessableEntity() {
    when(config.isActivateSupportNotifications()).thenReturn(true);
    when(config.postSupportUploadUrl())
        .thenReturn("http://" + CONTAINER.getHost() + ":" + CONTAINER.getServerPort() + "/");
    mockServerClient
        .when(request())
        .respond(
            response()
                .withHeader(Header.header("Content-Type", "application/json"))
                .withStatusCode(HttpStatusCodes.STATUS_CODE_UNPROCESSABLE_ENTITY));
    assertThrows(
        UnprocessableEntityException.class,
        () -> service.postAttachmentToSupport("Test".getBytes()));
  }

  // Creates support ticket with random values
  private DuosTicket generateTicket() {
    List<SupportRequestType> types = new ArrayList<>(EnumSet.allOf(SupportRequestType.class));
    Collections.shuffle(types);
    return TicketFactory.createTicket(
        new TicketFields(
            randomAlphabetic(10),
            types.get(0),
            randomAlphabetic(10),
            randomAlphabetic(10),
            randomAlphabetic(10),
            randomAlphabetic(10),
            List.of(randomAlphanumeric(10))));
  }
}
