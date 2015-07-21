package org.genomebridge.consent.http;

import com.sun.jersey.api.client.Client;
import org.genomebridge.consent.http.models.DACUser;

public abstract class DACUserServiceTest extends AbstractTest{

    public String dacUserPath() {
        return path2Url("/dacuser");
    }

    public DACUser retrieveDacUser(Client client, String url) {
        return get(client, url).getEntity(DACUser.class);
    }

    public String dacUserPathByEmail(String email) {
        return path2Url(String.format("/dacuser/%s", email));
    }


}
