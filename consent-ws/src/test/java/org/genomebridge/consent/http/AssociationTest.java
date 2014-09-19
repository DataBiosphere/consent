package org.genomebridge.consent.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.genomebridge.consent.http.models.ConsentAssociation;
import org.junit.ClassRule;
import org.junit.Test;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.ArrayList;
import java.lang.System;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.fail;

/**
 * Created by egolin on 9/15/14.
 */
public class AssociationTest extends ConsentServiceTest {
    public static final int OK = ClientResponse.Status.OK.getStatusCode();
    public static final int BAD_REQUEST = ClientResponse.Status.BAD_REQUEST.getStatusCode();

    private static ConsentAssociation buildConsentAssociation(String atype, String... elements) {
        final ArrayList<String> elem_list = new ArrayList<String>();
        for (String elem : elements)
            elem_list.add(elem);
        return new ConsentAssociation(atype, elem_list);
    }

    private String associationPath(String consentId) { return consentPath(consentId) + "/association"; }

    private String associationQueryPath(String consentId, String atype, String id) {
        String assoc_path = associationPath(consentId);
        String query_path;
        if (atype == null && id == null)
            query_path = assoc_path;
        else if (id == null)    // atype != null && id==null
            query_path = String.format("%s?associationType=%s", assoc_path, atype);
        else if (atype != null) // atype!= null && id!= null
            query_path = String.format("%s?associationType=%s&id=%s", assoc_path, atype, id);
        else                    // atype==null && id != null (error condition)
            query_path = String.format("%s?id=%s", assoc_path, id);
        System.out.println(String.format("Generated query path='%s'", query_path));
        return query_path;
    }

    @ClassRule
    public static final DropwizardAppRule<ConsentConfiguration> RULE =
            new DropwizardAppRule<>(ConsentApplication.class,
                    resourceFilePath("consent-config.yml"));


    @Override
    public DropwizardAppRule<ConsentConfiguration> rule() {
        return RULE;
    }

    @Test
    public void testCreateAssociation() {
        Client client = new Client();

        ArrayList<ConsentAssociation> assoc_list = new ArrayList<ConsentAssociation>();
        assoc_list.add(buildConsentAssociation("sample", "SM-1234", "SM-5678"));

        ClientResponse response = checkStatus( OK, post(client, associationPath("dummyid"), assoc_list) );
        String result_json = response.getEntity(String.class);
        try {
            final ObjectMapper MAPPER = Jackson.newObjectMapper();
            assertThat(MAPPER.writeValueAsString(assoc_list)).isEqualTo(result_json);
        } catch (JsonProcessingException e) {
            fail(String.format("Exception thrown processing json '%s'", result_json));
        }
        // String createdLocation = checkHeader(response, "Location");
    }

    @Test
    public void testCreateComplexAssociation() {
        Client client = new Client();

        ArrayList<ConsentAssociation> assoc_list = new ArrayList<ConsentAssociation>();
        assoc_list.add(buildConsentAssociation("sample", "SM-1234", "SM-5678"));
        assoc_list.add(buildConsentAssociation("sampleSet", "SC-9571"));

        ClientResponse response = checkStatus( OK, post(client, associationPath("dummyid"), assoc_list) );
        String result_json = response.getEntity(String.class);
        try {
            final ObjectMapper MAPPER = Jackson.newObjectMapper();
            assertThat(MAPPER.writeValueAsString(assoc_list)).isEqualTo(result_json);
        } catch (JsonProcessingException e) {
            fail(String.format("Exception thrown processing json '%s'", result_json));
        }
        // String createdLocation = checkHeader(response, "Location");
    }

    @Test
    public void testUpdateAssociation() {
        Client client = new Client();

        ArrayList<ConsentAssociation> assoc_list = new ArrayList<ConsentAssociation>();
        assoc_list.add(buildConsentAssociation("sample", "SM-1234", "SM-5678"));

        ClientResponse response = checkStatus( OK, put(client, associationPath("dummyid"), assoc_list) );
        String result_json = response.getEntity(String.class);
        try {
            final ObjectMapper MAPPER = Jackson.newObjectMapper();
            assertThat(MAPPER.writeValueAsString(assoc_list)).isEqualTo(result_json);
        } catch (JsonProcessingException e) {
            fail(String.format("Exception thrown processing json '%s'", result_json));
        }

        // String createdLocation = checkHeader(response, "Location");
    }

    @Test
    public void testGetAssociation() {
        Client client = new Client();

        ArrayList<ConsentAssociation> assoc_list = new ArrayList<ConsentAssociation>();
        assoc_list.add(buildConsentAssociation("sample", "SM-1234", "SM-5678"));

        ClientResponse response;
        // test no query parameters
        response = checkStatus( OK, get(client, associationQueryPath("dummyid", null, null)) );
        // test associationType="sample"
        response = checkStatus( OK, get(client, associationQueryPath("dummyid", "sample", null)) );
        // test associationType="sample"&id="SM-1234"
        response = checkStatus( OK, get(client, associationQueryPath("dummyid", "sample", "SM-1234")) );
        // test id="SM-1234" (error case)
        response = checkStatus( BAD_REQUEST, get(client, associationQueryPath("dummyid", null, "SM-1234")) );


        // String createdLocation = checkHeader(response, "Location");
    }

    @Test
    public void testDeleteAssociation() {
        Client client = new Client();

        ArrayList<ConsentAssociation> assoc_list = new ArrayList<ConsentAssociation>();
        assoc_list.add(buildConsentAssociation("sample", "SM-1234", "SM-5678"));

        ClientResponse response;
        // test no query parameters
        response = checkStatus( OK, delete(client, associationQueryPath("dummyid", null, null)) );
        // test associationType="sample"
        response = checkStatus( OK, delete(client, associationQueryPath("dummyid", "sample", null)) );
        // test associationType="sample"&id="SM-1234"
        response = checkStatus( OK, delete(client, associationQueryPath("dummyid", "sample", "SM-1234")) );
        // test id="SM-1234" (error case)
        response = checkStatus( BAD_REQUEST, delete(client, associationQueryPath("dummyid", null, "SM-1234")) );


        // String createdLocation = checkHeader(response, "Location");
    }

}
