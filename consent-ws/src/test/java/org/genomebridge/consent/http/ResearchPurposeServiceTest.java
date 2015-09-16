package org.genomebridge.consent.http;

import org.genomebridge.consent.http.models.DataRequest;
import org.genomebridge.consent.http.models.ResearchPurpose;

import javax.ws.rs.client.Client;

public abstract class ResearchPurposeServiceTest extends AbstractTest {

    public String purposePath() {
        return path2Url("/purpose");
    }

    public ResearchPurpose retrieveResearchPurpose(Client client, String url) {
        return getJson(client, url).readEntity(ResearchPurpose.class);
    }

    public String researchPurposePathById(String id) {
        return path2Url(String.format("/purpose/%s", id));
    }

}
