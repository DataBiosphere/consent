package org.genomebridge.consent.http;

import org.genomebridge.consent.http.models.Consent;

import javax.ws.rs.client.Client;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public abstract class ConsentServiceTest extends AbstractTest {

    public String consentPath() {
        return path2Url("/consent");
    }

    public String consentPath(String id) {
        try {
            return path2Url(String.format("/consent/%s", URLEncoder.encode(id, "UTF-8")));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace(System.err);
            return String.format("/consent/%s", id);
        }
    }

    public Consent retrieveConsent(Client client, String url) {
        return getJson(client, url).readEntity(Consent.class);
    }

}
