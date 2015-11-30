package org.broadinstitute.consent.http;

import org.broadinstitute.consent.http.models.DACUser;

import javax.ws.rs.client.Client;

public abstract class DACUserServiceTest extends AbstractTest {

    public String dacUserPath() {
        return path2Url("/dacuser");
    }

    public DACUser retrieveDacUser(Client client, String url) {
        return getJson(client, url).readEntity(DACUser.class);
    }

    public String dacUserPathByEmail(String email) {
        return path2Url(String.format("/dacuser/%s", email));
    }

    public String dacUserPathById(Integer id) {
        return path2Url(String.format("/dacuser/%s", id));
    }

}
