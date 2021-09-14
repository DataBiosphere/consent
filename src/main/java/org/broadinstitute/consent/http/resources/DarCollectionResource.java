package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import java.util.Collections;
import java.util.List;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
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
  public DarCollectionResource(UserService userService, DarCollectionService darCollectionService) {
    this.userService = userService;
    this.darCollectionService = darCollectionService;
  }

  @GET
  @Produces("application/json")
  @RolesAllowed(RESEARCHER)
  public Response getCollectionsForResearcher(@Auth AuthUser authUser) {
    try {
      User user = userService.findUserByEmail(authUser.getEmail());
      List<DarCollection> collections = darCollectionService.getCollectionsForUser(user);
      darCollectionService.addDatasetsToCollections(collections);
      return Response.ok().entity(collections).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @GET
  @Path("{collectionId}")
  @Produces("application/json")
  @PermitAll
  public Response getCollectionById(
      @Auth AuthUser authUser,
      @PathParam("collectionId") Integer collectionId) {
    try {
      User user = userService.findUserByEmail(authUser.getEmail());
      DarCollection collection = darCollectionService.getByCollectionId(collectionId);
      // Users can only see their own collection, regardless of user's roles
      validateAuthedRoleUser(Collections.emptyList(), user, collection.getCreateUserId());
      return Response.ok().entity(collection).build();
    } catch (ForbiddenException e) {
      // We don't want to leak existence, throw a not found
      throw new NotFoundException();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @GET
  @Path("dar/{referenceId}")
  @Produces("application/json")
  @PermitAll
  public Response getCollectionByReferenceId(
      @Auth AuthUser authUser,
      @PathParam("referenceId") String referenceId) {
    try {
      User user = userService.findUserByEmail(authUser.getEmail());
      DarCollection collection = darCollectionService.getByReferenceId(referenceId);
      // Users can only see their own collection, regardless of user's roles
      validateAuthedRoleUser(Collections.emptyList(), user, collection.getCreateUserId());
      return Response.ok().entity(collection).build();
    } catch (ForbiddenException e) {
      // We don't want to leak existence, throw a not found
      throw new NotFoundException();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }
}
