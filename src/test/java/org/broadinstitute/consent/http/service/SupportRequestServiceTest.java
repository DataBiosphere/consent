package org.broadinstitute.consent.http.service;

import com.google.api.client.http.HttpStatusCodes;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.support.CustomRequestField;
import org.broadinstitute.consent.http.models.support.SupportRequestComment;
import org.broadinstitute.consent.http.models.support.SupportRequester;
import org.broadinstitute.consent.http.models.support.SupportTicket;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.Header;
import org.mockserver.model.HttpRequest;
import org.testcontainers.containers.MockServerContainer;

import javax.ws.rs.ServerErrorException;

import java.util.ArrayList;
import java.util.List;

import static org.broadinstitute.consent.http.WithMockServer.IMAGE;
import static org.eclipse.jetty.util.component.LifeCycle.stop;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;


public class SupportRequestServiceTest {

    private SupportRequestService service;

    private MockServerClient mockServerClient;

    @Mock
    private ServicesConfiguration config;

    @Mock
    private AuthUser authUser;

    @Rule
    public MockServerContainer container = new MockServerContainer(IMAGE);

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        mockServerClient = new MockServerClient(container.getHost(), container.getServerPort());
        when(config.postSupportRequestUrl()).thenReturn("http://" + container.getHost() + ":" + container.getServerPort() + "/");
        service = new SupportRequestService(config);
    }

    @After
    public void tearDown() {
        stop(container);
    }

    @Test
    public void testCreateSupportTicket() {
        String name = RandomStringUtils.randomAlphabetic(10);
        String type = RandomStringUtils.randomAlphabetic(10);
        String email = RandomStringUtils.randomAlphabetic(10);
        String subject = RandomStringUtils.randomAlphabetic(10);
        String description = RandomStringUtils.randomAlphabetic(10);
        String url = RandomStringUtils.randomAlphabetic(10);
        SupportTicket ticket = service.createSupportTicket(name, type, email, subject, description, url);

        assertNotNull(ticket);
        assertNotNull(ticket.getRequest());
        SupportTicket.SupportRequest supportRequest = ticket.getRequest();
        assertEquals(name, supportRequest.getRequester().getName());
        assertEquals(email, supportRequest.getRequester().getEmail());
        assertEquals(subject, supportRequest.getSubject());
        assertEquals(360000669472L, supportRequest.getTicketFormId());

        List<CustomRequestField> customFields = supportRequest.getCustomFields();
        assertEquals(5, customFields.size());
        assertTrue(customFields.contains(new CustomRequestField(360012744452L, type)));
        assertTrue(customFields.contains(new CustomRequestField(360007369412L, description)));
        assertTrue(customFields.contains(new CustomRequestField(360012744292L, name)));
        assertTrue(customFields.contains(new CustomRequestField(360012782111L, email)));
        assertTrue(customFields.contains(new CustomRequestField(360018545031L, email)));

        String commentBody = description + "\n\n------------------\nSubmitted from: " + url;
        assertEquals(commentBody, supportRequest.getComment().getBody());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateSupportTicketMissingField() {
        String type = RandomStringUtils.randomAlphabetic(10);
        String email = RandomStringUtils.randomAlphabetic(10);
        String subject = RandomStringUtils.randomAlphabetic(10);
        String description = RandomStringUtils.randomAlphabetic(10);
        String url = RandomStringUtils.randomAlphabetic(10);
        service.createSupportTicket(null, type, email, subject, description, url);
    }

    @Test
    public void testPostTicketToSupport() throws Exception {
        SupportTicket ticket = generateTicket();
        mockServerClient.when(request().withMethod("POST"))
                .respond(response()
                        .withHeader(Header.header("Content-Type", "application/json"))
                        .withStatusCode(HttpStatusCodes.STATUS_CODE_OK));
        service.postTicketToSupport(ticket, authUser);

        HttpRequest[] requests = mockServerClient.retrieveRecordedRequests(null);
        assertEquals(1, requests.length);
        Object requestBody = requests[0].getBody().getValue();
        assertEquals(convertTicketToJson(ticket), requestBody);
    }

    @Test(expected = ServerErrorException.class)
    public void testPostTicketToSupportServerError() throws Exception {
        mockServerClient.when(request())
                .respond(response()
                        .withHeader(Header.header("Content-Type", "application/json"))
                        .withStatusCode(HttpStatusCodes.STATUS_CODE_SERVER_ERROR));
        service.postTicketToSupport(generateTicket(), authUser);
    }

    @Test
    public void testSendSuggestedPropertiesToSupport_NoUpdates() {

    }

    @Test
    public void testSendSuggestedPropertiesToSupport_Institution() {

    }

    @Test
    public void testSendSuggestedPropertiesToSupport_SigningOfficial() {

    }

    @Test
    public void testSendSuggestedPropertiesToSupport_InstitutionSigningOfficial() {

    }


    //creates support ticket with random values for testing postTicketToSupport
    private SupportTicket generateTicket() {
        SupportRequester requester = new SupportRequester(
                RandomStringUtils.randomAlphabetic(10),
                RandomStringUtils.randomAlphabetic(10)
        );
        String subject = RandomStringUtils.randomAlphabetic(10);
        List<CustomRequestField> customFields = new ArrayList<>();
        customFields.add(new CustomRequestField(
                RandomUtils.nextLong(),
                RandomStringUtils.randomAlphabetic(10)
        ));
        SupportRequestComment comment = new SupportRequestComment(RandomStringUtils.randomAlphabetic(10));

        return new SupportTicket(requester, subject, customFields, comment);
    }

    private String convertTicketToJson(SupportTicket ticket) {
        SupportTicket.SupportRequest supportRequest = ticket.getRequest();
        CustomRequestField customField = supportRequest.getCustomFields().get(0);
        return String.format("{\n" +
                        "  \"request\": {\n" +
                        "    \"requester\": {\n" +
                        "      \"name\": \"%s\",\n" +
                        "      \"email\": \"%s\"\n" +
                        "    },\n" +
                        "    \"subject\": \"%s\",\n" +
                        "    \"custom_fields\": [\n" +
                        "      {\n" +
                        "        \"id\": %d,\n" +
                        "        \"value\": \"%s\"\n" +
                        "      }\n" +
                        "    ],\n" +
                        "    \"comment\": {\n" +
                        "      \"body\": \"%s\"\n" +
                        "    },\n" +
                        "    \"ticket_form_id\": 360000669472\n" +
                        "  }\n" +
                        "}",
                supportRequest.getRequester().getName(),
                supportRequest.getRequester().getEmail(),
                supportRequest.getSubject(),
                customField.getId(),
                customField.getValue(),
                supportRequest.getComment().getBody());
    }
}
