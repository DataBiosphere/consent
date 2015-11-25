package org.genomebridge.consent.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.genomebridge.consent.http.configurations.ConsentConfiguration;
import org.genomebridge.consent.http.models.Consent;
import org.genomebridge.consent.http.models.ConsentAssociation;
import org.genomebridge.consent.http.models.grammar.Everything;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;


/**
 * Consent Association Tests
 * <p/>
 * Created by egolin on 9/15/14.
 */
public class AssociationTest extends ConsentServiceTest {

    @ClassRule
    public static final DropwizardAppRule<ConsentConfiguration> RULE =
            new DropwizardAppRule<>(ConsentApplication.class,
                    resourceFilePath("consent-config.yml"));


    @Override
    public DropwizardAppRule<ConsentConfiguration> rule() {
        return RULE;
    }


    @Before
    public void setup() throws Exception {
        mockTranslateResponse();
    }


    //
    //  TESTS
    //

    @Test
    public void testCreateAssociation() {
        final String consentId = setupConsent();

        Client client = ClientBuilder.newClient();

        List<ConsentAssociation> assoc_list = new ArrayList<>();
        assoc_list.add(buildConsentAssociation("sample", "SM-12345", "SM-56789"));

        Response response = checkStatus(OK, post(client, associationPath(consentId), assoc_list));
        checkAssociations(assoc_list, response);
        String location = checkHeader(response, "Location");
        System.out.println(String.format("*** testCreateAssociation - returned location '%s'", location));
    }

    @Test
    public void testCreateComplexAssociation() {
        final String consentId = setupConsent();

        Client client = ClientBuilder.newClient();

        ArrayList<ConsentAssociation> assoc_list = new ArrayList<>();
        assoc_list.add(buildConsentAssociation("sample", "SM-11234", "SM-55678"));
        assoc_list.add(buildConsentAssociation("sampleSet", "SC-95571"));

        Response response = checkStatus(OK, post(client, associationPath(consentId), assoc_list));
        checkAssociations(assoc_list, response);
        String location = checkHeader(response, "Location");
        System.out.println(String.format("*** testCreateComplexAssociation - returned location '%s'", location));
    }

    @Test
    public void testUpdateAssociation() {
        final String consentId = setupConsent();

        Client client = ClientBuilder.newClient();

        ArrayList<ConsentAssociation> assoc_list1 = new ArrayList<>();
        assoc_list1.add(buildConsentAssociation("sample", "SM-12334"));

        ArrayList<ConsentAssociation> assoc_list2 = new ArrayList<>();
        assoc_list2.add(buildConsentAssociation("sample", "SM-56778"));

        ArrayList<ConsentAssociation> assoc_list3 = new ArrayList<>();
        assoc_list3.add(buildConsentAssociation("sample", "SM-12334", "SM-56778"));

        Response response = checkStatus(OK, put(client, associationPath(consentId), assoc_list1));
        checkAssociations(assoc_list1, response);
        String location = checkHeader(response, "Location");
        System.out.println(String.format("*** testUpdateAssociation - returned location '%s'", location));

        response = checkStatus(OK, put(client, associationPath(consentId), assoc_list2));
        checkAssociations(assoc_list3, response);
        location = checkHeader(response, "Location");
        System.out.println(String.format("*** testUpdateAssociation - returned location '%s'", location));

        response = checkStatus(OK, put(client, associationPath(consentId), assoc_list1));
        checkAssociations(assoc_list3, response);
        location = checkHeader(response, "Location");
        System.out.println(String.format("*** testUpdateAssociation - returned location '%s'", location));

        ArrayList<ConsentAssociation> assoc_list4 = new ArrayList<>();
        assoc_list4.add(buildConsentAssociation("sampleSet", "SC-95711"));
        assoc_list3.add(assoc_list4.get(0));
        response = checkStatus(OK, put(client, associationPath(consentId), assoc_list4));
        checkAssociations(assoc_list3, response);
        location = checkHeader(response, "Location");
        System.out.println(String.format("*** testUpdateAssociation - returned location '%s'", location));
    }

