package org.broadinstitute.consent.http.service;

import com.google.api.client.http.HttpStatusCodes;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.db.InstitutionDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.enumeration.SupportRequestType;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserUpdateFields;
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
import org.mockserver.model.HttpError;
import org.mockserver.model.HttpRequest;
import org.mockserver.verify.VerificationTimes;
import org.testcontainers.containers.MockServerContainer;

import javax.ws.rs.ServerErrorException;

import java.util.ArrayList;
import java.util.List;

import static org.broadinstitute.consent.http.WithMockServer.IMAGE;
import static org.eclipse.jetty.util.component.LifeCycle.stop;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;


public class SupportRequestServiceTest {

    private SupportRequestService service;

    private MockServerClient mockServerClient;

    @Mock
    private InstitutionDAO institutionDAO;

    @Mock
    private UserDAO userDAO;

    @Mock
    private ServicesConfiguration config;

    @Rule
    public MockServerContainer container = new MockServerContainer(IMAGE);

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        mockServerClient = new MockServerClient(container.getHost(), container.getServerPort());
        when(config.isActivateSupportNotifications()).thenReturn(true);
        when(config.postSupportRequestUrl()).thenReturn("http://" + container.getHost() + ":" + container.getServerPort() + "/");
        service = new SupportRequestService(config, institutionDAO, userDAO);
    }

    @After
    public void tearDown() {
        stop(container);
    }

    @Test
    public void testCreateSupportTicket() {
        String name = RandomStringUtils.randomAlphabetic(10);
        SupportRequestType type = SupportRequestType.QUESTION;
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
        assertTrue(customFields.contains(new CustomRequestField(360012744452L, type.getValue())));
        assertTrue(customFields.contains(new CustomRequestField(360007369412L, description)));
        assertTrue(customFields.contains(new CustomRequestField(360012744292L, name)));
        assertTrue(customFields.contains(new CustomRequestField(360012782111L, email)));
        assertTrue(customFields.contains(new CustomRequestField(360018545031L, email)));

        String commentBody = description + "\n\n------------------\nSubmitted from: " + url;
        assertEquals(commentBody, supportRequest.getComment().getBody());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateSupportTicketMissingField() {
        SupportRequestType type = SupportRequestType.QUESTION;
        String email = RandomStringUtils.randomAlphabetic(10);
        String subject = RandomStringUtils.randomAlphabetic(10);
        String description = RandomStringUtils.randomAlphabetic(10);
        String url = RandomStringUtils.randomAlphabetic(10);
        service.createSupportTicket(null, type, email, subject, description, url);
    }

    @Test
    public void testPostTicketToSupport() throws Exception {
        SupportTicket ticket = generateTicket();
        SupportTicket.SupportRequest supportRequest = ticket.getRequest();
        CustomRequestField customField = supportRequest.getCustomFields().get(0);
        String expectedBody = String.format("{\n" +
                        "  \"request\" : {\n" +
                        "    \"requester\" : {\n" +
                        "      \"name\" : \"%s\",\n" +
                        "      \"email\" : \"%s\"\n" +
                        "    },\n" +
                        "    \"subject\" : \"%s\",\n" +
                        "    \"custom_fields\" : [ {\n" +
                        "      \"id\" : %d,\n" +
                        "      \"value\" : \"%s\"\n" +
                        "    } ],\n" +
                        "    \"comment\" : {\n" +
                        "      \"body\" : \"%s\"\n" +
                        "    },\n" +
                        "    \"ticket_form_id\" : 360000669472\n" +
                        "  }\n" +
                        "}",
                supportRequest.getRequester().getName(),
                supportRequest.getRequester().getEmail(),
                supportRequest.getSubject(),
                customField.getId(),
                customField.getValue(),
                supportRequest.getComment().getBody());

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
    public void testPostTicketToSupportNotificationsNotActivated() throws Exception {
        SupportTicket ticket = generateTicket();
        when(config.isActivateSupportNotifications()).thenReturn(false);
        // verify no requests sent if activateSupportNotifications is false; throw error if post attempted
        mockServerClient.when(request()).error(new HttpError());
        service.postTicketToSupport(ticket);
    }

    @Test(expected = ServerErrorException.class)
    public void testPostTicketToSupportServerError() throws Exception {
        mockServerClient.when(request())
                .respond(response()
                        .withHeader(Header.header("Content-Type", "application/json"))
                        .withStatusCode(HttpStatusCodes.STATUS_CODE_SERVER_ERROR));
        service.postTicketToSupport(generateTicket());
    }

    @Test
    public void testHandleSuggestedUserFieldsSupportRequest() {
        String displayName = RandomStringUtils.randomAlphabetic(10);
        String email = RandomStringUtils.randomAlphabetic(10);
        User user = new User();
        user.setDisplayName(displayName);
        user.setEmail(email);
        UserUpdateFields updateFields = new UserUpdateFields();
        updateFields.setSuggestedInstitution(RandomStringUtils.randomAlphabetic(10));

        mockServerClient.when(request())
                .respond(response()
                        .withHeader(Header.header("Content-Type", "application/json"))
                        .withStatusCode(HttpStatusCodes.STATUS_CODE_CREATED));
        service.handleInstitutionSOSupportRequest(updateFields, user);
        mockServerClient.verify(request().withMethod("POST"), VerificationTimes.exactly(1));
    }

    @Test
    public void testHandleSuggestedUserFieldsSupportRequest_NoUpdates() {
        UserUpdateFields updateFields = new UserUpdateFields();
        // verify no requests sent if no suggested user fields are provided; fail if request attempted
        mockServerClient.when(request()).error(new HttpError());
        service.handleInstitutionSOSupportRequest(updateFields, new User());
        assertNull(updateFields.getSuggestedInstitution());
        assertNull(updateFields.getSuggestedSigningOfficial());
    }

    @Test
    public void testCreateSuggestedUserFieldsTicket_SuggestedInstitution() {
        String displayName = RandomStringUtils.randomAlphabetic(10);
        String email = RandomStringUtils.randomAlphabetic(10);
        User user = new User();
        user.setDisplayName(displayName);
        user.setEmail(email);

        String suggestedInstitution = RandomStringUtils.randomAlphabetic(10);
        UserUpdateFields updateFields = new UserUpdateFields();
        updateFields.setSuggestedInstitution(suggestedInstitution);

        SupportTicket ticket = service.createInstitutionSOSupportTicket(updateFields, user);
        SupportTicket.SupportRequest supportRequest = ticket.getRequest();
        assertEquals(displayName, supportRequest.getRequester().getName());
        assertEquals(email, supportRequest.getRequester().getEmail());
        assertEquals(displayName + " user updates: New Institution Request", supportRequest.getSubject());
        assertEquals(360000669472L, supportRequest.getTicketFormId());

        String expectedDescription = String.format("User %s [%s] has:\n- requested a new institution: %s",
                user.getDisplayName(),
                user.getEmail(),
                suggestedInstitution);
        List<CustomRequestField> customFields = supportRequest.getCustomFields();
        assertEquals(5, customFields.size());
        assertTrue(customFields.contains(new CustomRequestField(360012744452L, SupportRequestType.TASK.getValue())));
        assertTrue(customFields.contains(new CustomRequestField(360007369412L, expectedDescription)));
        assertTrue(customFields.contains(new CustomRequestField(360012744292L, displayName)));
        assertTrue(customFields.contains(new CustomRequestField(360012782111L, email)));
        assertTrue(customFields.contains(new CustomRequestField(360018545031L, email)));

        String commentBody = expectedDescription + "\n\n------------------\nSubmitted from: " + config.postSupportRequestUrl();
        assertEquals(commentBody, supportRequest.getComment().getBody());
    }

    @Test
    public void testCreateSuggestedUserFieldsTicket_SuggestedSigningOfficial() {
        String displayName = RandomStringUtils.randomAlphabetic(10);
        String email = RandomStringUtils.randomAlphabetic(10);
        User user = new User();
        user.setDisplayName(displayName);
        user.setEmail(email);

        String suggestedSigningOfficial = RandomStringUtils.randomAlphabetic(10);
        UserUpdateFields updateFields = new UserUpdateFields();
        updateFields.setSuggestedSigningOfficial(suggestedSigningOfficial);

        SupportTicket ticket = service.createInstitutionSOSupportTicket(updateFields, user);
        SupportTicket.SupportRequest supportRequest = ticket.getRequest();
        assertEquals(displayName + " user updates: New Signing Official Request", supportRequest.getSubject());

        String expectedDescription = String.format("User %s [%s] has:\n- requested a new signing official: %s",
                user.getDisplayName(),
                user.getEmail(),
                suggestedSigningOfficial);
        List<CustomRequestField> customFields = supportRequest.getCustomFields();
        assertTrue(customFields.contains(new CustomRequestField(360007369412L, expectedDescription)));
    }

    @Test
    public void testCreateSuggestedUserFieldsTicket_SelectedInstitution() {
        String displayName = RandomStringUtils.randomAlphabetic(10);
        String email = RandomStringUtils.randomAlphabetic(10);
        User user = new User();
        user.setDisplayName(displayName);
        user.setEmail(email);

        UserUpdateFields updateFields = new UserUpdateFields();
        updateFields.setInstitutionId(1);
        Institution institution = new Institution();
        String institutionName = RandomStringUtils.randomAlphabetic(10);
        institution.setName(institutionName);

        when(institutionDAO.findInstitutionById(1)).thenReturn(institution);

        SupportTicket ticket = service.createInstitutionSOSupportTicket(updateFields, user);
        SupportTicket.SupportRequest supportRequest = ticket.getRequest();
        assertEquals(displayName + " user updates: Institution Selection", supportRequest.getSubject());

        String expectedDescription = String.format("User %s [%s] has:\n- selected an existing institution: %s",
                user.getDisplayName(),
                user.getEmail(),
                institutionName);
        List<CustomRequestField> customFields = supportRequest.getCustomFields();
        assertEquals(5, customFields.size());
        assertTrue(customFields.contains(new CustomRequestField(360007369412L, expectedDescription)));
    }

    @Test
    public void testCreateSuggestedUserFieldsTicket_SelectedInstitutionNotFound() {
        String displayName = RandomStringUtils.randomAlphabetic(10);
        String email = RandomStringUtils.randomAlphabetic(10);
        User user = new User();
        user.setDisplayName(displayName);
        user.setEmail(email);

        UserUpdateFields updateFields = new UserUpdateFields();
        int institutionId = RandomUtils.nextInt();
        updateFields.setInstitutionId(institutionId);

        when(institutionDAO.findInstitutionById(institutionId)).thenReturn(null);

        SupportTicket ticket = service.createInstitutionSOSupportTicket(updateFields, user);
        SupportTicket.SupportRequest supportRequest = ticket.getRequest();
        assertEquals(displayName + " user updates: Institution Selection", supportRequest.getSubject());

        String expectedDescription = String.format("User %s [%s] has:\n- attempted to select institution with id %s (not found)",
                user.getDisplayName(),
                user.getEmail(),
                institutionId);
        List<CustomRequestField> customFields = supportRequest.getCustomFields();
        assertEquals(5, customFields.size());
        assertTrue(customFields.contains(new CustomRequestField(360007369412L, expectedDescription)));
    }

    @Test
    public void testCreateSuggestedUserFieldsTicket_SelectedSigningOfficial() {
        String displayName = RandomStringUtils.randomAlphabetic(10);
        String email = RandomStringUtils.randomAlphabetic(10);
        User user = new User();
        user.setDisplayName(displayName);
        user.setEmail(email);

        UserUpdateFields updateFields = new UserUpdateFields();
        updateFields.setSelectedSigningOfficialId(1);
        User signingOfficial = new User();
        signingOfficial.setDisplayName(RandomStringUtils.randomAlphabetic(10));
        signingOfficial.setEmail(RandomStringUtils.randomAlphabetic(10));

        when(userDAO.findUserById(1)).thenReturn(signingOfficial);

        SupportTicket ticket = service.createInstitutionSOSupportTicket(updateFields, user);
        SupportTicket.SupportRequest supportRequest = ticket.getRequest();
        assertEquals(displayName + " user updates: Signing Official Selection", supportRequest.getSubject());

        String expectedDescription = String.format("User %s [%s] has:\n- selected an existing signing official: %s, %s",
                user.getDisplayName(),
                user.getEmail(),
                signingOfficial.getDisplayName(),
                signingOfficial.getEmail());
        List<CustomRequestField> customFields = supportRequest.getCustomFields();
        assertEquals(5, customFields.size());
        assertTrue(customFields.contains(new CustomRequestField(360012744452L, SupportRequestType.TASK.getValue())));
        assertTrue(customFields.contains(new CustomRequestField(360007369412L, expectedDescription)));
    }

    @Test
    public void testCreateSuggestedUserFieldsTicket_SelectedSigningOfficialNotFound() {
        String displayName = RandomStringUtils.randomAlphabetic(10);
        String email = RandomStringUtils.randomAlphabetic(10);
        User user = new User();
        user.setDisplayName(displayName);
        user.setEmail(email);

        UserUpdateFields updateFields = new UserUpdateFields();
        int signingOfficialId = RandomUtils.nextInt();
        updateFields.setSelectedSigningOfficialId(signingOfficialId);

        when(userDAO.findUserById(signingOfficialId)).thenReturn(null);

        SupportTicket ticket = service.createInstitutionSOSupportTicket(updateFields, user);
        SupportTicket.SupportRequest supportRequest = ticket.getRequest();
        assertEquals(displayName + " user updates: Signing Official Selection", supportRequest.getSubject());

        String expectedDescription = String.format("User %s [%s] has:\n- attempted to select signing official with id %s (not found)",
                user.getDisplayName(),
                user.getEmail(),
                signingOfficialId);
        List<CustomRequestField> customFields = supportRequest.getCustomFields();
        assertEquals(5, customFields.size());
        assertTrue(customFields.contains(new CustomRequestField(360007369412L, expectedDescription)));
    }

    @Test
    public void testCreateSuggestedUserFieldsTicket_MultipleFields() {
        String displayName = RandomStringUtils.randomAlphabetic(10);
        String email = RandomStringUtils.randomAlphabetic(10);
        User user = new User();
        user.setDisplayName(displayName);
        user.setEmail(email);

        UserUpdateFields updateFields = new UserUpdateFields();
        String suggestedInstitution = RandomStringUtils.randomAlphabetic(10);
        updateFields.setSuggestedInstitution(suggestedInstitution);
        String suggestedSigningOfficial = RandomStringUtils.randomAlphabetic(10);
        updateFields.setSuggestedSigningOfficial(suggestedSigningOfficial);

        SupportTicket ticket = service.createInstitutionSOSupportTicket(updateFields, user);
        SupportTicket.SupportRequest supportRequest = ticket.getRequest();
        assertEquals(displayName + " user updates: New Institution Request, New Signing Official Request", supportRequest.getSubject());

        String expectedDescription = String.format("User %s [%s] has:\n" +
                        "- requested a new institution: %s\n" +
                        "- requested a new signing official: %s",
                user.getDisplayName(),
                user.getEmail(),
                suggestedInstitution,
                suggestedSigningOfficial);
        List<CustomRequestField> customFields = supportRequest.getCustomFields();
        assertEquals(5, customFields.size());
        assertTrue(customFields.contains(new CustomRequestField(360007369412L, expectedDescription)));
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
}
