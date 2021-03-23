package org.broadinstitute.consent.http.resources;

import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.service.AbstractMatchAPI;
import org.broadinstitute.consent.http.service.AbstractMatchProcessAPI;
import org.broadinstitute.consent.http.service.AuditService;
import org.broadinstitute.consent.http.service.ConsentService;
import org.broadinstitute.consent.http.service.MatchAPI;
import org.broadinstitute.consent.http.service.MatchProcessAPI;
import org.broadinstitute.consent.http.service.UserService;
import org.broadinstitute.consent.http.service.users.AbstractDACUserAPI;
import org.broadinstitute.consent.http.service.users.DACUserAPI;
import org.broadinstitute.consent.http.service.validate.AbstractUseRestrictionValidatorAPI;
import org.broadinstitute.consent.http.service.validate.UseRestrictionValidatorAPI;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@SuppressWarnings("deprecation")
@RunWith(PowerMockRunner.class)
@PowerMockIgnore("jdk.internal.reflect.*")
@PrepareForTest({
        AbstractDACUserAPI.class,
        AbstractMatchProcessAPI.class,
        AbstractMatchAPI.class,
        AbstractUseRestrictionValidatorAPI.class
})
public class ConsentResourceTest {

    @Mock
    private DACUserAPI dacUserAPI;
    @Mock
    private AuditService auditService;
    @Mock
    private ConsentService consentService;
    @Mock
    private MatchProcessAPI matchProcessAPI;
    @Mock
    private MatchAPI matchAPI;
    @Mock
    private UseRestrictionValidatorAPI useRestrictionValidatorAPI;
    @Mock
    private UserService userService;
    @Mock
    UriInfo info;
    @Mock
    UriBuilder builder;

    private ConsentResource resource;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(AbstractDACUserAPI.class);
        PowerMockito.mockStatic(AbstractMatchProcessAPI.class);
        PowerMockito.mockStatic(AbstractMatchAPI.class);
        PowerMockito.mockStatic(AbstractUseRestrictionValidatorAPI.class);
    }

    private void initResource() {
        when(builder.path(anyString())).thenReturn(builder);
        when(builder.build()).thenReturn(URI.create("https://test.domain.org/some/path"));
        when(info.getRequestUriBuilder()).thenReturn(builder);
        when(AbstractDACUserAPI.getInstance()).thenReturn(dacUserAPI);
        when(AbstractMatchProcessAPI.getInstance()).thenReturn(matchProcessAPI);
        when(AbstractMatchAPI.getInstance()).thenReturn(matchAPI);
        when(AbstractUseRestrictionValidatorAPI.getInstance()).thenReturn(useRestrictionValidatorAPI);
        resource = new ConsentResource(auditService, userService, consentService);
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
        doNothing().when(useRestrictionValidatorAPI).validateUseRestriction(any());
        when(consentService.create(any())).thenReturn(consent);
        doNothing().when(auditService).saveConsentAudit(any(), any(), any(), any());
        doNothing().when(matchProcessAPI).processMatchesForConsent(any());

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
        doNothing().when(useRestrictionValidatorAPI).validateUseRestriction(any());
        when(consentService.retrieve(any())).thenReturn(consent);
        when(consentService.update(any(), any())).thenReturn(consent);
        doNothing().when(auditService).saveConsentAudit(any(), any(), any(), any());
        doNothing().when(matchProcessAPI).processMatchesForConsent(any());

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
        doNothing().when(useRestrictionValidatorAPI).validateUseRestriction(any());
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
        doNothing().when(useRestrictionValidatorAPI).validateUseRestriction(any());
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
        doNothing().when(useRestrictionValidatorAPI).validateUseRestriction(any());
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
        doNothing().when(useRestrictionValidatorAPI).validateUseRestriction(any());
        initResource();

        Response response = resource.update(consent.getConsentId(), consent, user);
        assertEquals(400, response.getStatus());
    }

}
