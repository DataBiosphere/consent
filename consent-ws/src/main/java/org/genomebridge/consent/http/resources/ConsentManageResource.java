package org.genomebridge.consent.http.resources;

import org.genomebridge.consent.http.service.AbstractConsentAPI;
import org.genomebridge.consent.http.service.ConsentAPI;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Path("{api : (api/)?}consent")
public class ConsentManageResource extends Resource {

    private final ConsentAPI api;

    public ConsentManageResource() {
        this.api = AbstractConsentAPI.getInstance();
    }

    @GET
    @Path("/manage")
    public Response getConsentManage() {
        return Response.ok(api.describeConsentManage())
                .build();
    }

    @GET
    @Path("/unreviewed")
    public Response getTotalUnreviewedConsent() {
        return Response.ok("{\"dulUnReviewedCases\":"+api.getUnReviewedConsents()+"}").build();
    }


}
