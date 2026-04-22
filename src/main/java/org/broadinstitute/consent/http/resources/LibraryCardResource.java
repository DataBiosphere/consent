package org.broadinstitute.consent.http.resources;

import com.google.api.client.http.HttpStatusCodes;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.models.LibraryCard;
import org.broadinstitute.consent.http.models.LibraryCardDaaAudit;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.LibraryCardService;
import org.broadinstitute.consent.http.service.UserService;
import org.broadinstitute.consent.http.service.feature.InstitutionAndLibraryCardEnforcement;
import org.broadinstitute.consent.http.util.gson.GsonUtil;

@Path("api/libraryCards")
public class LibraryCardResource extends Resource {

  private final UserService userService;
  private final LibraryCardService libraryCardService;
  private final InstitutionAndLibraryCardEnforcement institutionAndLibraryCardEnforcement;

  @Inject
  public LibraryCardResource(
      UserService userService,
      LibraryCardService libraryCardService,
      InstitutionAndLibraryCardEnforcement institutionAndLibraryCardEnforcement) {
    this.userService = userService;
    this.libraryCardService = libraryCardService;
    this.institutionAndLibraryCardEnforcement = institutionAndLibraryCardEnforcement;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed(ADMIN)
  public Response getLibraryCards(@SuppressWarnings("unused") @Auth DuosUser duosUser) {
    try {
      List<LibraryCard> libraryCards = libraryCardService.findAllLibraryCards();
      return Response.ok().entity(libraryCards).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/{id}")
  @RolesAllowed(ADMIN)
  public Response getLibraryCardById(
      @SuppressWarnings("unused") @Auth DuosUser duosUser, @PathParam("id") Integer id) {
    try {
      LibraryCard libraryCard = libraryCardService.findLibraryCardById(id);
      return Response.ok().entity(libraryCard).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/institution/{id}")
  @RolesAllowed({ADMIN})
  public Response getLibraryCardsByInstitutionId(
      @SuppressWarnings("unused") @Auth DuosUser duosUser, @PathParam("id") Integer id) {
    try {
      List<LibraryCard> libraryCards = libraryCardService.findLibraryCardsByInstitutionId(id);
      return Response.ok().entity(libraryCards).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed({SIGNINGOFFICIAL})
  public Response createLibraryCard(@Auth DuosUser duosUser, String libraryCard) {
    try {
      User user = duosUser.getUser();
      LibraryCard payload = GsonUtil.getInstance().fromJson(libraryCard, LibraryCard.class);
      payload.setCreateUserId(user.getUserId());
      LibraryCard newLibraryCard = libraryCardService.createLibraryCard(payload, user);
      return Response.status(HttpStatusCodes.STATUS_CODE_CREATED).entity(newLibraryCard).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @DELETE
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/{id}")
  @RolesAllowed({ADMIN, SIGNINGOFFICIAL})
  public Response deleteLibraryCard(@Auth DuosUser duosUser, @PathParam("id") Integer id) {
    User user = duosUser.getUser();
    LibraryCard card = libraryCardService.findLibraryCardById(id);
    User lcUser = null;
    try {
      lcUser = userService.findUserById(card.getUserId());
    } catch (NotFoundException nfe) {
      // LC User can be null - do not need to error here
    }
    try {
      // If user is not an admin and SO institutionID doesn't match the user's throw an exception
      if (lcUser != null
          && !checkIsAdmin(user)
          && !lcUser.getInstitution().equals(user.getInstitution())) {
        throw new ForbiddenException("You are not authorized to delete this library card");
      }
      libraryCardService.deleteLibraryCardById(id);
      return Response.status(204).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/history/{userId}")
  @RolesAllowed({ADMIN, SIGNINGOFFICIAL})
  public Response findLibraryCardDaaAuditsByUserId(
      @Auth DuosUser duosUser, @PathParam("userId") Integer userId) {
    try {
      User libraryCardUser = userService.findUserById(userId);
      User authedUser = duosUser.getUser();
      if (!checkIsAdmin(authedUser)) {
        institutionAndLibraryCardEnforcement.validateEmailsFromSameInstitution(
            authedUser.getEmail(), libraryCardUser.getEmail());
      }
      List<LibraryCardDaaAudit> audits =
          libraryCardService.findLibraryCardDaaAuditsByUserId(userId);
      return Response.ok().entity(audits).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  private boolean checkIsAdmin(User user) {
    return user.getRoles().stream()
        .anyMatch(role -> role.getName().equalsIgnoreCase(UserRoles.ADMIN.getRoleName()));
  }
}
