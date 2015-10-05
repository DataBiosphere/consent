package org.genomebridge.consent.http.resources;

import org.genomebridge.consent.http.models.Match;
import org.genomebridge.consent.http.service.AbstractMatchingServiceAPI;
import org.genomebridge.consent.http.service.MatchingServiceAPI;
import org.genomebridge.consent.http.service.UnknownIdentifierException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.util.List;

@Path("{api : (api/)?}matching")
public class MatchingResource {

    private final MatchingServiceAPI api;

    public MatchingResource(){
        this.api = AbstractMatchingServiceAPI.getInstance();
    }

    // single research purpose and single consent
    @GET
    @Path("/single/{consentId}/{purposeId}")
    @Produces("application/json")
    public Response getSingleMatch(@PathParam("consentId") String consentId, @PathParam("purposeId") String purposeId){
        try {
            Match match = api.findSingleMatch(consentId, purposeId);
            return Response.status(Response.Status.OK).entity(match).build();
        } catch (IOException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        } catch (UnknownIdentifierException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    // single consent across a collection of purposes
    @GET
    @Path("/multipleConsents/{consentId}")
    @Produces("application/json")
    public Response getMultiplePurposesMatch(@PathParam("consentId") String consentId){
        try {
            List<Match> matches = api.findMatchesForConsent(consentId);
            return Response.status(Response.Status.OK).entity(matches).build();
        } catch (IOException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        } catch (UnknownIdentifierException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    // single research purpose across a collection of consents
    @GET
    @Path("/multiplePurposes/{purposeId}")
    @Produces("application/json")
    public Response getMultipleConsentsMatch(@PathParam("purposeId") String purposeId){
        try {
            List<Match> matches = api.findMatchesForPurpose(purposeId);
            return Response.status(Response.Status.OK).entity(matches).build();
        } catch (IOException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        } catch (UnknownIdentifierException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }
}