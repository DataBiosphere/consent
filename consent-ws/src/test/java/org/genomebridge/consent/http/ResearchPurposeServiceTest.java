package org.genomebridge.consent.http;

import org.bson.Document;
import org.genomebridge.consent.http.models.Consent;
import org.genomebridge.consent.http.models.Match;
import org.genomebridge.consent.http.models.ResearchPurpose;

import javax.ws.rs.client.Client;

public abstract class ResearchPurposeServiceTest extends AbstractTest {

    public String purposePath() {
        return path2Url("/purpose");
    }

    public String accessPath() {
        return path2Url("/dar");
    }

    public String accessPathById(String id) {
        return path2Url(String.format("/dar/%s", id));
    }

    public ResearchPurpose retrieveResearchPurpose(Client client, String url) {
        return getJson(client, url).readEntity(ResearchPurpose.class);
    }

    public Document retrieveAccess(Client client, String url) {
        return getJson(client, url).readEntity(Document.class);
    }

    public String researchPurposePathById(String id) {
        return path2Url(String.format("/purpose/%s", id));
    }


    public String matchPath(){
        return path2Url("/match");
    }

    public String matchPathById(Integer id){
        return path2Url(String.format("/match/%s", id));
    }

    public Match retrieveMatch(Client client, String url) {
        return getJson(client, url).readEntity(Match.class);
    }

    public String consentPath() {
        return path2Url("/consent");
    }

    public Consent retrieveConsent(Client client, String url) {
        return getJson(client, url).readEntity(Consent.class);
    }



}
