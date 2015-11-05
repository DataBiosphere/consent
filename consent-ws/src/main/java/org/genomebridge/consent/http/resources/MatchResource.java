package org.genomebridge.consent.http.resources;


import org.genomebridge.consent.http.service.*;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;


@Path("{api : (api/)?}match")
public class MatchResource extends Resource {

    private final MatchAPI api;

    public MatchResource() {
        this.api = AbstractMatchAPI.getInstance();
    }

    @GET
    @Path("/{consentId}/{purposeId}")
    public Response getMatchByConsentAndPurpose(@PathParam("consentId") String consentId, @PathParam("purposeId") String purposeId) {
            return Response.ok(api.findMatchByConsentIdAndPurposeId(consentId, purposeId))
                    .build();
    }


}