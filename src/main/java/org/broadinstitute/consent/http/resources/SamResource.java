package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.sam.ResourceType;
import org.broadinstitute.consent.http.models.sam.TosResponse;
import org.broadinstitute.consent.http.models.sam.UserStatus;
import org.broadinstitute.consent.http.models.sam.UserStatusDiagnostics;
import org.broadinstitute.consent.http.models.sam.UserStatusInfo;
import org.broadinstitute.consent.http.service.UserService;
import org.broadinstitute.consent.http.service.sam.SamService;

@Path("api/sam")
public class SamResource extends Resource {

  private final SamService samService;
  private final UserService userService;

  @Inject
  public SamResource(SamService samService, UserService userService) {
    this.samService = samService;
    this.userService = userService;
  }

  @Path("resource-types")
  @GET
  @Produces("application/json")
  @PermitAll
  public Response getResourceTypes(@Auth DuosUser duosUser) {
    try {
      List<ResourceType> types = samService.getResourceTypes(duosUser);
      return Response.ok().entity(types.toString()).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @Path("register/self")
  @POST
  @Produces("application/json")
  @PermitAll
  public Response postRegistrationInfo(@Auth DuosUser duosUser, @Context UriInfo uriInfo) {
    try {
      URI location = URI.create(uriInfo.getBaseUri() + "/api/sam/register/self/info");
      UserStatus userStatus = samService.postRegistrationInfo(duosUser);
      return Response.created(location).entity(userStatus.toString()).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @Path("register/self/diagnostics")
  @GET
  @Produces("application/json")
  @PermitAll
  public Response getSelfDiagnostics(@Auth DuosUser duosUser) {
    try {
      UserStatusDiagnostics selfDiagnostics = samService.getSelfDiagnostics(duosUser);
      return Response.ok().entity(selfDiagnostics.toString()).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @Path("register/self/info")
  @GET
  @Produces("application/json")
  @PermitAll
  public Response getRegistrationInfo(@Auth DuosUser duosUser) {
    try {
      UserStatusInfo userInfo = samService.getRegistrationInfo(duosUser);
      return Response.ok().entity(userInfo.toString()).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @Path("register/self/tos")
  @POST
  @Produces("application/json")
  @PermitAll
  public Response postSelfTos(@Auth DuosUser duosUser) {
    try {
      // Ensure that the user is a registered DUOS user before accepting ToS:
      try {
        userService.findUserByEmail(duosUser.getEmail());
      } catch (NotFoundException nfe) {
        User user = new User();
        user.setEmail(duosUser.getEmail());
        user.setDisplayName(duosUser.getName());
        userService.createUser(user);
      }
      TosResponse tosResponse = samService.postTosAcceptedStatus(duosUser);
      return Response.ok().entity(tosResponse).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @Path("register/self/tos")
  @DELETE
  @Produces("application/json")
  @PermitAll
  public Response removeTos(@Auth DuosUser duosUser) {
    try {
      TosResponse tosResponse = samService.removeTosAcceptedStatus(duosUser);
      return Response.ok().entity(tosResponse).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }
}
