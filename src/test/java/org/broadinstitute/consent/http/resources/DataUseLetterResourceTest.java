package org.broadinstitute.consent.http.resources;

import org.broadinstitute.consent.http.cloudstore.GCSStore;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.grammar.Everything;
import org.broadinstitute.consent.http.service.AbstractConsentAPI;
import org.broadinstitute.consent.http.service.AuditService;
import org.broadinstitute.consent.http.service.ConsentAPI;
import org.broadinstitute.consent.http.service.UserService;
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
import org.powermock.core.classloader.annotations.PowerMockIgnore;
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
@PowerMockIgnore("jdk.internal.reflect.*")
@PrepareForTest({
        AbstractConsentAPI.class,
        AbstractDACUserAPI.class
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
    private AuditService auditService;
    @Mock
    private UserService userService;

    private AuthUser user = new AuthUser("oauthuser@broadinstitute.org");
    private DataUseLetterResource resource;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(AbstractConsentAPI.class);
        PowerMockito.mockStatic(AbstractDACUserAPI.class);
    }

    private void initResource() {
        User user = new User();
        user.setEmail(this.user.getName());
        user.setDacUserId(1);
        when(userService.findUserByEmail(any())).thenReturn(user);
        when(AbstractConsentAPI.getInstance()).thenReturn(consentAPI);
        when(AbstractDACUserAPI.getInstance()).thenReturn(dacUserAPI);
        resource = new DataUseLetterResource(auditService, store, userService);
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
