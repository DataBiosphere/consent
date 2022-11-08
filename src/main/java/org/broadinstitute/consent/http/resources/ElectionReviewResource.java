package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;
import org.broadinstitute.consent.http.service.ReviewResultsService;

import javax.annotation.security.PermitAll;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Path("api/electionReview")
public class ElectionReviewResource extends Resource {

    private final ReviewResultsService service;
    @Inject
    public ElectionReviewResource(ReviewResultsService reviewResultsService) {
        this.service = reviewResultsService;
    }

    @GET
    @Path("/openElection")
    @Produces("application/json")
    @PermitAll
    public String openElections() {
        return ("{ \"open\" : " + service.openElections() + " }");
    }

}
