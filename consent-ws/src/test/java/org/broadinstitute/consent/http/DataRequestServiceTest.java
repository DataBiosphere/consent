package org.broadinstitute.consent.http;

import org.broadinstitute.consent.http.models.DataRequest;

import javax.ws.rs.client.Client;

public abstract class DataRequestServiceTest extends AbstractTest {

    public String dataRequestPath() {
        return path2Url("/dataRequest");
    }

    public DataRequest retrieveDataRequest(Client client, String url) {
        return getJson(client, url).readEntity(DataRequest.class);
    }

    public String dataRequestPathById(Integer id) {
        return path2Url(String.format("/dataRequest/%s", id));
    }

}
