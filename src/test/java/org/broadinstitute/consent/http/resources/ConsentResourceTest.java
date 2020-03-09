package org.broadinstitute.consent.http.resources;

import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.service.AbstractAuditServiceAPI;
import org.broadinstitute.consent.http.service.AbstractConsentAPI;
import org.broadinstitute.consent.http.service.AbstractElectionAPI;
import org.broadinstitute.consent.http.service.AbstractMatchAPI;
import org.broadinstitute.consent.http.service.AbstractMatchProcessAPI;
import org.broadinstitute.consent.http.service.AuditServiceAPI;
import org.broadinstitute.consent.http.service.ConsentAPI;
import org.broadinstitute.consent.http.service.ElectionAPI;
import org.broadinstitute.consent.http.service.MatchAPI;
import org.broadinstitute.consent.http.service.MatchProcessAPI;
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
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@SuppressWarnings("deprecation")
@RunWith(PowerMockRunner.class)
@PrepareForTest({
        AbstractConsentAPI.class,
        AbstractDACUserAPI.class,
        AbstractAuditServiceAPI.class,
        AbstractMatchProcessAPI.class,
        AbstractMatchAPI.class,
        AbstractUseRestrictionValidatorAPI.class,
        AbstractElectionAPI.class,
})
public class ConsentResourceTest {

    @Mock
    private ConsentAPI api;
    @Mock
    private DACUserAPI dacUserAPI;
    @Mock
    private AuditServiceAPI auditServiceAPI;
    @Mock
    private MatchProcessAPI matchProcessAPI;
    @Mock
    private MatchAPI matchAPI;
    @Mock
    private UseRestrictionValidatorAPI useRestrictionValidatorAPI;
    @Mock
    private ElectionAPI electionAPI;
    @Mock
    UriInfo info;
    @Mock
    UriBuilder builder;

    private ConsentResource resource;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(AbstractConsentAPI.class);
        PowerMockito.mockStatic(AbstractDACUserAPI.class);
        PowerMockito.mockStatic(AbstractAuditServiceAPI.class);
        PowerMockito.mockStatic(AbstractMatchProcessAPI.class);
        PowerMockito.mockStatic(AbstractMatchAPI.class);
        PowerMockito.mockStatic(AbstractUseRestrictionValidatorAPI.class);
        PowerMockito.mockStatic(AbstractElectionAPI.class);
    }

    private void initResource() {
        when(builder.path(anyString())).thenReturn(builder);
        when(builder.build()).thenReturn(URI.create("https://test.domain.org/some/path"));
        when(info.getRequestUriBuilder()).thenReturn(builder);
        when(AbstractConsentAPI.getInstance()).thenReturn(api);
        when(AbstractDACUserAPI.getInstance()).thenReturn(dacUserAPI);
        when(AbstractAuditServiceAPI.getInstance()).thenReturn(auditServiceAPI);
        when(AbstractMatchProcessAPI.getInstance()).thenReturn(matchProcessAPI);
        when(AbstractMatchAPI.getInstance()).thenReturn(matchAPI);
        when(AbstractUseRestrictionValidatorAPI.getInstance()).thenReturn(useRestrictionValidatorAPI);
        when(AbstractElectionAPI.getInstance()).thenReturn(electionAPI);
        resource = new ConsentResource();
    }

    @Test
    public void testCreateConsent() throws Exception {
        DACUser dacUser = new DACUser();
        dacUser.setEmail("test@email.com");
        AuthUser user = new AuthUser(dacUser.getEmail());
        Consent consent = new Consent();
        consent.setConsentId(UUID.randomUUID().toString());
        consent.setDataUse(new DataUseBuilder().setGeneralUse(true).build());

        when(dacUserAPI.describeDACUserByEmail(any())).thenReturn(dacUser);
        doNothing().when(useRestrictionValidatorAPI).validateUseRestriction(any());
        when(api.create(any())).thenReturn(consent);
        doNothing().when(auditServiceAPI).saveConsentAudit(any(), any(), any(), any());
        doNothing().when(matchProcessAPI).processMatchesForConsent(any());

        initResource();

        Response response = resource.createConsent(info, consent, user);
        assertEquals(201, response.getStatus());
    }

    @Test
    public void testUpdateConsent() throws Exception {
        DACUser dacUser = new DACUser();
        dacUser.setEmail("test@email.com");
        AuthUser user = new AuthUser(dacUser.getEmail());
        Consent consent = new Consent();
        consent.setLastElectionArchived(true); // Unarchived consents cannot be updated.
        consent.setConsentId(UUID.randomUUID().toString());
        consent.setDataUse(new DataUseBuilder().setGeneralUse(true).build());

        when(dacUserAPI.describeDACUserByEmail(any())).thenReturn(dacUser);
        doNothing().when(useRestrictionValidatorAPI).validateUseRestriction(any());
        when(api.retrieve(any())).thenReturn(consent);
        when(api.update(any(), any())).thenReturn(consent);
        doNothing().when(auditServiceAPI).saveConsentAudit(any(), any(), any(), any());
        doNothing().when(matchProcessAPI).processMatchesForConsent(any());

        initResource();

        Response response = resource.update(consent.getConsentId(), consent, user);
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testDeleteConsent() {
        doNothing().when(api).delete(any());
        initResource();

        Response response = resource.delete(UUID.randomUUID().toString());
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testMissingDataUseCreate() throws Exception {
        DACUser dacUser = new DACUser();
        dacUser.setEmail("test@email.com");
        AuthUser user = new AuthUser(dacUser.getEmail());
        Consent consent = new Consent();
        consent.setConsentId(UUID.randomUUID().toString());
        when(dacUserAPI.describeDACUserByEmail(any())).thenReturn(dacUser);
        doNothing().when(useRestrictionValidatorAPI).validateUseRestriction(any());
        initResource();

        Response response = resource.createConsent(info, consent, user);
        assertEquals(400, response.getStatus());
    }

    @Test
    public void testMissingDataUseUpdate() throws Exception {
        DACUser dacUser = new DACUser();
        dacUser.setEmail("test@email.com");
        AuthUser user = new AuthUser(dacUser.getEmail());
        Consent consent = new Consent();
        consent.setConsentId(UUID.randomUUID().toString());
        when(api.retrieve(any())).thenReturn(consent);
        when(dacUserAPI.describeDACUserByEmail(any())).thenReturn(dacUser);
        doNothing().when(useRestrictionValidatorAPI).validateUseRestriction(any());
        initResource();

        Response response = resource.update(consent.getConsentId(), consent, user);
        assertEquals(400, response.getStatus());
    }


}
