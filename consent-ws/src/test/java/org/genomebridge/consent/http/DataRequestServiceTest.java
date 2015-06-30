package org.genomebridge.consent.http;

import org.genomebridge.consent.http.models.DataRequest;

import com.sun.jersey.api.client.Client;

public abstract class DataRequestServiceTest extends AbstractTest {

    public String dataRequestPath() {
         return path2Url("/dataRequest");
    }

    public DataRequest retrieveDataRequest(Client client, String url) {
        return get(client, url).getEntity(DataRequest.class);
    }

    public String dataRequestPathById(Integer id) {
       return path2Url(String.format("/dataRequest/%s", id));
    }

 

}
