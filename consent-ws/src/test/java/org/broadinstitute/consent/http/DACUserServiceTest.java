package org.broadinstitute.consent.http;

import org.broadinstitute.consent.http.models.DACUser;

import javax.ws.rs.client.Client;
import java.io.IOException;

public abstract class DACUserServiceTest extends AbstractTest {

    public String dacUserPath() {
        return path2Url("/dacuser");
    }

    public DACUser retrieveDacUser(Client client, String url) throws IOException {
        return getJson(client, url).readEntity(DACUser.class);
    }

    public String dacUserPathByEmail(String email) {
        return path2Url(String.format("/dacuser/%s", email));
    }

    public String dacUserPathById(Integer id) {
        return path2Url(String.format("/dacuser/%s", id));
    }

    public String validateDelegationPath(String role) {
        return path2Url(String.format("dacuser/validateDelegation?role=" + role));
    }

    public String statusValue(Integer userId ) {
        return path2Url(String.format("dacuser/status/%s", userId));
    }

}
