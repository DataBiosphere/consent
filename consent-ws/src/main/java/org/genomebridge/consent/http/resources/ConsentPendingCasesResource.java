package org.genomebridge.consent.http.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.genomebridge.consent.http.service.AbstractPendingCaseAPI;
import org.genomebridge.consent.http.service.PendingCaseAPI;

@Path("/consent/pendingCases/{dacUserId}")
public class ConsentPendingCasesResource extends Resource {

    private PendingCaseAPI api;

    public ConsentPendingCasesResource() {
        this.api = AbstractPendingCaseAPI.getInstance();
    }

    @GET
    public Response getConsentPendingCases(@PathParam("dacUserId") Integer dacUserId) {
        return Response.ok(api.describeConsentPendingCases(dacUserId))               
                       .build();
     }

}