    @Test
    public void testGetAssociation() {
        final String consentId = setupConsent();

        String controlSample = "SM-"+Math.random();
        Client client = ClientBuilder.newClient();

        List<ConsentAssociation> assoc_list = new ArrayList<>();
        assoc_list.add(buildConsentAssociation("sample", controlSample , "SM-"+Math.random() ));

        Response response = checkStatus(OK, post(client, associationPath(consentId), assoc_list));
        checkAssociations(assoc_list, response);
        String location = checkHeader(response, "Location");
        System.out.println(String.format("*** testGetAssociation - returned location '%s'", location));

        // test no query parameters
        response = checkStatus(OK, getJson(client, associationQueryPath(consentId, null, null)));
        checkAssociations(assoc_list, response);
        location = checkHeader(response, "Location");
        System.out.println(String.format("*** testGetAssociation - returned location '%s'", location));

        // test associationType="sample"
        response = checkStatus(OK, getJson(client, associationQueryPath(consentId, "sample", null)));
        checkAssociations(assoc_list, response);
        location = checkHeader(response, "Location");
        System.out.println(String.format("*** testGetAssociation - returned location '%s'", location));

        // test associationType="sample"&id="SM-1234"
        response = checkStatus(OK, getJson(client, associationQueryPath(consentId, "sample", controlSample)));
        List<ConsentAssociation> singleSample = new ArrayList<>();
        singleSample.add(buildConsentAssociation("sample", controlSample));
        checkAssociations(singleSample, response);
        location = checkHeader(response, "Location");
        System.out.println(String.format("*** testGetAssociation - returned location '%s'", location));

        // test id="SM-1234" (error case)
        checkStatus(BAD_REQUEST, getJson(client, associationQueryPath(consentId, null, controlSample)));
    }

    @Test
    public void testDeleteAssociationByTypeAndObject() {
        final String consentId = setupConsent();

        Client client = ClientBuilder.newClient();

        ArrayList<ConsentAssociation> assoc_list1 = new ArrayList<>();
        assoc_list1.add(buildConsentAssociation("sample", "AMD-12354", "AME-56789"));
        assoc_list1.add(buildConsentAssociation("sampleSet", "ACE-9571"));

        Response response = checkStatus(OK, post(client, associationPath(consentId), assoc_list1));
        checkAssociations(assoc_list1, response);

        response = checkStatus(OK, delete(client, associationQueryPath(consentId, "sample", "AMD-12354")));

        ArrayList<ConsentAssociation> assoc_list2 = new ArrayList<>();
        assoc_list2.add(buildConsentAssociation("sample", "AME-56789"));
        assoc_list2.add(buildConsentAssociation("sampleSet", "ACE-9571"));
        checkAssociations(assoc_list2, response);
        String location = checkHeader(response, "Location");
        System.out.println(String.format("*** testDeleteAssociationByTypeAndObject - returned location '%s'", location));
    }

    @Test
    public void testDeleteAssociationByType() {
        final String consentId = setupConsent();

        Client client = ClientBuilder.newClient();

        ArrayList<ConsentAssociation> assoc_list1 = new ArrayList<>();
        assoc_list1.add(buildConsentAssociation("sample", "SME-1234", "SMT-5678"));
        assoc_list1.add(buildConsentAssociation("sampleSet", "SCE-9571"));

        Response response = checkStatus(OK, post(client, associationPath(consentId), assoc_list1));
        checkAssociations(assoc_list1, response);

        response = checkStatus(OK, delete(client, associationQueryPath(consentId, "sample", null)));

        ArrayList<ConsentAssociation> assoc_list2 = new ArrayList<>();
        assoc_list2.add(buildConsentAssociation("sampleSet", "SCE-9571"));
        checkAssociations(assoc_list2, response);
        String location = checkHeader(response, "Location");
        System.out.println(String.format("*** testDeleteAssociationByType - returned location '%s'", location));
    }

    @Test
    public void testDeleteAssociationAll() {
        final String consentId = setupConsent();

        Client client = ClientBuilder.newClient();

        ArrayList<ConsentAssociation> assoc_list1 = new ArrayList<>();
        assoc_list1.add(buildConsentAssociation("sample", "SE-1234", "SE-5678"));
        assoc_list1.add(buildConsentAssociation("sampleSet", "SE-9571"));

        Response response = checkStatus(OK, post(client, associationPath(consentId), assoc_list1));
        checkAssociations(assoc_list1, response);

        response = checkStatus(OK, delete(client, associationQueryPath(consentId, null, null)));

        ArrayList<ConsentAssociation> assoc_list2 = new ArrayList<>();
        checkAssociations(assoc_list2, response);
        String location = checkHeader(response, "Location");
        System.out.println(String.format("*** testDeleteAssociationAll - returned location '%s'", location));
    }

    @Test
    public void testDeleteAssociationError() {
        final String consentId = setupConsent();

        Client client = ClientBuilder.newClient();

        ArrayList<ConsentAssociation> assoc_list1 = new ArrayList<>();
        assoc_list1.add(buildConsentAssociation("sample", "SF-1234", "SF-5678"));
        assoc_list1.add(buildConsentAssociation("sampleSet", "SF-9571"));

        Response response = checkStatus(OK, post(client, associationPath(consentId), assoc_list1));
        checkAssociations(assoc_list1, response);

        checkStatus(BAD_REQUEST, delete(client, associationQueryPath(consentId, null, "SF-1234")));
    }

