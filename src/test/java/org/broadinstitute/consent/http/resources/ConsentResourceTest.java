package org.broadinstitute.consent.http.resources;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.UUID;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.AuditService;
import org.broadinstitute.consent.http.service.ConsentService;
import org.broadinstitute.consent.http.service.MatchService;
import org.broadinstitute.consent.http.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ConsentResourceTest {

    @Mock
    private AuditService auditService;
    @Mock
    private ConsentService consentService;
    @Mock
    private MatchService matchService;
    @Mock
    private UserService userService;
    @Mock
    UriInfo info;
    @Mock
    UriBuilder builder;

    private ConsentResource resource;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    private void initResource() {
        when(builder.path(anyString())).thenReturn(builder);
        when(builder.build()).thenReturn(URI.create("https://test.domain.org/some/path"));
        when(info.getRequestUriBuilder()).thenReturn(builder);
        resource = new ConsentResource(auditService, userService, consentService, matchService);
    }

    @Test
    public void testCreateConsent() throws Exception {
        User dacUser = new User();
        dacUser.setEmail("test@email.com");
        AuthUser user = new AuthUser(dacUser.getEmail());
        Consent consent = new Consent();
        consent.setConsentId(UUID.randomUUID().toString());
        consent.setDataUse(new DataUseBuilder().setGeneralUse(true).build());

        when(userService.findUserByEmail(any())).thenReturn(dacUser);
        when(consentService.create(any())).thenReturn(consent);
        doNothing().when(auditService).saveConsentAudit(any(), any(), any(), any());
        doNothing().when(matchService).reprocessMatchesForConsent(any());

        initResource();

        Response response = resource.createConsent(info, consent, user);
        assertEquals(201, response.getStatus());
    }

    @Test
    public void testUpdateConsent() throws Exception {
        User dacUser = new User();
        dacUser.setEmail("test@email.com");
        AuthUser user = new AuthUser(dacUser.getEmail());
        Consent consent = new Consent();
        consent.setLastElectionArchived(true); // Unarchived consents cannot be updated.
        consent.setConsentId(UUID.randomUUID().toString());
        consent.setDataUse(new DataUseBuilder().setGeneralUse(true).build());

        when(userService.findUserByEmail(any())).thenReturn(dacUser);
        when(consentService.retrieve(any())).thenReturn(consent);
        when(consentService.update(any(), any())).thenReturn(consent);
        doNothing().when(auditService).saveConsentAudit(any(), any(), any(), any());
        doNothing().when(matchService).reprocessMatchesForConsent(any());

        initResource();

        Response response = resource.update(consent.getConsentId(), consent, user);
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testDeleteConsent() {
        doNothing().when(consentService).delete(any());
        initResource();

        Response response = resource.delete(UUID.randomUUID().toString());
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testMissingDataUseCreate() {
        User dacUser = new User();
        dacUser.setEmail("test@email.com");
        AuthUser user = new AuthUser(dacUser.getEmail());
        Consent consent = new Consent();
        consent.setConsentId(UUID.randomUUID().toString());
        when(userService.findUserByEmail(any())).thenReturn(dacUser);
        initResource();

        Response response = resource.createConsent(info, consent, user);
        assertEquals(400, response.getStatus());
    }

    @Test
    public void testMissingDataUseUpdate() throws Exception {
        User dacUser = new User();
        dacUser.setEmail("test@email.com");
        AuthUser user = new AuthUser(dacUser.getEmail());
        Consent consent = new Consent();
        consent.setConsentId(UUID.randomUUID().toString());
        when(consentService.retrieve(any())).thenReturn(consent);
        when(userService.findUserByEmail(any())).thenReturn(dacUser);
        initResource();

        Response response = resource.update(consent.getConsentId(), consent, user);
        assertEquals(400, response.getStatus());
    }

    @Test
    public void testInvalidDULCreate() throws Exception {
        User dacUser = new User();
        dacUser.setEmail("test@email.com");
        AuthUser user = new AuthUser(dacUser.getEmail());
        Consent consent = new Consent();
        consent.setDataUse(new DataUseBuilder().setGeneralUse(true).build());
        consent.setConsentId(UUID.randomUUID().toString());
        consent.setDataUseLetter(UUID.randomUUID().toString());
        when(consentService.retrieve(any())).thenReturn(consent);
        when(userService.findUserByEmail(any())).thenReturn(dacUser);
        initResource();

        Response response = resource.createConsent(info, consent, user);
        assertEquals(400, response.getStatus());
    }

    @Test
    public void testInvalidDULUpdate() throws Exception {
        User dacUser = new User();
        dacUser.setEmail("test@email.com");
        AuthUser user = new AuthUser(dacUser.getEmail());
        Consent consent = new Consent();
        consent.setConsentId(UUID.randomUUID().toString());
        consent.setDataUseLetter(UUID.randomUUID().toString());
        when(consentService.retrieve(any())).thenReturn(consent);
        when(userService.findUserByEmail(any())).thenReturn(dacUser);
        initResource();

        Response response = resource.update(consent.getConsentId(), consent, user);
        assertEquals(400, response.getStatus());
    }

}
