package org.broadinstitute.consent.http.resources;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.ConsentManage;
import org.broadinstitute.consent.http.service.ConsentService;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("{auth: (basic/|api/)?}consent")
public class ConsentManageResource extends Resource {

    private final ConsentService consentService;

    @Inject
    public ConsentManageResource(ConsentService consentService) {
        this.consentService = consentService;
    }

    @GET
    @Path("/manage")
    @RolesAllowed(ADMIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getConsentManage(@Auth AuthUser authUser) {
        List<ConsentManage> consentManages = consentService.describeConsentManage(authUser);
        return Response.ok().entity(consentManages).build();
    }

    @GET
    @Path("/unreviewed")
    @RolesAllowed(ADMIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTotalUnreviewedConsent(@Auth AuthUser authUser) {
        Integer count = consentService.getUnReviewedConsents(authUser);
        UnreviewedCases entity = new UnreviewedCases(count);
        return Response.ok().entity(entity).build();
    }

    static class UnreviewedCases {
        @JsonProperty
        Integer dulUnReviewedCases;

        UnreviewedCases(Integer dulUnReviewedCases) {
            this.dulUnReviewedCases = dulUnReviewedCases;
        }
    }

}
