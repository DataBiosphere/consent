package org.broadinstitute.consent.http.resources;

import com.google.api.client.http.HttpStatusCodes;
import com.google.gson.Gson;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.DarCollection;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.models.dto.DatasetDTO;
import org.broadinstitute.consent.http.service.DarCollectionService;
import org.broadinstitute.consent.http.service.UserService;

@Path("api/collections")
public class DarCollectionResource extends Resource {
  private final UserService userService;
  private final DarCollectionService darCollectionService;

  @Inject
  public DarCollectionResource(
    UserService userService,
    DarCollectionService darCollectionService
  ) {
    this.userService = userService;
    this.darCollectionService = darCollectionService;
  }

  @GET
  @Produces("application/json")
  @Path("/{id}")
  @RolesAllowed(RESEARCHER)
  public Response getCollectionsForResearcher(@Auth AuthUser authUser) {
    try{
      User user = userService.findUserByEmail(authUser.getEmail());
      List<DarCollection> collections = darCollectionService.findDarCollectionsByUserId(user.getDacUserId());
      return Response.ok().entity(collections).build();
    } catch(Exception e) {
      return createExceptionResponse(e);
    }
  }

  @GET
  @Produces("application/json")
  @Path("/{id}")
  @RolesAllowed({ADMIN, CHAIRPERSON})
  public Response getCollectionsForRole(@Auth AuthUser authUser, @PathParam("roleName") String roleName) {
    try{
        //NOTE: overall flow for function, may need to split functionality between resource and service
        //(1) get user, get user role
        //(2) fetch collections based on role
        //(3) fetch datasets and add to collection
        //(4) return collections response
      }
    } catch(Exception e) {
      createExceptionResponse(e);
    }
  }
}