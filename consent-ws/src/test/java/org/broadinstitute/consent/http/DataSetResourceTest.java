package org.broadinstitute.consent.http;

import com.google.common.io.Resources;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.broadinstitute.consent.http.configurations.ConsentConfiguration;
import org.broadinstitute.consent.http.models.DataSet;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import static org.junit.Assert.assertTrue;

public class DataSetResourceTest extends DataSetServiceTest {

    @ClassRule
    public static final DropwizardAppRule<ConsentConfiguration> RULE = new DropwizardAppRule<>(
            ConsentApplication.class, resourceFilePath("consent-config.yml"));

    @Override
    public DropwizardAppRule<ConsentConfiguration> rule() {
        return RULE;
    }

    @Test
    public void testCreateDataSetWrongType() throws Exception {
        Client client = ClientBuilder.newBuilder().register(MultiPartFeature.class).build();
        WebTarget webTarget = client.target(postDataSetFile(false, 1));
        MultiPart mp = createFormData("wrongExt", "pdf");
        mockValidateTokenResponse();
        Response response = webTarget
                .request(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer access_token")
                .post(Entity.entity(mp, mp.getMediaType()));
        ArrayList<String> result = response.readEntity(new GenericType<ArrayList<String>>() {});
        assertTrue(result.size() == 2);
        assertTrue(response.getStatus() == (BAD_REQUEST));
        assertTrue(result.get(0).equals("A problem has occurred while uploading datasets - Contact Support"));
        assertTrue(result.get(1).equals("The file type is not the expected one. Please download the Dataset Spreadsheet Model from the 'Add Datasets' window."));
    }

    @Test
    public void testCreateMissingHeaders() throws Exception {
        // No matter other errors in the file, if the headers doesn't match, it will not try to parse.
        Client client = ClientBuilder.newBuilder().register(MultiPartFeature.class).build();
        WebTarget webTarget = client.target(postDataSetFile(false, 1));
        MultiPart mp = createFormData("missingHeader", "txt");
        mockValidateTokenResponse();
        Response response = webTarget
                .request(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer access_token")
                .post(Entity.entity(mp, mp.getMediaType()));
        ArrayList<String> result = response.readEntity(new GenericType<ArrayList<String>>() {});
        assertTrue(result.size() == 2);
        assertTrue(response.getStatus() == (BAD_REQUEST));
        assertTrue(result.get(0).equals("Your file has more/less columns than expected. Expected quantity: 10"));
        assertTrue(result.get(1).equals("Please download the Dataset Spreadsheet Model from the 'Add Datasets' window."));
    }

    @Test
    public void testCreateCorrectFile() throws Exception {
        Client client = ClientBuilder.newBuilder()
                .register(MultiPartFeature.class).build();
        WebTarget webTarget = client.target(postDataSetFile(true, 1));
        MultiPart mp = createFormData("correctFile", "txt");
        mockValidateTokenResponse();
        Response response = webTarget
                .request(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer access_token")
                .post(Entity.entity(mp, mp.getMediaType()));
        ArrayList<DataSet> result = response.readEntity(new GenericType<ArrayList<DataSet>>(){});
        assertTrue(response.getStatus() == (OK));
        assertTrue(result.size() == 3);
    }

    private MultiPart createFormData(String name, String ext) throws URISyntaxException, IOException {
        MultiPart multiPart = new MultiPart();
        multiPart.setMediaType(MediaType.MULTIPART_FORM_DATA_TYPE);
        String fileName = "dataset/" + name + "." + ext;
        URI uri = Resources.getResource(fileName).toURI();
        File file = new File(uri);
        FileDataBodyPart fileDataBodyPart = new FileDataBodyPart("data", file, MediaType.valueOf("text/plain"));
        multiPart.bodyPart(fileDataBodyPart);
        return multiPart;
    }
}