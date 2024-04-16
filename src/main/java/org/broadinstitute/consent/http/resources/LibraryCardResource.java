package org.broadinstitute.consent.http.resources;

import com.google.api.client.http.HttpStatusCodes;
import com.google.gson.Gson;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.LibraryCard;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.LibraryCardService;
import org.broadinstitute.consent.http.service.UserService;
import org.broadinstitute.consent.http.util.gson.GsonUtil;

@Path("api/libraryCards")
public class LibraryCardResource extends Resource {

  private final UserService userService;
  private final LibraryCardService libraryCardService;

  @Inject
  public LibraryCardResource(
      UserService userService,
      LibraryCardService libraryCardService
  ) {
    this.userService = userService;
    this.libraryCardService = libraryCardService;
  }

  @GET
  @Produces("application/json")
  @RolesAllowed(ADMIN)
  public Response getLibraryCards(@Auth AuthUser authUser) {
    try {
      List<LibraryCard> libraryCards = libraryCardService.findAllLibraryCards();
      return Response.ok().entity(libraryCards).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @GET
  @Produces("application/json")
  @Path("/{id}")
  @RolesAllowed(ADMIN)
  public Response getLibraryCardById(@Auth AuthUser authUser, @PathParam("id") Integer id) {
    try {
      LibraryCard libraryCard = libraryCardService.findLibraryCardById(id);
      return Response.ok().entity(libraryCard).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @GET
  @Produces("application/json")
  @Path("/institution/{id}")
  @RolesAllowed({ADMIN})
  public Response getLibraryCardsByInstitutionId(@Auth AuthUser authUser,
      @PathParam("id") Integer id) {
    try {
      List<LibraryCard> libraryCards = libraryCardService.findLibraryCardsByInstitutionId(id);
      return Response.ok().entity(libraryCards).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @POST
  @Consumes("application/json")
  @Produces("application/json")
  @RolesAllowed({ADMIN, SIGNINGOFFICIAL})
  public Response createLibraryCard(@Auth AuthUser authUser, String libraryCard) {
    try {
      User user = userService.findUserByEmail(authUser.getEmail());
      LibraryCard payload = GsonUtil.gsonBuilderWithAdapters().create().fromJson(libraryCard, LibraryCard.class);
      payload.setCreateUserId(user.getUserId());
      LibraryCard newLibraryCard = libraryCardService.createLibraryCard(payload, user);
      return Response.status(HttpStatusCodes.STATUS_CODE_CREATED).entity(newLibraryCard).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @PUT
  @Consumes("application/json")
  @Produces("application/json")
  @Path("/{id}")
  @RolesAllowed(ADMIN)
  public Response updateLibraryCard(@Auth AuthUser authUser, @PathParam("id") Integer id,
      String libraryCard) {
    try {
      User user = userService.findUserByEmail(authUser.getEmail());
      LibraryCard payload = GsonUtil.gsonBuilderWithAdapters().create().fromJson(libraryCard, LibraryCard.class);
      LibraryCard updatedLibraryCard = libraryCardService.updateLibraryCard(payload, id,
          user.getUserId());
      return Response.ok().entity(updatedLibraryCard).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @DELETE
  @Produces("application/json")
  @Path("/{id}")
  @RolesAllowed({ADMIN, SIGNINGOFFICIAL})
  public Response deleteLibraryCard(@Auth AuthUser authUser, @PathParam("id") Integer id) {
    User user = userService.findUserByEmail(authUser.getEmail());
    LibraryCard card = libraryCardService.findLibraryCardById(id);
    try {
      // If user is not an admin and LC institutionID doesn't match the users's throw an exception
      if (!checkIsAdmin(user) && !card.getInstitutionId().equals(user.getInstitutionId())) {
        throw new ForbiddenException("You are not authorized to delete this library card");
      }
      libraryCardService.deleteLibraryCardById(id);
      return Response.status(204).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  private Boolean checkIsAdmin(User user) {
    return user.getRoles()
        .stream()
        .anyMatch(role -> role.getName().equalsIgnoreCase(UserRoles.ADMIN.getRoleName()));
  }
}
