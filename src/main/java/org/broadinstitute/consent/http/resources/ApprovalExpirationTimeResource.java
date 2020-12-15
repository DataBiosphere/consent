package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import java.net.URI;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.broadinstitute.consent.http.models.ApprovalExpirationTime;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.ApprovalExpirationTimeService;
import org.broadinstitute.consent.http.service.UserService;

@Path("{api : (api/)?}approvalExpirationTime")
public class ApprovalExpirationTimeResource extends Resource {

  private final ApprovalExpirationTimeService approvalExpirationTimeService;
  private final UserService userService;

  @Inject
  public ApprovalExpirationTimeResource(ApprovalExpirationTimeService approvalExpirationTimeService,
      UserService userService) {
    this.approvalExpirationTimeService = approvalExpirationTimeService;
    this.userService = userService;
  }

  @POST
  @Consumes("application/json")
  @Produces("application/json")
  @RolesAllowed(ADMIN)
  public Response createdApprovalExpirationTime(
      @Auth AuthUser authUser,
      @Context UriInfo info,
      ApprovalExpirationTime approvalExpirationTime) {
    URI uri;
    try {
      User user = userService.findUserByEmail(authUser.getName());
      approvalExpirationTime.setUserId(user.getDacUserId());
      approvalExpirationTime = approvalExpirationTimeService.create(approvalExpirationTime);
      uri = info.getRequestUriBuilder().path("{id}").build(approvalExpirationTime.getId());
      return Response.created(uri).entity(approvalExpirationTime).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @GET
  @Produces("application/json")
  @RolesAllowed(ADMIN)
  public Response describeApprovalExpirationTime() {
    try {
      return Response.ok().entity(approvalExpirationTimeService.findApprovalExpirationTime()).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @GET
  @Path("/{id}")
  @Produces("application/json")
  @RolesAllowed(ADMIN)
  public Response describe(@PathParam("id") Integer id) {
    try {
      return Response.ok()
          .entity(approvalExpirationTimeService.findApprovalExpirationTimeById(id))
          .build();
    } catch (NotFoundException e) {
      return createExceptionResponse(e);
    }
  }

  @PUT
  @Path("/{id}")
  @Consumes("application/json")
  @Produces("application/json")
  @RolesAllowed(ADMIN)
  public Response update(
      @Auth AuthUser authUser,
      @Context UriInfo info,
      ApprovalExpirationTime approvalExpirationTime,
      @PathParam("id") Integer id) {
    try {
      User user = userService.findUserByEmail(authUser.getName());
      approvalExpirationTime.setUserId(user.getDacUserId());
      URI uri = info.getRequestUriBuilder().path("{id}").build(id);
      approvalExpirationTime = approvalExpirationTimeService.update(approvalExpirationTime, id);
      return Response.ok(uri).entity(approvalExpirationTime).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @DELETE
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/{id}")
  @RolesAllowed(ADMIN)
  public Response delete(@PathParam("id") Integer id) {
    try {
      approvalExpirationTimeService.deleteApprovalExpirationTime(id);
      return Response.ok().build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }
}
