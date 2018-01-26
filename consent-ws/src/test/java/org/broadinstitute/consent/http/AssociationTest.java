package org.broadinstitute.consent.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.broadinstitute.consent.http.configurations.ConsentConfiguration;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.models.*;
import org.broadinstitute.consent.http.models.dto.WorkspaceAssociationDTO;
import org.broadinstitute.consent.http.models.grammar.Everything;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.Assert.assertTrue;


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
        mockValidateResponse();
    }


    //
    //  TESTS
    //

    @Test
    public void testCreateAssociation() throws IOException {
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
    public void testCreateComplexAssociation() throws IOException {
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
    public void testUpdateAssociation() throws IOException {
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
    public void testGetAssociation() throws IOException {
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
    public void testDeleteAssociationByTypeAndObject() throws IOException {
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
    public void testDeleteAssociationByType() throws IOException {
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
    public void testDeleteAssociationAll() throws IOException {
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
    public void testDeleteAssociationError() throws IOException {
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
    public void testQueryConsentByAssociation() throws IOException {
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
        checkStatus(NOT_FOUND, getJson(client, queryAssociationPath("sample", "TST-$$$$")));
        // check that we got back no consents
        checkStatus(NOT_FOUND, getJson(client, queryAssociationPath("sampleSet", s2)));
    }

    private static String WORKSPACE_TYPE = "workspace";
    private static String NON_EXISTENT_WORKSPACE = UUID.randomUUID().toString();
    private static String TO_UPDATE_WORKSPACE = UUID.randomUUID().toString();
    private static String TO_CREATE_CONSENT = "testId2";
    private static String TO_UPDATE_CONSENT = "testId3";

    private static String WORKSPACE_URL = "workspace/%s";

    @Test
    public void testPostWorkspaceAssociation() throws IOException {
        Client client = ClientBuilder.newClient();
        final ConsentAssociation consent_association = buildConsentAssociation(WORKSPACE_TYPE, NON_EXISTENT_WORKSPACE);
        checkStatus(OK, post(client, associationPath(TO_CREATE_CONSENT), Arrays.asList(consent_association)));
        testGetWorkspaceAssociation(NON_EXISTENT_WORKSPACE, TO_CREATE_CONSENT);
        testGetWorkspaceAssociationWithElections(NON_EXISTENT_WORKSPACE, TO_CREATE_CONSENT);
        checkStatus(CONFLICT, post(client, associationPath(TO_CREATE_CONSENT), Arrays.asList(consent_association)));
    }

    /** Workspace associations can't be updated, no matter the association values! **/
    @Test
    public void testPutWorkspaceAssociation() throws IOException {
        Client client = ClientBuilder.newClient();
        final ConsentAssociation consent_association = buildConsentAssociation(WORKSPACE_TYPE, TO_UPDATE_WORKSPACE);
        checkStatus(CONFLICT, put(client, associationPath(TO_UPDATE_CONSENT), Arrays.asList(consent_association)));
        List<ConsentAssociation> created = retrieveAssociations(client, associationPath(TO_UPDATE_CONSENT));
        System.out.println(created.toString());
    }

    //
    //  HELPER METHODS
    //

    private void testGetWorkspaceAssociation(String workspaceId, String consentId) throws IOException {
        Client client = ClientBuilder.newClient();
        Response response = checkStatus(OK, getJson(client, path2Url(String.format(WORKSPACE_URL, workspaceId))));
        WorkspaceAssociationDTO workspaceAssociation = response.readEntity(WorkspaceAssociationDTO.class);
        assertTrue(workspaceAssociation.getConsent() != null);
        assertTrue(workspaceAssociation.getConsent().getConsentId().equals(consentId));
    }

    private void testGetWorkspaceAssociationWithElections(String workspaceId, String consentId) throws IOException {
        Election election = createElection(consentId);
        Client client = ClientBuilder.newClient();
        Response response = checkStatus(OK, getJson(client, path2Url(String.format(WORKSPACE_URL, workspaceId))));
        WorkspaceAssociationDTO workspaceAssociation = response.readEntity(WorkspaceAssociationDTO.class);
        assertTrue(workspaceAssociation.getConsent() != null);
        assertTrue(workspaceAssociation.getConsent().getConsentId().equals(consentId));
        assertTrue(workspaceAssociation.getElectionStatus().get(1).getElectionStatus().equals(ElectionStatus.OPEN.getValue()));
        deleteElection(election.getElectionId(), consentId);
    }

    private List<ConsentAssociation> retrieveAssociations(Client client, String url) throws IOException {
        return getJson(client, url).readEntity(new GenericType<List<ConsentAssociation>>(){});
    }

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

    private String setupConsent() throws IOException {
        Client client = ClientBuilder.newClient();
        DataUseDTO dataUse = new DataUseBuilder().setGeneralUse(true).build();
        Consent rec = new ConsentBuilder().
                setRequiresManualReview(false).
                setUseRestriction(new Everything()).
                setDataUse(dataUse).
                setName(UUID.randomUUID().toString()).
                build();
        Response response = checkStatus( CREATED, post(client, consentPath(), rec) );
        String createdLocation = checkHeader(response, "Location");
        String consent_id = createdLocation.substring(createdLocation.lastIndexOf("/") + 1);
        System.out.println(String.format("setupConsent created consent with id '%s' at location '%s'", createdLocation, consent_id));
        return consent_id;
    }

    // Create elections - Workspace associations related code

    public Election createElection(String consentId) throws IOException {
        Client client = ClientBuilder.newClient();
        Election election = new Election();
        election.setStatus(ElectionStatus.OPEN.getValue());
        election.setElectionType(ElectionType.TRANSLATE_DUL.getValue());
        Response response = checkStatus(CREATED, post(client, electionConsentPath(consentId), election));
        String createdLocation = checkHeader(response, "Location");
        return retrieveElection(client, createdLocation);
    }

    public String electionConsentPath(String id) {
        try {
            return path2Url(String.format("consent/%s/election", URLEncoder.encode(id, "UTF-8")));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace(System.err);
            return String.format("consent/%s/election", id);
        }
    }

    public Election retrieveElection(Client client, String url) throws IOException {
        return getJson(client, url).readEntity(Election.class);
    }

    public void deleteElection(Integer electionId, String consentId) throws IOException {
        Client client = ClientBuilder.newClient();
        List<Vote> votes = getJson(client, voteConsentPath(consentId)).readEntity(new GenericType<List<Vote>>() {
        });
        for (Vote vote : votes) {
            checkStatus(OK,
                    delete(client, voteConsentIdPath(consentId, vote.getVoteId())));
        }
        checkStatus(OK,
                delete(client, electionConsentPathById(consentId, electionId)));

    }

    public String voteConsentPath(String consentId) {
        try {
            return path2Url(String.format("consent/%s/vote", URLEncoder.encode(consentId, "UTF-8")));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace(System.err);
            return String.format("consent/%s/vote", consentId);
        }
    }

    public String voteConsentIdPath(String consentId, Integer voteId) {
        try {
            return path2Url(String.format("consent/%s/vote/%s", URLEncoder.encode(consentId, "UTF-8"), voteId));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace(System.err);
            return String.format("consent/%s/vote/%s", consentId, voteId);
        }
    }

    public String electionConsentPathById(String referenceId, Integer electionId) {
        try {
            return path2Url(String.format("consent/%s/election/%s", URLEncoder.encode(referenceId, "UTF-8"), electionId));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace(System.err);
            return String.format("consent/%s/election/%s", referenceId, electionId);
        }
    }

}