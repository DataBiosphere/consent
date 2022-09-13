package org.broadinstitute.consent.http.resources;

import com.google.gson.Gson;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.tdr.ApprovedUsers;
import org.broadinstitute.consent.http.service.DataAccessRequestService;
import org.broadinstitute.consent.http.service.DatasetService;
import org.broadinstitute.consent.http.service.TDRService;
import org.broadinstitute.consent.http.service.UserService;


import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;


@Path("api/tdr")
public class TDRResource extends Resource {

    private final TDRService tdrService;
    private final DatasetService datasetService;
    private final UserService userService;
    private final DataAccessRequestService darService;


    @Inject
    public TDRResource(TDRService tdrService, DatasetService datasetService,
                       UserService userService, DataAccessRequestService darService) {
        this.datasetService = datasetService;
        this.tdrService = tdrService;
        this.userService = userService;
        this.darService = darService;
    }

    @GET
    @Produces("application/json")
    @PermitAll
    @Path("/{identifier}/approved/users")
    public Response getApprovedUsers(@Auth AuthUser authUser, @PathParam("identifier") String identifier) {
        try {
            Dataset dataset = this.datasetService.findDatasetByIdentifier(identifier);
            if (Objects.isNull(dataset)) {
                throw new NotFoundException("Could not find dataset " + identifier);
            }

            ApprovedUsers approvedUsers = this.tdrService.getApprovedUsersForDataset(dataset);
            return Response.ok(approvedUsers).build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

    @GET
    @Produces("application/json")
    @PermitAll
    @Path("/{identifier}")
    public Response getDatasetByIdentifier(@Auth AuthUser authUser, @PathParam("identifier") String identifier) {
        try {
            Dataset dataset = this.datasetService.findDatasetByIdentifier(identifier);
            if (Objects.isNull(dataset)) {
                throw new NotFoundException("Could not find dataset " + identifier);
            }

            return Response.ok(dataset).build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

    @POST
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/dar/draft")
    @RolesAllowed(ADMIN)
    public Response createDraftDataAccessRequest(
        @Auth AuthUser authUser, @Context UriInfo info, List<String> datasetIdentifiers, String projectTitle) {
      try {
        // Ensure that the user is a registered DUOS and SAM user
        User user = findOrCreateUser(authUser);
        List<Integer> datasetIds = datasetIdentifiers.stream()
                .map(identifier -> this.datasetService.findDatasetByIdentifier(identifier).getDataSetId())
                .collect(Collectors.toList());
        if (datasetIds.isEmpty()) {
            throw new IllegalArgumentException("The dataset identifiers provided must be associated with an existing dataset");
        }
        DataAccessRequest newDar = new DataAccessRequest();
        DataAccessRequestData data = new DataAccessRequestData();
        String referenceId = UUID.randomUUID().toString();
        newDar.setReferenceId(referenceId);
        data.setReferenceId(referenceId);
        data.setProjectTitle(projectTitle);
        newDar.setData(data);
        newDar.setDatasetIds(datasetIds);
        DataAccessRequest result = this.darService.insertDraftDataAccessRequest(user, newDar);
        URI uri = info.getRequestUriBuilder().path("/" + result.getReferenceId()).build();
        return Response.created(uri).entity(result.convertToSimplifiedDar()).build();
      } catch (Exception e) {
          return createExceptionResponse(e);
      }
    }

    // should this be private? otherwise can't test
    User findOrCreateUser(AuthUser authUser) {
        // Ensure that the user is a registered DUOS and SAM user
        User tdrUser;
        try {
            tdrUser = this.userService.findUserByEmail(authUser.getEmail());
        } catch (NotFoundException nfe) {
            User newTdrUser = new User();
            newTdrUser.setEmail(authUser.getEmail());
            newTdrUser.setDisplayName(authUser.getName());
            tdrUser = this.userService.createUser(newTdrUser);
        }
        return tdrUser;
    }
}
