package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.dto.Error;
import org.broadinstitute.consent.http.service.AbstractMatchProcessAPI;
import org.broadinstitute.consent.http.service.DataAccessRequestService;
import org.broadinstitute.consent.http.service.EmailNotifierService;
import org.broadinstitute.consent.http.service.MatchProcessAPI;
import org.broadinstitute.consent.http.service.UserService;

@Path("api/dar/v2")
public class DataAccessRequestResourceVersion2 extends Resource {

  private final DataAccessRequestService dataAccessRequestService;
  private final MatchProcessAPI matchProcessAPI;
  private final EmailNotifierService emailNotifierService;
  private final UserService userService;

  @Inject
  public DataAccessRequestResourceVersion2(
      DataAccessRequestService dataAccessRequestService,
      EmailNotifierService emailNotifierService,
      UserService userService) {
    this.dataAccessRequestService = dataAccessRequestService;
    this.emailNotifierService = emailNotifierService;
    this.matchProcessAPI = AbstractMatchProcessAPI.getInstance();
    this.userService = userService;
  }

  @POST
  @Consumes("application/json")
  @Produces("application/json")
  @RolesAllowed(RESEARCHER)
  public Response createDataAccessRequest(
      @Auth AuthUser authUser, @Context UriInfo info, String dar) {
    User user = findUserByEmail(authUser.getName());
    DataAccessRequest newDar = new DataAccessRequest();
    DataAccessRequestData data = DataAccessRequestData.fromString(dar);
    if (Objects.isNull(data)) {
      data = new DataAccessRequestData();
    }
    if (Objects.nonNull(data.getReferenceId())) {
      newDar.setReferenceId(data.getReferenceId());
    } else {
      String referenceId = UUID.randomUUID().toString();
      newDar.setReferenceId(referenceId);
      data.setReferenceId(referenceId);
    }
    newDar.setData(data);

    try {
      List<DataAccessRequest> results =
          dataAccessRequestService.createDataAccessRequest(user, newDar);
      URI uri = info.getRequestUriBuilder().build();
      for (DataAccessRequest r : results) {
        matchProcessAPI.processMatchesForPurpose(r.getReferenceId());
        emailNotifierService.sendNewDARRequestMessage(
            r.getData().getDarCode(), r.getData().getDatasetIds());
      }
      return Response.created(uri)
          .entity(
              results.stream()
                  .map(DataAccessRequest::convertToSimplifiedDar)
                  .collect(Collectors.toList()))
          .build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @GET
  @Path("/{referenceId}")
  @Produces("application/json")
  @PermitAll
  public Response getByReferenceId(
      @Auth AuthUser authUser, @PathParam("referenceId") String referenceId) {
    try {
      DataAccessRequest dar = dataAccessRequestService.findByReferenceId(referenceId);
      if (Objects.nonNull(dar)) {
        return Response.status(Response.Status.OK).entity(dar.convertToSimplifiedDar()).build();
      }
      return Response.status(Response.Status.NOT_FOUND)
          .entity(
              new Error(
                  "Unable to find Data Access Request with reference id: " + referenceId,
                  Response.Status.NOT_FOUND.getStatusCode()))
          .build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @PUT
  @Path("/{referenceId}")
  @Produces("application/json")
  @RolesAllowed(RESEARCHER)
  public Response updateByReferenceId(
      @Auth AuthUser authUser, @PathParam("referenceId") String referenceId, String dar) {
    try {
      User user = findUserByEmail(authUser.getName());
      DataAccessRequest originalDar = dataAccessRequestService.findByReferenceId(referenceId);
      DataAccessRequestData data = DataAccessRequestData.fromString(dar);
      originalDar.setData(data);
      DataAccessRequest updatedDar =
          dataAccessRequestService.updateByReferenceIdVersion2(user, originalDar);
      matchProcessAPI.processMatchesForPurpose(referenceId);
      return Response.ok().entity(updatedDar.convertToSimplifiedDar()).build();
    } catch (Exception e) {
      return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
    }
  }

  private User findUserByEmail(String email) {
    User user = userService.findUserByEmail(email);
    if (user == null) {
      throw new NotFoundException("Unable to find User with the provided email: " + email);
    }
    return user;
  }
}
