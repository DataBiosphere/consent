package org.broadinstitute.consent.http;


import org.broadinstitute.consent.http.models.ApprovalExpirationTime;
import javax.ws.rs.client.Client;
import java.io.IOException;


public abstract class ApprovalExpirationTimeServiceTest extends AbstractTest {

    public String approvalExpirationTimePath() {
        return path2Url("/approvalExpirationTime");
    }

    public String approvalExpirationTimePath(Integer id) {
        return path2Url(String.format("approvalExpirationTime/%s", id));
    }

    public ApprovalExpirationTime retrieveApprovalExpirationTime(Client client, String url) throws IOException {
        return getJson(client, url).readEntity(ApprovalExpirationTime.class);
    }

}
