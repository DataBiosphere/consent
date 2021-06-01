package org.broadinstitute.consent.http.resources;

import com.google.api.client.http.HttpStatusCodes;
import com.google.gson.Gson;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import java.util.List;
import java.util.Objects;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.LibraryCard;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.LibraryCardService;
import org.broadinstitute.consent.http.service.UserService;

@Path("api/libraryCards")
public class LibraryCardResource extends Resource{
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
    try{
      List<LibraryCard> libraryCards = libraryCardService.findAllLibraryCards();
      return Response.ok().entity(libraryCards).build();
    } catch(Exception e) {
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
    } catch(Exception e) {
      return createExceptionResponse(e);
    }
  }

  @GET
  @Produces("application/json")
  @Path("/institution/{id}")
  @RolesAllowed(ADMIN)
  public Response getLibraryCardsByInstitutionId(@Auth AuthUser authUser, @PathParam("id") Integer id) {
    try{
      List<LibraryCard> libraryCards = libraryCardService.findLibraryCardsByInstitutionId(id);
      return Response.ok().entity(libraryCards).build();
    } catch(Exception e) {
      return createExceptionResponse(e);
    }
  }

  @POST
  @Consumes("application/json")
  @Produces("application/json")
  @RolesAllowed(ADMIN)
  public Response createLibraryCard(@Auth AuthUser authUser, String libraryCard) {
    try{
      User user = userService.findUserByEmail(authUser.getName());
      LibraryCard payload = new Gson().fromJson(libraryCard, LibraryCard.class);
      User lcUser = userService.findUserByEmail(payload.getUserEmail());

      if (Objects.isNull(lcUser)) {
        throw new BadRequestException("User email does not belong to a valid user.");
      }

      payload.setUserId(lcUser.getDacUserId());
      LibraryCard newLibraryCard = libraryCardService.createLibraryCard(payload, user.getDacUserId());
      return Response.status(HttpStatusCodes.STATUS_CODE_CREATED).entity(newLibraryCard).build();
    } catch(Exception e) {
      return createExceptionResponse(e);
    }
  }

  @PUT
  @Consumes("application/json")
  @Produces("application/json")
  @Path("/{id}")
  @RolesAllowed(ADMIN)
  public Response updateLibraryCard(@Auth AuthUser authUser, @PathParam("id") Integer id, String libraryCard) {
    try {
      User user = userService.findUserByEmail(authUser.getName());
      LibraryCard payload = new Gson().fromJson(libraryCard, LibraryCard.class);
      LibraryCard updatedLibraryCard = libraryCardService.updateLibraryCard(payload, id, user.getDacUserId());
      return Response.ok().entity(updatedLibraryCard).build();
    } catch(Exception e) {
      return createExceptionResponse(e);
    }
  }

  @DELETE
  @Produces("application/json")
  @Path("/{id")
  @RolesAllowed(ADMIN)
  public Response deleteLibraryCard(@Auth AuthUser authUser, @PathParam("id") Integer id) {
    try {
      libraryCardService.deleteLibraryCardById(id);
      return Response.status(204).build();
    } catch(Exception e) {
      return createExceptionResponse(e);
    }
  }
}
