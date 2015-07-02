package org.genomebridge.consent.http;

import static org.fest.assertions.api.Assertions.assertThat;

import io.dropwizard.testing.junit.DropwizardAppRule;

import org.genomebridge.consent.http.models.DataRequest;
import org.junit.ClassRule;
import org.junit.Test;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;

public class DataRequestTest extends DataRequestServiceTest {

    public static final int CREATED = ClientResponse.Status.CREATED
            .getStatusCode();
    public static final int OK = ClientResponse.Status.OK.getStatusCode();
    public static final int BADREQUEST = ClientResponse.Status.BAD_REQUEST
            .getStatusCode();
    public static final int NOT_FOUND = ClientResponse.Status.NOT_FOUND.getStatusCode();
    private static final Integer ID = 1;
    private static final Integer ID_2 = 2;
    private static final String DESCRIPTION = "TestDescription";
    private static final String RESEARCHER = "ResearcherTest";
    private static final Integer INVALID_DATA_REQUEST_ID = 584264;


    @ClassRule
    public static final DropwizardAppRule<ConsentConfiguration> RULE = new DropwizardAppRule<>(
            ConsentApplication.class, resourceFilePath("consent-config.yml"));

    @Override
    public DropwizardAppRule<ConsentConfiguration> rule() {
        return RULE;
    }

    @Test
    public void testCreateDataRequest() {
        Client client = new Client();
        DataRequest dataRequest = new DataRequest();
        dataRequest.setDataSetId(ID);
        dataRequest.setPurposeId(ID);
        dataRequest.setDescription(DESCRIPTION);
        dataRequest.setResearcher(RESEARCHER);
        ClientResponse response = checkStatus(CREATED,
                post(client, dataRequestPath(), dataRequest));
        String createdLocation = checkHeader(response, "Location");
        DataRequest created = retrieveDataRequest(client, createdLocation);
        assertThat(created.getDataSetId()).isEqualTo(ID);
        assertThat(created.getDescription()).isEqualTo(DESCRIPTION);
        assertThat(created.getPurposeId()).isEqualTo(ID);
        assertThat(created.getResearcher()).isEqualTo(RESEARCHER);
        assertThat(created.getRequestId()).isNotNull();
        testUpdateDataRequest(created);
        deleteDataRequest(created.getRequestId());
    }

    public void testUpdateDataRequest(DataRequest created) {
        Client client = new Client();
        created.setDataSetId(ID_2);
        created.setDescription(DESCRIPTION + "DR");
        checkStatus(OK, put(client, dataRequestPathById(created.getRequestId()), created));
        created = retrieveDataRequest(client, dataRequestPathById(created.getRequestId()));
        assertThat(created.getDataSetId()).isEqualTo(ID_2);
        assertThat(created.getDescription()).isEqualTo(DESCRIPTION + "DR");
        assertThat(created.getPurposeId()).isEqualTo(ID);
        assertThat(created.getResearcher()).isEqualTo(RESEARCHER);
        assertThat(created.getRequestId()).isNotNull();

    }

    public void deleteDataRequest(Integer id) {
        Client client = new Client();
        checkStatus(OK,
                delete(client, dataRequestPathById(id)));
    }

    @Test
    public void retrieveDataRequestWithInvalidId() {
        Client client = new Client();
        checkStatus(NOT_FOUND,
                get(client, dataRequestPathById(INVALID_DATA_REQUEST_ID)));
    }

    @Test
    public void testDataRequestWithNullRequieredFields() {
        Client client = new Client();
        DataRequest dataRequest = new DataRequest();
        dataRequest.setPurposeId(ID);
        dataRequest.setDescription(DESCRIPTION);
        dataRequest.setResearcher(RESEARCHER);
        // should return 400 bad request because the data request id does not exist
        checkStatus(BAD_REQUEST,
                post(client, dataRequestPath(), dataRequest));
    }

    @Test
    public void testUpdateDataRequestWithInvalidId() {
        Client client = new Client();
        DataRequest dataRequest = new DataRequest();
        // should return 400 bad request because the data request id does not exist
        checkStatus(NOT_FOUND,
                put(client, dataRequestPathById(INVALID_DATA_REQUEST_ID), dataRequest));
    }


}
