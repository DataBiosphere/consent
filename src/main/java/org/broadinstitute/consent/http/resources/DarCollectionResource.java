package org.broadinstitute.consent.http.resources;

import com.google.gson.Gson;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.DarCollection;
import org.broadinstitute.consent.http.models.PaginationResponse;
import org.broadinstitute.consent.http.models.PaginationToken;
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
  @Path("role/{roleName}")
  @Produces("application/json")
  @RolesAllowed({ADMIN, CHAIRPERSON, MEMBER, SIGNINGOFFICIAL})
  public Response getCollectionsForUserByRole(@Auth AuthUser authUser, @PathParam("roleName") String roleName) {
    try {
      User user = userService.findUserByEmail(authUser.getEmail());
      validateUserHasRoleName(user, roleName);
      List<DarCollection> collections = darCollectionService.getCollectionsForUserByRoleName(user, roleName);
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
      PaginationToken token = new PaginationToken(1, pageSize, sortField, sortOrder, filterTerm, DarCollection.acceptableSortFields, DarCollection.defaultTokenSortField);
      PaginationResponse<DarCollection> paginationResponse = darCollectionService.getCollectionsWithFilters(token, user);
      return Response.ok().entity(paginationResponse).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @GET
  @Path("paginated")
  @Produces("application/json")
  @RolesAllowed(RESEARCHER)
  public Response getCollectionsByToken(
    @Auth AuthUser authUser,
    @QueryParam("token") String token
  ) {
    try {
      User user = userService.findUserByEmail(authUser.getEmail());
      String json = getDecodedJson(token);
      PaginationToken paginationToken = convertJsonToPaginationToken(json);
      PaginationResponse<DarCollection> paginationResponse = darCollectionService.getCollectionsWithFilters(paginationToken, user);
      return Response.ok(paginationResponse).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }


  @PUT
  @Path("{id}/cancel")
  @Produces("application/json")
  @RolesAllowed(RESEARCHER)
  public Response cancelDarCollectionByCollectionId(@Auth AuthUser authUser, @PathParam("id") Integer collectionId) {
    try {
      User user = userService.findUserByEmail(authUser.getEmail());
      DarCollection collection = darCollectionService.getByCollectionId(collectionId);
      isCollectionPresent(collection);
      validateUserIsCreator(user, collection);
      DarCollection cancelledCollection = darCollectionService.cancelDarCollection(collection);
      return Response.ok().entity(cancelledCollection).build();
    } catch(Exception e) {
      return createExceptionResponse(e);
    }
  }

  private void isCollectionPresent(DarCollection collection) {
    if(Objects.isNull(collection)) {
      throw new NotFoundException("Collection not found");
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

  private String getDecodedJson(String token) {
    if(Objects.isNull(token) || token.isEmpty()) {
      throw new BadRequestException("Token must be provided");
    }
    try{
      return new String(Base64.getDecoder().decode(token), StandardCharsets.UTF_8);
    } catch (Exception e) {
      throw new BadRequestException("Invalid pagination token");
    }
  }

  private PaginationToken convertJsonToPaginationToken(String json) {
    try {
      return new Gson().fromJson(json, PaginationToken.class);
    } catch (Exception e) {
      throw new BadRequestException("Invalid pagination token");
    }
  }
}
