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
import org.broadinstitute.consent.http.models.User;
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
  public Response getCollectionsByUserId(@Auth AuthUser authUser, @PathParam("id") Integer userId) {
    try{
      User user = userService.findUserByEmail(authUser.getEmail());
      List<DarCollection> collections = darCollectionService.findDarCollectionsByUserId(user.getDacUserId());
      if(user.hasUserRole(UserRoles.ADMIN) || user.hasUserRole(UserRoles.CHAIRPERSON)) {
        HashMap<Integer, DarCollection> collectionMapping = darCollectionService.sortCollectionsByDataUse(collections);
      }
    } catch(Exception e) {
      return createExceptionResponse(e);
    }
  }

  public List<DarCollection> sortCollectionsByDataUse(List<DarCollection> collections, User user) {
    if(user.hasUserRole(UserRoles.ADMIN) || user.hasUserRole(UserRoles.CHAIRPERSON)) {
      HashMap<Integer, DarCollection> collectionMapping = darCollectionService.sortCollectionsByDataUse(collections);
    }
  }
}