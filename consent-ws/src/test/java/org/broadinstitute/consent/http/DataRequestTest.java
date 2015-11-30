package org.broadinstitute.consent.http;

import org.broadinstitute.consent.http.ConsentApplication;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.broadinstitute.consent.http.configurations.ConsentConfiguration;
import org.broadinstitute.consent.http.models.DataRequest;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import static org.assertj.core.api.Assertions.assertThat;

public class DataRequestTest extends DataRequestServiceTest {

    public static final int CREATED = Response.Status.CREATED.getStatusCode();
    public static final int OK = Response.Status.OK.getStatusCode();
    public static final int NOT_FOUND = Response.Status.NOT_FOUND.getStatusCode();
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
        Client client = ClientBuilder.newClient();
        DataRequest dataRequest = new DataRequest();
        dataRequest.setDataSetId(ID);
        dataRequest.setPurposeId(ID);
        dataRequest.setDescription(DESCRIPTION);
        dataRequest.setResearcher(RESEARCHER);
        Response response = checkStatus(CREATED,
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
        Client client = ClientBuilder.newClient();
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
        Client client = ClientBuilder.newClient();
        checkStatus(OK,
                delete(client, dataRequestPathById(id)));
    }

    @Test
    public void retrieveDataRequestWithInvalidId() {
        Client client = ClientBuilder.newClient();
        checkStatus(NOT_FOUND,
                getJson(client, dataRequestPathById(INVALID_DATA_REQUEST_ID)));
    }

    @Test
    public void testDataRequestWithNullRequieredFields() {
        Client client = ClientBuilder.newClient();
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
        Client client = ClientBuilder.newClient();
        DataRequest dataRequest = new DataRequest();
        // should return 400 bad request because the data request id does not exist
        checkStatus(NOT_FOUND,
                put(client, dataRequestPathById(INVALID_DATA_REQUEST_ID), dataRequest));
    }


}
