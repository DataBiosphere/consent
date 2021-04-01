package org.broadinstitute.consent.http.resources;


import com.google.inject.Inject;
import org.broadinstitute.consent.http.service.MatchService;

import javax.annotation.security.PermitAll;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;


@Path("api/match")
public class MatchResource extends Resource {

    private final MatchService service;

    @Inject
    public MatchResource(MatchService matchService) {
        this.service = matchService;
    }

    @GET
    @Path("/{consentId}/{purposeId}")
    @PermitAll
    public Response getMatchByConsentAndPurpose(@PathParam("consentId") String consentId, @PathParam("purposeId") String purposeId) {
            return Response.ok(service.findMatchByConsentIdAndPurposeId(consentId, purposeId))
                    .build();
    }


}