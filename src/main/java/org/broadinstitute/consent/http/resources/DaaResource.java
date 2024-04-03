package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.UriInfo;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.DataAccessAgreement;
import org.broadinstitute.consent.http.models.LibraryCard;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.service.DaaService;
import org.broadinstitute.consent.http.service.DacService;
import org.broadinstitute.consent.http.service.LibraryCardService;
import org.broadinstitute.consent.http.service.UserService;
import org.broadinstitute.consent.http.util.ConsentLogger;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

@Path("api/daa")
public class DaaResource extends Resource implements ConsentLogger {

  private final DaaService daaService;
  private final DacService dacService;
  private final UserService userService;
  private final LibraryCardService libraryCardService;

  @Inject
  public DaaResource(DaaService daaService, DacService dacService, UserService userService,
      LibraryCardService libraryCardService) {
    this.daaService = daaService;
    this.dacService = dacService;
    this.userService = userService;
    this.libraryCardService = libraryCardService;
  }

  @POST
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed({ADMIN, CHAIRPERSON})
  @Path("/daa/{dacId}")
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
      DataAccessAgreement daa = daaService.createDaaWithFso(user.getUserId(), dacId,
          uploadInputStream, fileDetail);
      URI uri = info.getBaseUriBuilder()
          // This will be the GET endpoint for the created DAA
          .replacePath("api/daa/{daaId}")
          .build(daa.getDaaId());
      return Response.created(uri).entity(daa).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @PUT
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed({ADMIN, SIGNINGOFFICIAL})
  @Path("{daaId}/{userId}")
  public Response createLibraryCardDaaRelation(
      @Context UriInfo info,
      @Auth AuthUser authUser,
      @PathParam("daaId") Integer daaId,
      @PathParam("userId") Integer userId) {
    try {
      User authedUser = userService.findUserByEmail(authUser.getEmail());
      int authedUserInstitutionId = authedUser.getInstitutionId();
      User user = userService.findUserById(userId);
      int userInstitutionId = user.getInstitutionId();
      // Assert that the user has the correct institution permissions to add a DAA-LC relationship.
      // Admins can add a DAA with any DAC, but signing officials can only create relationships for
      // library cards associated with the same institution they are associated with.
      if (!authedUser.hasUserRole(UserRoles.ADMIN) && !authedUser.hasUserRole(UserRoles.SIGNINGOFFICIAL)) {
        return Response.status(Status.FORBIDDEN).build();
      } else if (authedUser.hasUserRole(UserRoles.SIGNINGOFFICIAL) && authedUserInstitutionId != userInstitutionId) {
          return Response.status(Status.FORBIDDEN).build();
      }
      List<LibraryCard> libraryCards = libraryCardService.findLibraryCardsByUserId(userId);
      Optional<LibraryCard> matchingCard = libraryCards.stream()
          .filter(card -> card.getInstitutionId() == authedUser.getInstitutionId().intValue())
          .findFirst();
      if (matchingCard.isEmpty()) {
        return Response.status(Status.NOT_FOUND).build();
      }
      int libraryCardId = matchingCard.get().getId();
      libraryCardService.addDaaToLibraryCard(libraryCardId, daaId);
      URI uri = info.getBaseUriBuilder()
          .replacePath("api/libraryCards/{libraryCardId}")
          .build(libraryCardId);
      return Response.ok().location(uri).entity(matchingCard.get()).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @PermitAll
  public Response findAll() {
    try {
      List<DataAccessAgreement> daas = daaService.findAll();
      return Response.ok(daas).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed({ADMIN, MEMBER, CHAIRPERSON, RESEARCHER})
  @Path("{daaId}")
  public Response findById(
      @PathParam("daaId") Integer daaId) {
    try {
      DataAccessAgreement daa = daaService.findById(daaId);
      return Response.ok(daa).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @DELETE
  @RolesAllowed({ADMIN, SIGNINGOFFICIAL})
  @Path("{daaId}/{userId}")
  public Response deleteDaaForUser(
      @Auth AuthUser authUser,
      @PathParam("daaId") Integer daaId,
      @PathParam("userId") Integer userId) {
    try {
      User user = userService.findUserByEmail(authUser.getEmail());
      if (!user.hasUserRole(UserRoles.ADMIN)) {
        return Response.status(Status.FORBIDDEN).build();
      }
      List<LibraryCard> libraryCards = libraryCardService.findLibraryCardsByUserId(userId);
      for (LibraryCard libraryCard : libraryCards) {
        libraryCardService.removeDaaFromLibraryCard(libraryCard.getId(), daaId);
      }
      return Response.ok().build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }
}
