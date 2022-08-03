package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.tdr.ApprovedUsers;
import org.broadinstitute.consent.http.service.DatasetService;
import org.broadinstitute.consent.http.service.TDRService;

import javax.annotation.security.PermitAll;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

@Path("api/tdr")
public class TDRResource extends Resource {

    private final TDRService tdrService;
    private final DatasetService datasetService;


    @Inject
    public TDRResource(TDRService tdrService, DatasetService datasetService) {
        this.datasetService = datasetService;
        this.tdrService = tdrService;
    }

    @GET
    @Produces("application/json")
    @PermitAll
    @Path("/{identifier}/approved/users")
    public Response getApprovedUsers(@Auth AuthUser authUser, @PathParam("identifier") String identifier) {
        try {
            Dataset dataset = this.datasetService.findDatasetByIdentifier(identifier);
            if (dataset == null) {
                throw new NotFoundException("Could not find dataset " + identifier);
            }

            ApprovedUsers approvedUsers = this.tdrService.getApprovedUsersForDataset(dataset);
            return Response.ok(approvedUsers).build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }
}
