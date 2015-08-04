package org.genomebridge.consent.http;

import com.sun.jersey.api.client.Client;
import org.genomebridge.consent.http.models.Consent;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public abstract class ConsentServiceTest extends AbstractTest {

    public String consentPath() { return path2Url("/consent"); }

    public String consentPath(String id) {
        try {
            return path2Url(String.format("/consent/%s", URLEncoder.encode(id, "UTF-8")));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace(System.err);
            return String.format("/consent/%s", id);
        }
    }

    public Consent retrieveConsent(Client client, String url) {
        return get(client, url).getEntity(Consent.class);
    }

}
