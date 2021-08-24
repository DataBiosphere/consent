package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.sam.ResourceType;
import org.broadinstitute.consent.http.models.sam.UserStatus;
import org.broadinstitute.consent.http.models.sam.UserStatusDiagnostics;
import org.broadinstitute.consent.http.models.sam.UserStatusInfo;
import org.broadinstitute.consent.http.service.sam.SamService;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;

@Path("api/sam")
public class SamResource extends Resource {

  private final SamService samService;

  @Inject
  public SamResource(SamService samService) {
    this.samService = samService;
  }

  @Path("resource-types")
  @GET
  @Produces("application/json")
  public Response getResourceTypes(@Auth AuthUser authUser) {
    try {
      List<ResourceType> types = samService.getResourceTypes(authUser);
      return Response.ok().entity(types.toString()).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @Path("register/self")
  @POST
  @Produces("application/json")
  public Response postRegistrationInfo(@Auth AuthUser authUser, @Context UriInfo uriInfo) {
    try {
      URI location = URI.create(uriInfo.getBaseUri() + "/api/sam/register/self/info");
      UserStatus userStatus = samService.postRegistrationInfo(authUser);
      return Response.created(location).entity(userStatus.toString()).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @Path("register/self/diagnostics")
  @GET
  @Produces("application/json")
  public Response getSelfDiagnostics(@Auth AuthUser authUser) {
    try {
      UserStatusDiagnostics selfDiagnostics = samService.getSelfDiagnostics(authUser);
      return Response.ok().entity(selfDiagnostics.toString()).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @Path("register/self/info")
  @GET
  @Produces("application/json")
  public Response getRegistrationInfo(@Auth AuthUser authUser) {
    try {
      UserStatusInfo userInfo = samService.getRegistrationInfo(authUser);
      return Response.ok().entity(userInfo.toString()).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }
}
