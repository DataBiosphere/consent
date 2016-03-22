package org.broadinstitute.consent.http;

import org.broadinstitute.consent.http.ConsentApplication;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.broadinstitute.consent.http.cloudstore.GCSStore;
import org.broadinstitute.consent.http.configurations.ConsentConfiguration;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.grammar.Everything;
import org.broadinstitute.consent.http.resources.DataUseLetterResource;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Timestamp;
import java.util.Date;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.when;

public class DataUseLetterResourceTest extends ConsentServiceTest {

    @Mock
    GCSStore storage;
    @Mock
    FormDataContentDisposition ct;

    DataUseLetterResource dulResource;

    @ClassRule
    public static final DropwizardAppRule<ConsentConfiguration> RULE =
            new DropwizardAppRule<>(ConsentApplication.class,
                    resourceFilePath("consent-config.yml"));

    @Override
    public DropwizardAppRule<ConsentConfiguration> rule() {
        return RULE;
    }

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        dulResource = new DataUseLetterResource(storage);
        mockTranslateResponse();
        mockValidateResponse();
    }

    @Test
    public void testAssociateDUL() throws Exception {
        String id = setupConsent(null);
        File fileToUpload = File.createTempFile("temp","pdf");
        fileToUpload.deleteOnExit();
        when(storage.postStorageDocument(anyString(), any(InputStream.class), eq("application/pdf"), eq("pdf"))).
                thenReturn(consentDulPath(id));
        when(ct.getFileName()).thenReturn("temp.pdf");
        //noinspection ResultOfMethodCallIgnored
        fileToUpload.createNewFile();
        FormDataBodyPart bodyPart = new FormDataBodyPart();
        bodyPart.setContentDisposition(ct);
        bodyPart.setMediaType(MediaType.valueOf("application/pdf"));
        Consent c = dulResource.createDUL(new FileInputStream(fileToUpload), bodyPart, id);
        assertEquals(c.consentId, id);
        assertEquals(c.getDataUseLetter(), consentDulPath(id));
        assertFalse(c.getRequiresManualReview());
        assertEquals(c.getUseRestriction(), new Everything());
    }

    @Test
    public void testUpdateAssociatedDUL() throws Exception {
        String id = setupConsent(null);
        File fileToUpload = File.createTempFile("temp","pdf");
        fileToUpload.deleteOnExit();
        when(storage.putStorageDocument(anyString(), any(InputStream.class), eq("application/pdf"), eq("pdf"))).
                thenReturn(consentDulPath(id));
        when(ct.getFileName()).thenReturn("temp.pdf");
        //noinspection ResultOfMethodCallIgnored
        fileToUpload.createNewFile();
        FormDataBodyPart bodyPart = new FormDataBodyPart();
        bodyPart.setContentDisposition(ct);
        bodyPart.setMediaType(MediaType.valueOf("application/pdf"));
        Consent c = dulResource.updateDUL(new FileInputStream(fileToUpload), bodyPart, id);
        assertEquals(c.consentId, id);
        assertEquals(c.getDataUseLetter(), consentDulPath(id));
        assertFalse(c.getRequiresManualReview());
        assertEquals(c.getUseRestriction(), new Everything());

    }

    @Test
    public void testDeleteDUL() throws Exception {
        String id = setupConsent("dataUseLetterToDelete");
        when(storage.deleteStorageDocument(id)).thenReturn(true);
        Consent c = dulResource.deleteDUL(id);
        assertEquals(c.consentId, id);
        assertEquals(c.getDataUseLetter(), "");
        assertFalse(c.getRequiresManualReview());
        assertEquals(c.getUseRestriction(), new Everything());
    }

    private String setupConsent(String dul) {
        Client client = ClientBuilder.newBuilder().register(MultiPartFeature.class).build();
        Timestamp createDate = new Timestamp(new Date().getTime());
        Consent rec = new Consent(false, new Everything(), dul, UUID.randomUUID().toString(), createDate, createDate, createDate);
        Response response = checkStatus(CREATED, post(client, consentPath(), rec));
        String createdLocation = checkHeader(response, "Location");
        String consent_id = createdLocation.substring(createdLocation.lastIndexOf("/") + 1);
        System.out.println(String.format("setupConsent created consent with id '%s' at location '%s'", createdLocation, consent_id));
        return consent_id;
    }

    //
    //  HELPER METHODS
    //

    private String consentDulPath(String consentId) {
        return consentPath(consentId) + "/dul";
    }
}
