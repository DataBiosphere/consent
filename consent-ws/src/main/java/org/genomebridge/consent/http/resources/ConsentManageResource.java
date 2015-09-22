package org.genomebridge.consent.http.resources;

import org.genomebridge.consent.http.service.AbstractConsentAPI;
import org.genomebridge.consent.http.service.ConsentAPI;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Path("{api : (api/)?}consent/manage")
public class ConsentManageResource extends Resource {

    private final ConsentAPI api;

    public ConsentManageResource() {
        this.api = AbstractConsentAPI.getInstance();
    }

    @GET
    public Response getConsentManage() {
        return Response.ok(api.describeConsentManage())
                .build();
    }

}
