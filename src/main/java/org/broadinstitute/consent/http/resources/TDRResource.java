package org.broadinstitute.consent.http.resources;

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
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;


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
            Dataset dataset = datasetService.findDatasetByIdentifier(identifier);
            if (Objects.isNull(dataset)) {
                throw new NotFoundException("Could not find dataset " + identifier);
            }

            ApprovedUsers approvedUsers = tdrService.getApprovedUsersForDataset(dataset);
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
            Dataset dataset = datasetService.findDatasetByIdentifier(identifier);
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
    @PermitAll
    public Response createDraftDataAccessRequest(
        @Auth AuthUser authUser,
        @Context UriInfo info,
        @QueryParam("identifiers") String identifiers,
        @QueryParam("projectTitle") String projectTitle) {
      try {
        // Ensure that the user is a registered DUOS and SAM user
        User user = userService.findOrCreateUser(authUser);
        if (Objects.isNull(identifiers) || identifiers.isBlank()) {
          throw new BadRequestException("No dataset identifiers were provided");
        } else {
          List<String> identifierList = Arrays.stream(identifiers.split(","))
                  .map(String::trim)
                  .filter(identifier -> !identifier.isBlank())
                  // this will filter duplicate identifier strings, ex. "DUOS-000594, DUOS-000594"
                  .distinct()
                  .toList();
          List<Integer> aliasList = identifierList
                  .stream()
                  .map(Dataset::parseIdentifierToAlias)
                  // this will filter duplicate aliases, ex. "593, 593"
                  .distinct()
                  .toList();
          List<Dataset> datasets = tdrService.getDatasetsByIdentifier(aliasList);
          List<Integer> datasetAliases = datasets.stream().map(Dataset::getAlias).toList();
          // Check that we were able to find a dataset id for all identifiers provided
          if (aliasList.size() != datasets.size()) {
            // isolate a list of identifier strings that were not matched to datasets
            List<String> notFoundIdentifiers = identifierList
                    .stream()
                    .filter(identifier -> !datasetAliases.contains(Dataset.parseIdentifierToAlias(identifier)))
                    .toList();
            // throw a NFE to let the client know which identifiers were NOT found so they can rectify their request
            throw new NotFoundException("Invalid dataset identifiers were provided: " + notFoundIdentifiers);
          }
          List<Integer> datasetIds = datasets
                  .stream()
                  .map(Dataset::getDataSetId)
                  .toList();
          DataAccessRequest newDar = new DataAccessRequest();
          DataAccessRequestData data = new DataAccessRequestData();
          String referenceId = UUID.randomUUID().toString();
          newDar.setReferenceId(referenceId);
          data.setReferenceId(referenceId);
          if (!Objects.isNull(projectTitle) && !projectTitle.isBlank()) {
            data.setProjectTitle(projectTitle);
          }
          newDar.setData(data);
          newDar.setDatasetIds(datasetIds);
          DataAccessRequest result = darService.insertDraftDataAccessRequest(user, newDar);
          // URI should return the new DAR url
          URI uri = info.getRequestUriBuilder().replacePath("").replacePath("api/dar/v2/" + result.getReferenceId()).replaceQuery(null).build();
          return Response.created(uri).entity(result.convertToSimplifiedDar()).build();
        }
      } catch (Exception e) {
          return createExceptionResponse(e);
      }
    }
}
