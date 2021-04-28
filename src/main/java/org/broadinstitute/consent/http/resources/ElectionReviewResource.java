package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;
import org.apache.commons.collections.CollectionUtils;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.ElectionReview;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.service.ConsentService;
import org.broadinstitute.consent.http.service.DataAccessRequestService;
import org.broadinstitute.consent.http.service.ElectionService;
import org.broadinstitute.consent.http.service.ReviewResultsService;

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

    private final ReviewResultsService service;
    private final ElectionService electionService;
    private final ConsentService consentService;
    private final DataAccessRequestService darService;

    @Inject
    public ElectionReviewResource(DataAccessRequestService darService, ConsentService consentService, ElectionService electionService, ReviewResultsService reviewResultsService) {
        this.service = reviewResultsService;
        this.electionService = electionService;
        this.consentService = consentService;
        this.darService = darService;
    }

    @GET
    @Produces("application/json")
    @RolesAllowed({ADMIN, MEMBER, CHAIRPERSON, ALUMNI})
    public ElectionReview getCollectElectionReview(@QueryParam("referenceId") String referenceId, @QueryParam("type") String type) {
        return service.describeLastElectionReviewByReferenceIdAndType(referenceId, type);
    }

    @GET
    @Path("/openElection")
    @Produces("application/json")
    @PermitAll
    public String openElections() {
        return ("{ \"open\" : " + service.openElections() + " }");
    }

    @GET
    @Path("/{electionId}")
    @Produces("application/json")
    @RolesAllowed({ADMIN, MEMBER, CHAIRPERSON, ALUMNI})
    public ElectionReview getElectionReviewByElectionId(@PathParam("electionId") Integer electionId) {
        return service.describeElectionReviewByElectionId(electionId, null);
    }

    @GET
    @Path("access/{electionId}")
    @Produces("application/json")
    @RolesAllowed({ADMIN, MEMBER, CHAIRPERSON, ALUMNI})
    public ElectionReview getAccessElectionReviewByReferenceId(@PathParam("electionId") Integer electionId) {
        Election election = electionService.describeElectionById(electionId);
        Election consentElection = electionService.getConsentElectionByDARElectionId(election.getElectionId());
        DataAccessRequest dar = darService.findByReferenceId(election.getReferenceId());
        List<Integer> dataSetId = new ArrayList<>();
        if (Objects.nonNull(dar) && Objects.nonNull(dar.getData()) && Objects.nonNull(dar.getData().getDatasetIds())) {
            dataSetId.addAll(dar.getData().getDatasetIds());
        }
        Consent consent = consentService.getConsentFromDatasetID(dataSetId.get(0));
        ElectionReview accessElectionReview = service.describeElectionReviewByElectionId(electionId, null);
        List<Vote> agreementVote = service.describeAgreementVote(electionId);
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
        Integer rpElectionId = electionService.findRPElectionByElectionAccessId(electionId);
        if (Objects.nonNull(rpElectionId)) {
            return service.describeElectionReviewByElectionId(rpElectionId, isFinalAccess);
        } else return null;
    }

    @GET
    @Path("last/{referenceId}")
    @Produces("application/json")
    @RolesAllowed({ADMIN, MEMBER, CHAIRPERSON, ALUMNI})
    public ElectionReview getElectionReviewByReferenceId(@PathParam("referenceId") String referenceId) {
        return service.describeElectionReviewByReferenceId(referenceId);
    }

}
