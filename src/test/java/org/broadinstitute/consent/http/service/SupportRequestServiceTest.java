package org.broadinstitute.consent.http.service;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.github.tomakehurst.wiremock.http.Fault;
import com.google.api.client.http.HttpStatusCodes;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ServerErrorException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import org.broadinstitute.consent.http.WireMockTestHelper;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.enumeration.SupportRequestType;
import org.broadinstitute.consent.http.exceptions.UnprocessableEntityException;
import org.broadinstitute.consent.http.models.support.DuosTicket;
import org.broadinstitute.consent.http.models.support.TicketFactory;
import org.broadinstitute.consent.http.models.support.TicketFields;
import org.broadinstitute.consent.http.util.HttpClientUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SupportRequestServiceTest extends WireMockTestHelper {

  private SupportRequestService service;
  @Mock private ServicesConfiguration config;

  @BeforeEach
  void init() {
    service = new SupportRequestService(new HttpClientUtil(config), new TicketFactory(), config);
  }

  @Test
  void testPostTicketToSupport() throws Exception {
    DuosTicket ticket = generateTicket();
    String expectedBody = ticket.toString();

    when(config.isActivateSupportNotifications()).thenReturn(true);
    when(config.postSupportRequestUrl()).thenReturn(mockServerBaseUrl() + "/");
    wireMockServer.stubFor(
        post(anyUrl())
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withStatus(HttpStatusCodes.STATUS_CODE_CREATED)
                    .withBody(expectedBody)));

    service.postTicketToSupport(ticket);
    URL supportUrl = URI.create(config.postSupportRequestUrl()).toURL();
    wireMockServer.verify(
        exactly(1),
        postRequestedFor(urlPathEqualTo(supportUrl.getPath()))
            .withRequestBody(equalTo(expectedBody)));
  }

  @Test
  void testPostTicketToSupportNotificationsNotActivated() {
    DuosTicket ticket = generateTicket();
    when(config.isActivateSupportNotifications()).thenReturn(false);
    // verify no requests sent if activateSupportNotifications is false; throw error if post
    // attempted
    wireMockServer.stubFor(
        any(anyUrl()).willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));
    assertThrows(BadRequestException.class, () -> service.postTicketToSupport(ticket));
  }

  @Test
  void testPostTicketToSupportNotificationsUnprocessableEntity() {
    DuosTicket ticket = generateTicket();
    when(config.isActivateSupportNotifications()).thenReturn(true);
    when(config.postSupportRequestUrl()).thenReturn(mockServerBaseUrl() + "/");
    wireMockServer.stubFor(
        any(anyUrl())
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withStatus(HttpStatusCodes.STATUS_CODE_UNPROCESSABLE_ENTITY)));
    assertThrows(UnprocessableEntityException.class, () -> service.postTicketToSupport(ticket));
  }

  @Test
  void testPostTicketToSupportServerError() {
    DuosTicket ticket = generateTicket();
    when(config.isActivateSupportNotifications()).thenReturn(true);
    when(config.postSupportRequestUrl()).thenReturn(mockServerBaseUrl() + "/");
    wireMockServer.stubFor(
        any(anyUrl())
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withStatus(HttpStatusCodes.STATUS_CODE_SERVER_ERROR)));
    assertThrows(ServerErrorException.class, () -> service.postTicketToSupport(ticket));
  }

  @Test
  void testPostAttachmentToSupport() throws Exception {
    String expectedBody =
        """
        { "upload": { "token": "token string" } }
        """;
    when(config.isActivateSupportNotifications()).thenReturn(true);
    when(config.postSupportUploadUrl()).thenReturn(mockServerBaseUrl() + "/");
    wireMockServer.stubFor(
        post(anyUrl())
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withStatus(HttpStatusCodes.STATUS_CODE_CREATED)
                    .withBody(expectedBody)));
    service = new SupportRequestService(new HttpClientUtil(config), new TicketFactory(), config);
    service.postAttachmentToSupport("Test".getBytes());
    URL supportUrl = URI.create(config.postSupportUploadUrl()).toURL();
    wireMockServer.verify(exactly(1), postRequestedFor(urlPathEqualTo(supportUrl.getPath())));
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
    when(config.postSupportUploadUrl()).thenReturn(mockServerBaseUrl() + "/");
    wireMockServer.stubFor(
        post(anyUrl())
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withStatus(HttpStatusCodes.STATUS_CODE_CREATED)
                    .withBody(expectedBody)));
    assertThrows(
        ServerErrorException.class, () -> service.postAttachmentToSupport("Test".getBytes()));
  }

  @Test
  void testPostAttachmentToSupportNotificationsNotActivated() {
    when(config.isActivateSupportNotifications()).thenReturn(false);
    // verify no requests sent if activateSupportNotifications is false; throw error if post
    // attempted
    wireMockServer.stubFor(
        any(anyUrl()).willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));
    assertThrows(
        BadRequestException.class, () -> service.postAttachmentToSupport("Test".getBytes()));
  }

  @Test
  void testPostAttachmentToSupportServerError() {
    when(config.isActivateSupportNotifications()).thenReturn(true);
    when(config.postSupportUploadUrl()).thenReturn(mockServerBaseUrl() + "/");
    wireMockServer.stubFor(
        any(anyUrl())
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withStatus(HttpStatusCodes.STATUS_CODE_SERVER_ERROR)));
    assertThrows(
        ServerErrorException.class, () -> service.postAttachmentToSupport("Test".getBytes()));
  }

  @Test
  void testPostAttachmentToSupportUnprocessableEntity() {
    when(config.isActivateSupportNotifications()).thenReturn(true);
    when(config.postSupportUploadUrl()).thenReturn(mockServerBaseUrl() + "/");
    wireMockServer.stubFor(
        any(anyUrl())
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withStatus(HttpStatusCodes.STATUS_CODE_UNPROCESSABLE_ENTITY)));
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
