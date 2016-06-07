package org.broadinstitute.consent.http.resources;

import org.apache.commons.collections.CollectionUtils;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.ElectionReview;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.service.*;
import org.broadinstitute.consent.http.util.DarConstants;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

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
    @RolesAllowed({"ADMIN","MEMBER","CHAIRPERSON","ALUMNI"})
    public ElectionReview getCollectElectionReview(@QueryParam("referenceId") String referenceId, @QueryParam("type") String type) {
        return api.describeCollectElectionReviewByReferenceId(referenceId, type);
    }

    @GET
    @Path("/openElection")
    @Produces("application/json")
    @PermitAll
    public String openElections() {
        return ("{ \"open\" : " + api.openElections() + " }");
    }


    @GET
    @Path("/{electionId}")
    @Produces("application/json")
    @RolesAllowed({"ADMIN","MEMBER","CHAIRPERSON","ALUMNI"})
    public ElectionReview getElectionReviewByElectionId(@PathParam("electionId") Integer electionId) {
        return api.describeElectionReviewByElectionId(electionId, null);
    }



    @GET
    @Path("access/{electionId}")
    @Produces("application/json")
    @RolesAllowed({"ADMIN","MEMBER","CHAIRPERSON","ALUMNI"})
    public ElectionReview getAccessElectionReviewByReferenceId(@PathParam("electionId") Integer electionId, @QueryParam("isFinalAccess") Boolean isFinalAccess) {
        Election election = electionAPI.describeElectionById(electionId);
        List<String> dataSetId = accessRequestAPI.describeDataAccessRequestFieldsById(election.getReferenceId(), Arrays.asList(DarConstants.DATASET_ID)).get(DarConstants.DATASET_ID, List.class);
        Consent consent =  consentAPI.getConsentFromDatasetID(dataSetId.get(0));
        ElectionReview accessElectionReview = api.describeElectionReviewByElectionId(electionId,isFinalAccess);
        List<Vote> agreementVote = api.describeAgreementVote(electionId);
        accessElectionReview.setVoteAgreement(CollectionUtils.isNotEmpty(agreementVote) ? agreementVote.get(0) : null);
        accessElectionReview.setConsent(consent);
        return accessElectionReview;
    }

    @GET
    @Path("rp/{electionId}")
    @Produces("application/json")
    @RolesAllowed({"ADMIN","MEMBER","CHAIRPERSON","ALUMNI"})
    public ElectionReview getRPElectionReviewByReferenceId(@PathParam("electionId") Integer electionId, @QueryParam("isFinalAccess") Boolean isFinalAccess) {
        Integer rpElectionId = electionAPI.findRPElectionByElectionAccessId(electionId);
        if (Objects.nonNull(rpElectionId)) {
            return api.describeElectionReviewByElectionId(rpElectionId,isFinalAccess);
        }
        else return null;
    }


    @GET
    @Path("last/{referenceId}")
    @Produces("application/json")
    @RolesAllowed({"ADMIN","MEMBER","CHAIRPERSON","ALUMNI"})
    public ElectionReview getElectionReviewByReferenceId(@PathParam("referenceId") String referenceId) {
        return api.describeElectionReviewByReferenceId(referenceId);
    }


}