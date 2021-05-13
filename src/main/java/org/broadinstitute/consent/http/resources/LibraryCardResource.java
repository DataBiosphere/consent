package org.broadinstitute.consent.http.resources;

import com.google.gson.Gson;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import java.util.List;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
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
// import org.broadinstitute.consent.http.service.LibraryCardService;
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
      User user = userService.findUserByEmail(authUser.getName()); //NOTE: may not need this? Check tech doc to make sure
      List<LibraryCard> libraryCards = libraryCardService.getLibraryCards();
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
      User user = userService.findUserByEmail(authUser.getName());
      LibraryCard libraryCard = libraryCardService.getLibraryCardById(id);
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
      User user = userService.findUserByEmail(authUser.getName());
      List<LibraryCard> libraryCards = libraryCardService.getLibraryCardsByInstitutionId(id);
      return Response.ok().entity(libraryCards).build();
    } catch(Exception e) {
      createExceptionResponse(e);
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
      LibraryCard newLibraryCard = libraryCardService.createLibraryCard(payload, user);
      return Response.ok().entity(newLibraryCard).build();
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
      LibraryCard updatedLibraryCard = libraryCardService.updateLibraryCard(id, payload);
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
      libraryCardService.deleteLibraryCard(id);
      return Response.status(204).build();
    } catch(Exception e) {
      return createExceptionResponse(e);
    }
  }
}