    @Test
    public void testQueryConsentByAssociation() {
        // first we need to set up some consents and associations
        final String consentId1 = setupConsent();
        final String consentId2 = setupConsent();
        final String s1 = "AO-1234";
        final String s2 = "BS-1234";
        final String s3 = "DU-1234";
        final String s4 = "EH-1234";
        System.out.println(String.format("*** testQueryByAssociation, generated ids: '%s', '%s', '%s', '%s'", s1, s2, s3, s4));

        Client client = ClientBuilder.newClient();

        List<ConsentAssociation> assoc_list = new ArrayList<>();
        assoc_list.add(buildConsentAssociation("sample", s1, s2, s3));
        Response response = checkStatus(OK, post(client, associationPath(consentId1), assoc_list));
        checkAssociations(assoc_list, response);
        System.out.println(String.format("*** testQueryConsentByAssociation ***:  created consent 1: '%s'", consentId1));

        assoc_list.clear();
        assoc_list.add(buildConsentAssociation("sample", s4));
        response = checkStatus(OK, post(client, associationPath(consentId2), assoc_list));
        checkAssociations(assoc_list, response);
        System.out.println(String.format("*** testQueryConsentByAssociation ***:  created consent 2: '%s'", consentId2));

        String qpath = queryAssociationPath("sample", s2);
        response = checkStatus(OK, getJson(client, qpath));

        // check that we got back both consents
        ArrayList<String> consent_urls = getConsentUrls(response);
        assertThat(consent_urls.size()).isEqualTo(1);
        String location = checkHeader(response, "Location");
        System.out.println(String.format("*** testQueryByAssociation(1) - returned location '%s'", location));

        // check that we got back just consent1
        response = checkStatus(OK, getJson(client, queryAssociationPath("sample", s1)));
        consent_urls = getConsentUrls(response);
        assertThat(consent_urls.size()).isEqualTo(1);
        location = checkHeader(response, "Location");
        System.out.println(String.format("*** testQueryByAssociation(2) - returned location '%s'", location));

        // check that we got back no consents
        response = checkStatus(OK, getJson(client, queryAssociationPath("sample", "TST-$$$$")));
        consent_urls = getConsentUrls(response);
        assertThat(consent_urls.size()).isEqualTo(0);

        // check that we got back no consents
        response = checkStatus(OK, getJson(client, queryAssociationPath("sampleSet", s2)));
        consent_urls = getConsentUrls(response);
        assertThat(consent_urls.size()).isEqualTo(0);
    }

    //
    //  HELPER METHODS
    //

    private static ConsentAssociation buildConsentAssociation(String atype, String... elements) {
        final ArrayList<String> elem_list = new ArrayList<>();
        Collections.addAll(elem_list, elements);
        return new ConsentAssociation(atype, elem_list);
    }

    private String associationPath(String consentId) {
        return consentPath(consentId) + "/association";
    }

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

    private void checkAssociations(List<ConsentAssociation> assoc_list, Response response) {
        String result_json = response.readEntity(String.class);
        try {
            final ObjectMapper MAPPER = Jackson.newObjectMapper();
            assertThat(result_json).isEqualTo(MAPPER.writeValueAsString(assoc_list));
        } catch (JsonProcessingException e) {
            fail(String.format("Exception thrown processing json '%s'", result_json));
        }
    }

    private ArrayList<String> getConsentUrls(Response response) {
        ArrayList<String> urls = null;
        String result_json = response.readEntity(String.class);
        try {
            final ObjectMapper MAPPER = Jackson.newObjectMapper();
            TypeFactory tf = TypeFactory.defaultInstance();
            urls = MAPPER.readValue(result_json, tf.constructCollectionType(ArrayList.class, String.class));
            System.out.println(String.format("*** Got json back: '%s' and converted to '%s'", result_json, urls.toString()));
        } catch (Exception e) {
            System.out.println(String.format("+++Exception ('%s') caugh processing JSON result", e.getMessage()));
        }
        return urls;
    }

    private static Random _rand = new Random();

    private String genSampleId() {
        int id_int = _rand.nextInt(9000) + 1000;
        return String.format("TST-%4d", id_int);
    }

    private String queryAssociationPath(String atype, String id) {
        final String path = String.format("%s/associations/%s/%s", consentPath(), atype, id);
        System.out.println("*** queryAssociationPath = " + path);
        return path;
    }

    private String setupConsent() {
        Client client = ClientBuilder.newClient();
        Consent rec = new Consent(false, new Everything(), UUID.randomUUID().toString());
        Response response = checkStatus( CREATED, post(client, consentPath(), rec) );
        String createdLocation = checkHeader(response, "Location");
        String consent_id = createdLocation.substring(createdLocation.lastIndexOf("/") + 1);
        System.out.println(String.format("setupConsent created consent with id '%s' at location '%s'", createdLocation, consent_id));
        return consent_id;
    }
}
