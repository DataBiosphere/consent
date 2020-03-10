package org.broadinstitute.consent.http.resources;

import org.broadinstitute.consent.http.cloudstore.GCSStore;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.grammar.Everything;
import org.broadinstitute.consent.http.service.AbstractAuditServiceAPI;
import org.broadinstitute.consent.http.service.AbstractConsentAPI;
import org.broadinstitute.consent.http.service.AuditServiceAPI;
import org.broadinstitute.consent.http.service.ConsentAPI;
import org.broadinstitute.consent.http.service.users.AbstractDACUserAPI;
import org.broadinstitute.consent.http.service.users.DACUserAPI;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.UUID;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@SuppressWarnings("deprecation")
@RunWith(PowerMockRunner.class)
@PrepareForTest({
        AbstractConsentAPI.class,
        AbstractDACUserAPI.class,
        AbstractAuditServiceAPI.class
})
public class DataUseLetterResourceTest {

    @Mock
    private FormDataContentDisposition ct;
    @Mock
    private GCSStore store;
    @Mock
    private ConsentAPI consentAPI;
    @Mock
    private DACUserAPI dacUserAPI;
    @Mock
    private AuditServiceAPI auditServiceAPI;

    private AuthUser user = new AuthUser("oauthuser@broadinstitute.org");
    private DataUseLetterResource resource;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(AbstractConsentAPI.class);
        PowerMockito.mockStatic(AbstractDACUserAPI.class);
        PowerMockito.mockStatic(AbstractAuditServiceAPI.class);
    }

    private void initResource() {
        DACUser dacUser = new DACUser();
        dacUser.setEmail(user.getName());
        dacUser.setDacUserId(1);
        when(dacUserAPI.describeDACUserByEmail(any())).thenReturn(dacUser);
        when(AbstractConsentAPI.getInstance()).thenReturn(consentAPI);
        when(AbstractDACUserAPI.getInstance()).thenReturn(dacUserAPI);
        when(AbstractAuditServiceAPI.getInstance()).thenReturn(auditServiceAPI);
        resource = new DataUseLetterResource(store);
    }

    @Test
    public void testCreateDUL() throws Exception {
        Consent consent = setupConsent(null);
        File fileToUpload = File.createTempFile("temp","pdf");
        fileToUpload.deleteOnExit();
        when(store.postStorageDocument(any(InputStream.class), eq("application/pdf"), anyString())).
                thenReturn(consentDulPath(consent.getConsentId()));
        when(ct.getFileName()).thenReturn("temp.pdf");
        //noinspection ResultOfMethodCallIgnored
        fileToUpload.createNewFile();
        FormDataBodyPart bodyPart = new FormDataBodyPart();
        bodyPart.setContentDisposition(ct);
        bodyPart.setMediaType(MediaType.valueOf("application/pdf"));
        when(consentAPI.updateConsentDul(any(), any(), any())).thenReturn(consent);

        initResource();
        Consent c = resource.createDUL(new FileInputStream(fileToUpload), bodyPart, consent.getConsentId(), "temp.pdf", user);
        assertNotNull(c);
    }

    @Test
    public void testUpdateAssociatedDUL() throws Exception {
        File fileToUpload = File.createTempFile("temp","pdf");
        fileToUpload.deleteOnExit();
        Consent consent = setupConsent(fileToUpload.getName());
        when(store.putStorageDocument(any(InputStream.class), eq("application/pdf"), anyString())).
                thenReturn(consentDulPath(consent.getConsentId()));
        when(ct.getFileName()).thenReturn("temp.pdf");
        //noinspection ResultOfMethodCallIgnored
        fileToUpload.createNewFile();
        FormDataBodyPart bodyPart = new FormDataBodyPart();
        bodyPart.setContentDisposition(ct);
        bodyPart.setMediaType(MediaType.valueOf("application/pdf"));
        when(consentAPI.updateConsentDul(any(), any(), any())).thenReturn(consent);

        initResource();
        Consent c = resource.updateDUL(new FileInputStream(fileToUpload), bodyPart, consent.getConsentId(), "temp", user);
        assertNotNull(c);
        assertNotNull(c.getDulName());
    }

    @Test
    public void testDeleteDUL() throws Exception {
        Consent consent = setupConsent("dataUseLetterToDelete");
        when(store.deleteStorageDocument(consent.getConsentId())).thenReturn(true);
        when(consentAPI.deleteConsentDul(any())).thenReturn(consent);

        initResource();
        Consent c = resource.deleteDUL(consent.getConsentId());
        assertNotNull(c);
    }

    private Consent setupConsent(String dul) {
        DataUse dataUse = new DataUseBuilder().setGeneralUse(true).build();
        Consent consent = new Consent();
        consent.setDulName(dul);
        consent.setUseRestriction(new Everything());
        consent.setConsentId(UUID.randomUUID().toString());
        consent.setDataUse(dataUse);
        return consent;
    }

    private String consentDulPath(String consentId) {
        return consentId + "/dul";
    }
}
