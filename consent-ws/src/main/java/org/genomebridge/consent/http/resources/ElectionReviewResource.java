package org.genomebridge.consent.http.resources;

import org.genomebridge.consent.http.models.Consent;
import org.genomebridge.consent.http.models.Election;
import org.genomebridge.consent.http.models.ElectionReview;
import org.genomebridge.consent.http.service.*;

import javax.ws.rs.*;
import java.util.Arrays;

@Path("{api : (api/)?}electionReview")
public class ElectionReviewResource {

    private final ReviewResultsAPI api;
    private final ElectionAPI electionAPI;
    private final DataAccessRequestAPI accessRequestAPI;
    private final ConsentAPI consentAPI;

    public ElectionReviewResource() {
        this.api = AbstractReviewResultsAPI.getInstance();
        this.electionAPI = AbstractElectionAPI.getInstance();
        this.accessRequestAPI = AbstractDataAccessRequestAPI.getInstance();
        this.consentAPI = AbstractConsentAPI.getInstance();
    }


    @GET
    @Produces("application/json")
    public ElectionReview getCollectElectionReview(@QueryParam("referenceId") String referenceId) {
        return api.describeCollectElectionReviewByReferenceId(referenceId);
    }

    @GET
    @Path("/openElection")
    @Produces("application/json")
    public String openElections() {
        return ("{ \"open\" : " + api.openElections() + " }");
    }


    @GET
    @Path("/{electionId}")
    @Produces("application/json")
    public ElectionReview getElectionReviewByElectionId(@PathParam("electionId") Integer electionId) {
        return api.describeElectionReviewByElectionId(electionId, null);
    }



    @GET
    @Path("access/{electionId}")
    @Produces("application/json")
    public ElectionReview getAccessElectionReviewByReferenceId(@PathParam("electionId") Integer electionId, @QueryParam("isFinalAccess") Boolean isFinalAccess) {
        Election election = electionAPI.describeElectionById(electionId);
        String dataSetId = accessRequestAPI.describeDataAccessRequestFieldsById(election.getReferenceId(), Arrays.asList("datasetId")).getString("datasetId");
        Consent consent =  consentAPI.getConsentFromDatasetID(dataSetId);
        ElectionReview accessElectionReview = api.describeElectionReviewByElectionId(electionId,isFinalAccess);
        accessElectionReview.setConsent(consent);
        return accessElectionReview;
    }


    @GET
    @Path("last/{referenceId}")
    @Produces("application/json")
    public ElectionReview getElectionReviewByReferenceId(@PathParam("referenceId") String referenceId) {
       return api.describeElectionReviewByReferenceId(referenceId);
    }


}