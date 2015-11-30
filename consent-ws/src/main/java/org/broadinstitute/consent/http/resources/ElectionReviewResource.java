package org.broadinstitute.consent.http.resources;

import org.broadinstitute.consent.http.service.AbstractDataAccessRequestAPI;
import org.broadinstitute.consent.http.service.ConsentAPI;
import org.broadinstitute.consent.http.service.AbstractReviewResultsAPI;
import org.broadinstitute.consent.http.service.ReviewResultsAPI;
import org.broadinstitute.consent.http.service.AbstractElectionAPI;
import org.broadinstitute.consent.http.service.ElectionAPI;
import org.broadinstitute.consent.http.service.DataAccessRequestAPI;
import org.broadinstitute.consent.http.service.AbstractConsentAPI;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.ElectionReview;

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
    public ElectionReview getCollectElectionReview(@QueryParam("referenceId") String referenceId, @QueryParam("type") String type) {
        return api.describeCollectElectionReviewByReferenceId(referenceId, type);
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
        accessElectionReview.setVoteAgreement(api.describeAgreementVote(electionId));
        accessElectionReview.setConsent(consent);
        return accessElectionReview;
    }

    @GET
    @Path("rp/{electionId}")
    @Produces("application/json")
    public ElectionReview getRPElectionReviewByReferenceId(@PathParam("electionId") Integer electionId, @QueryParam("isFinalAccess") Boolean isFinalAccess) {
        Integer rpElectionId = electionAPI.findRPElectionByElectionAccessId(electionId);
        return api.describeElectionReviewByElectionId(rpElectionId,isFinalAccess);
    }

    @GET
    @Path("last/{referenceId}")
    @Produces("application/json")
    public ElectionReview getElectionReviewByReferenceId(@PathParam("referenceId") String referenceId) {
       return api.describeElectionReviewByReferenceId(referenceId);
    }


}