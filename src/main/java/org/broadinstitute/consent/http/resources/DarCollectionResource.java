package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.DarCollection;
import org.broadinstitute.consent.http.models.PaginationToken;
import org.broadinstitute.consent.http.models.PaginationResponse;
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
      validateUserIsCreator(user, collection);
      return Response.ok().entity(collection).build();
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
      validateUserIsCreator(user, collection);
      return Response.ok().entity(collection).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @GET
  @Path("query")
  @Produces("application/json")
  @RolesAllowed(RESEARCHER)
  public Response getCollectionsByInitialQuery(
    @Auth AuthUser authUser,
    @QueryParam("filterTerm") String filterTerm,
    @QueryParam("sortField") String sortField,
    @QueryParam("sortOrder") String sortOrder,
    @QueryParam("pageSize") int pageSize
  ) {
    try {
      User user = userService.findUserByEmail(authUser.getEmail());
      PaginationToken token = new PaginationToken(1, pageSize, sortField, sortOrder, filterTerm, defineAcceptableSortFields());
      PaginationResponse<DarCollection> paginationResponse = darCollectionService.getCollectionsWithFilters(token, user);
      return Response.ok().entity(paginationResponse).build();
    } catch(Exception e) {
      return createExceptionResponse(e);
    }
  }

  // A User should only see their own collection, regardless of the user's roles
  // We don't want to leak existence so throw a not found if someone tries to
  // view another user's collection.
  private void validateUserIsCreator(User user, DarCollection collection) {
    try {
      validateAuthedRoleUser(Collections.emptyList(), user, collection.getCreateUserId());
    } catch (ForbiddenException e) {
      throw new NotFoundException();
    }
  }

  private Map<String, String> defineAcceptableSortFields() {
    return Map.of(
      "projectTitle", "projectTitle",
      "researcher", "researcher",
      "darCode", "dar_code",
      "institution", "institution_name"
    );
  }
}
