package org.genomebridge.consent.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import io.dropwizard.testing.junit.DropwizardAppRule;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.text.ParseException;
import java.util.UUID;

import javax.ws.rs.core.MediaType;

import org.genomebridge.consent.http.cloudstore.GCSStore;
import org.genomebridge.consent.http.models.Consent;
import org.genomebridge.consent.http.models.grammar.Everything;
import org.genomebridge.consent.http.resources.DataUseLetterResource;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataBodyPart;

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
    public void setUp() throws IOException, GeneralSecurityException {
        MockitoAnnotations.initMocks(this);
        dulResource = new DataUseLetterResource(storage);
    }

    @Test
    public void testAssociateDUL() throws IOException, GeneralSecurityException, ParseException {
        String id = setupConsent(null);
        File fileToUpload = new File("temp.pdf");
        when(storage.postStorageDocument(anyString(), any(InputStream.class), eq("application/pdf"), eq("pdf"))).thenReturn(consentDulPath(id));
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
    public void testUpdateAssociatedDUL() throws IOException, GeneralSecurityException {
        String id = setupConsent(null);
        File fileToUpload = new File("temp.pdf");
        when(storage.putStorageDocument(anyString(), any(InputStream.class), eq("application/pdf"), eq("pdf"))).thenReturn(consentDulPath(id));
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
    public void testDeleteDUL() throws IOException, GeneralSecurityException {
        String id = setupConsent("dataUseLetterToDelete");
        when(storage.deleteStorageDocument(id)).thenReturn(true);
        Consent c = dulResource.deleteDUL(id);
        assertEquals(c.consentId, id);
        assertEquals(c.getDataUseLetter(), "");
        assertFalse(c.getRequiresManualReview());
        assertEquals(c.getUseRestriction(), new Everything());
    }

    private String setupConsent(String dul) {
        Client client = new Client();
        Consent rec = new Consent(false, new Everything(), dul, "structuredDataUseLetter", UUID.randomUUID().toString());
        ClientResponse response = checkStatus(CREATED, put(client, consentPath(), rec));
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
