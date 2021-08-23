package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.sam.ResourceType;
import org.broadinstitute.consent.http.models.sam.SamSelfDiagnostics;
import org.broadinstitute.consent.http.models.sam.SamUserInfo;
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
      SamUserInfo userInfo = samService.postRegistrationInfo(authUser);
      URI location = URI.create(uriInfo.getPath() + "/api/user/me");
      return Response.created(location).entity(userInfo.toString()).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @Path("register/self/diagnostics")
  @GET
  @Produces("application/json")
  public Response getSelfDiagnostics(@Auth AuthUser authUser) {
    try {
      SamSelfDiagnostics selfDiagnostics = samService.getSelfDiagnostics(authUser);
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
      SamUserInfo userInfo = samService.getRegistrationInfo(authUser);
      return Response.ok().entity(userInfo.toString()).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }
}
