package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.DataAccessAgreement;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.service.DaaService;
import org.broadinstitute.consent.http.service.DacService;
import org.broadinstitute.consent.http.service.UserService;
import org.broadinstitute.consent.http.util.ConsentLogger;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

@Path("api/daa")
public class DaaResource extends Resource implements ConsentLogger {

  private final DaaService daaService;
  private final DacService dacService;
  private final UserService userService;

  @Inject
  public DaaResource(DaaService daaService, DacService dacService, UserService userService) {
    this.daaService = daaService;
    this.dacService = dacService;
    this.userService = userService;
  }

  @POST
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed({ADMIN, CHAIRPERSON})
  @Path("{dacId}")
  public Response createDaaForDac(
      @Context UriInfo info,
      @Auth AuthUser authUser,
      @PathParam("dacId") Integer dacId,
      @FormDataParam("file") InputStream uploadInputStream,
      @FormDataParam("file") FormDataContentDisposition fileDetail) {
    try {
      dacService.findById(dacId);
      User user = userService.findUserByEmail(authUser.getEmail());
      // Assert that the user has the correct DAC permissions to add a DAA for the provided DacId.
      // Admins can add a DAA with any DAC, but chairpersons can only add DAAs for DACs they are a
      // chairperson for.
      if (!user.hasUserRole(UserRoles.ADMIN)) {
        List<Integer> dacIds = user.getRoles().stream().map(UserRole::getDacId).toList();
        if (!dacIds.contains(dacId)) {
          return Response.status(Status.FORBIDDEN).build();
        }
      }
      DataAccessAgreement daa = daaService.createDaaWithFso(user.getUserId(), dacId, uploadInputStream, fileDetail);
      URI uri = info.getBaseUriBuilder()
          // This will be the GET endpoint for the created DAA
          .replacePath("api/daa/{daaId}")
          .build(daa.getDaaId());
      return Response.created(uri).entity(daa).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

}
