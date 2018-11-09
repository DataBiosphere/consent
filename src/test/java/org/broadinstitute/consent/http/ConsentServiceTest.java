package org.broadinstitute.consent.http;

import org.broadinstitute.consent.http.models.Consent;

import javax.ws.rs.client.Client;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public abstract class ConsentServiceTest extends AbstractTest {


    public String consentPath() {
        return path2Url("/consent");
    }

    public String consentPath(String id) {
        try {
            return path2Url(String.format("consent/%s", URLEncoder.encode(id, "UTF-8")));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace(System.err);
            return String.format("consent/%s", id);
        }
    }

    public String electionConsentPath(String id) {
        try {
            return path2Url(String.format("consent/%s/election", URLEncoder.encode(id, "UTF-8")));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace(System.err);
            return String.format("consent/%s/election", id);
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

    public String voteConsentIdPath(String consentId, Integer voteId) {
        try {
            return path2Url(String.format("consent/%s/vote/%s", URLEncoder.encode(consentId, "UTF-8"), voteId));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace(System.err);
            return String.format("consent/%s/vote/%s", consentId, voteId);
        }
    }

    public String voteConsentPath(String consentId) {
        try {
            return path2Url(String.format("consent/%s/vote", URLEncoder.encode(consentId, "UTF-8")));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace(System.err);
            return String.format("consent/%s/vote", consentId);
        }
    }

    public Consent retrieveConsent(Client client, String url) throws IOException {
        return getJson(client, url).readEntity(Consent.class);
    }


}
