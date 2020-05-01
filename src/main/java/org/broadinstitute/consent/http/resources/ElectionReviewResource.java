package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;
import org.apache.commons.collections.CollectionUtils;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.ElectionReview;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.service.AbstractConsentAPI;
import org.broadinstitute.consent.http.service.AbstractElectionAPI;
import org.broadinstitute.consent.http.service.AbstractReviewResultsAPI;
import org.broadinstitute.consent.http.service.ConsentAPI;
import org.broadinstitute.consent.http.service.DataAccessRequestService;
import org.broadinstitute.consent.http.service.ElectionAPI;
import org.broadinstitute.consent.http.service.ReviewResultsAPI;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Path("api/electionReview")
public class ElectionReviewResource extends Resource {

    private final ReviewResultsAPI api;
    private final ElectionAPI electionAPI;
    private final ConsentAPI consentAPI;
    private final DataAccessRequestService darService;

    @Inject
    public ElectionReviewResource(DataAccessRequestService darService) {
        this.api = AbstractReviewResultsAPI.getInstance();
        this.electionAPI = AbstractElectionAPI.getInstance();
        this.consentAPI = AbstractConsentAPI.getInstance();
        this.darService = darService;
    }

    @GET
    @Produces("application/json")
    @RolesAllowed({ADMIN, MEMBER, CHAIRPERSON, ALUMNI})
    public ElectionReview getCollectElectionReview(@QueryParam("referenceId") String referenceId, @QueryParam("type") String type) {
        return api.describeLastElectionReviewByReferenceIdAndType(referenceId, type);
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
    @RolesAllowed({ADMIN, MEMBER, CHAIRPERSON, ALUMNI})
    public ElectionReview getElectionReviewByElectionId(@PathParam("electionId") Integer electionId) {
        return api.describeElectionReviewByElectionId(electionId, null);
    }

    @GET
    @Path("access/{electionId}")
    @Produces("application/json")
    @RolesAllowed({ADMIN, MEMBER, CHAIRPERSON, ALUMNI})
    public ElectionReview getAccessElectionReviewByReferenceId(@PathParam("electionId") Integer electionId, @QueryParam("isFinalAccess") Boolean isFinalAccess) {
        Election election = electionAPI.describeElectionById(electionId);
        Election consentElection = electionAPI.getConsentElectionByDARElectionId(election.getElectionId());
        DataAccessRequest dar = darService.findByReferenceId(election.getReferenceId());
        List<Integer> dataSetId = new ArrayList<>();
        if (Objects.nonNull(dar) && Objects.nonNull(dar.getData()) && Objects.nonNull(dar.getData().getDatasetId())) {
            dataSetId.addAll(dar.getData().getDatasetId());
        }
        Consent consent = consentAPI.getConsentFromDatasetID(dataSetId.get(0));
        ElectionReview accessElectionReview = api.describeElectionReviewByElectionId(electionId, isFinalAccess);
        List<Vote> agreementVote = api.describeAgreementVote(electionId);
        accessElectionReview.setConsent(consent);
        accessElectionReview.setVoteAgreement(CollectionUtils.isNotEmpty(agreementVote) ? agreementVote.get(0) : null);
        accessElectionReview.setAssociatedConsent(consent, consentElection);
        return accessElectionReview;
    }

    @GET
    @Path("rp/{electionId}")
    @Produces("application/json")
    @RolesAllowed({ADMIN, MEMBER, CHAIRPERSON, ALUMNI})
    public ElectionReview getRPElectionReviewByReferenceId(@PathParam("electionId") Integer electionId, @QueryParam("isFinalAccess") Boolean isFinalAccess) {
        Integer rpElectionId = electionAPI.findRPElectionByElectionAccessId(electionId);
        if (Objects.nonNull(rpElectionId)) {
            return api.describeElectionReviewByElectionId(rpElectionId, isFinalAccess);
        } else return null;
    }

    @GET
    @Path("last/{referenceId}")
    @Produces("application/json")
    @RolesAllowed({ADMIN, MEMBER, CHAIRPERSON, ALUMNI})
    public ElectionReview getElectionReviewByReferenceId(@PathParam("referenceId") String referenceId) {
        return api.describeElectionReviewByReferenceId(referenceId);
    }

}
