package org.genomebridge.consent.http.resources;

import org.genomebridge.consent.http.service.AbstractMatchingServiceAPI;
import org.genomebridge.consent.http.service.MatchingServiceAPI;
import org.genomebridge.consent.http.service.UnknownIdentifierException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import java.io.IOException;

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
    public void getSingleMatch(@PathParam("consentId") String consentId, @PathParam("purposeId") String purposeId){
        try {
            api.findSingleMatch(consentId, purposeId);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (UnknownIdentifierException e) {
            e.printStackTrace();
        }
    }

    // single consent across a collection of purposes
    @GET
    @Path("/multipleConsents/{consentId}")
    @Produces("application/json")
    public void getMultiplePurposesMatch(@PathParam("consentId") String consentId) throws IOException, UnknownIdentifierException {
        api.findMatchesForConsent(consentId);
    }

    // single research purpose across a collection of consents
    @GET
    @Path("/multiplePurposes/{purposeId}")
    @Produces("application/json")
    public void getMultipleConsentsMatch(@PathParam("purposeId") String purposeId) throws IOException, UnknownIdentifierException {
        api.findMatchesForPurpose(purposeId);
    }
}