package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.BadRequestException;
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
import jakarta.ws.rs.core.StreamingOutput;
import jakarta.ws.rs.core.UriInfo;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataAccessAgreement;
import org.broadinstitute.consent.http.models.LibraryCard;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.service.DaaService;
import org.broadinstitute.consent.http.service.DacService;
import org.broadinstitute.consent.http.service.EmailService;
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
  private final EmailService emailService;

  @Inject
  public DaaResource(DaaService daaService, DacService dacService, UserService userService,
      LibraryCardService libraryCardService, EmailService emailService) {
    this.daaService = daaService;
    this.dacService = dacService;
    this.userService = userService;
    this.libraryCardService = libraryCardService;
    this.emailService = emailService;
  }

  @POST
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed({ADMIN, CHAIRPERSON})
  @Path("/dac/{dacId}")
  public Response createDaaForDac(
      @Context UriInfo info,
      @Auth AuthUser authUser,
      @PathParam("dacId") Integer dacId,
      @FormDataParam("file") InputStream uploadInputStream,
      @FormDataParam("file") FormDataContentDisposition fileDetail) {
    try {
      validateFileDetails(fileDetail);
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
        LibraryCard createdLc = libraryCardService.createLibraryCardForSigningOfficial(user, authedUser);
        matchingCard = Optional.of(createdLc);
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

  @GET
  @PermitAll
  @Path("{daaId}/file")
  @Produces({MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_JSON})
  public Response findFileById(
      @PathParam("daaId") Integer daaId) {
    try {
      InputStream daa = daaService.findFileById(daaId);
      StreamingOutput stream = createStreamingOutput(daa);
      DataAccessAgreement daa2 = daaService.findById(daaId);
      String fileName = daa2.getFile().getFileName();
      return Response.ok(stream)
          .header("Content-Disposition", "attachment; filename=" + fileName)
          .build();
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

  @POST
  @PermitAll
  @Path("/request/{daaId}")
  public Response sendDaaRequestMessage(
      @Auth AuthUser authUser,
      @PathParam("daaId") Integer daaId) {
    try {
      User user = userService.findUserByEmail(authUser.getEmail());
      if (user.getInstitutionId() == null) {
        throw new BadRequestException("This user has not set their institution: " + user.getDisplayName());
      }
      if (user.getLibraryCards().stream()
          .anyMatch(libraryCard -> libraryCard.getDaaIds().contains(daaId))) {
        throw new IllegalArgumentException("User already has this DAA associated with their Library Card");
      }
      daaService.sendDaaRequestEmails(user, daaId);
      return Response.ok().build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @RolesAllowed({ADMIN, SIGNINGOFFICIAL})
  @Path("/bulk/{daaId}")
  public Response bulkAddUsersToDaa(
      @Auth AuthUser authUser,
      @PathParam("daaId") Integer daaId,
      String json) {
    try {
      User authedUser = userService.findUserByEmail(authUser.getEmail());
      List<User> users = userService.findUsersInJsonArray(json, "users");
      if (authedUser.hasUserRole(UserRoles.SIGNINGOFFICIAL)) {
        for (User user : users) {
          if (!Objects.equals(authedUser.getInstitutionId(), user.getInstitutionId())) {
            return Response.status(Status.FORBIDDEN).build();
          }
        }
      }
      daaService.findById(daaId);
      for (User user : users) {
        libraryCardService.addDaaToUserLibraryCardByInstitution(user, authedUser, daaId);
      }
      return Response.ok().build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @DELETE
  @Consumes(MediaType.APPLICATION_JSON)
  @RolesAllowed({ADMIN, SIGNINGOFFICIAL})
  @Path("/bulk/{daaId}")
  public Response bulkRemoveUsersFromDaa(
      @Auth AuthUser authUser,
      @PathParam("daaId") Integer daaId,
      String json) {
    try {
      User authedUser = userService.findUserByEmail(authUser.getEmail());
      List<User> users = userService.findUsersInJsonArray(json, "users");
      if (authedUser.hasUserRole(UserRoles.SIGNINGOFFICIAL)) {
        for (User user : users) {
          if (!Objects.equals(authedUser.getInstitutionId(), user.getInstitutionId())) {
            return Response.status(Status.FORBIDDEN).build();
          }
        }
      }
      daaService.findById(daaId);
      for (User user : users) {
        libraryCardService.removeDaaFromUserLibraryCardByInstitution(user, authedUser.getInstitutionId(), daaId);
      }
      return Response.ok().build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @RolesAllowed({ADMIN, SIGNINGOFFICIAL})
  @Path("/bulk/user/{userId}")
  public Response bulkAddDAAsToUser(
      @Auth AuthUser authUser,
      @PathParam("userId") Integer userId,
      String json) {
    try {
      User authedUser = userService.findUserByEmail(authUser.getEmail());
      User user = userService.findUserById(userId);
      if (authedUser.hasUserRole(UserRoles.SIGNINGOFFICIAL) && !Objects.equals(authedUser.getInstitutionId(), user.getInstitutionId())) {
        return Response.status(Status.FORBIDDEN).build();
      }
      List<DataAccessAgreement> daaList = daaService.findDAAsInJsonArray(json, "daaList");
      for (DataAccessAgreement daa : daaList) {
        libraryCardService.addDaaToUserLibraryCardByInstitution(user, authedUser, daa.getDaaId());
      }
      return Response.ok().build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @DELETE
  @Consumes(MediaType.APPLICATION_JSON)
  @RolesAllowed({ADMIN, SIGNINGOFFICIAL})
  @Path("/bulk/user/{userId}")
  public Response bulkRemoveDAAsFromUser(
      @Auth AuthUser authUser,
      @PathParam("userId") Integer userId,
      String json) {
    try {
      User authedUser = userService.findUserByEmail(authUser.getEmail());
      User user = userService.findUserById(userId);
      if (authedUser.hasUserRole(UserRoles.SIGNINGOFFICIAL) && !Objects.equals(authedUser.getInstitutionId(), user.getInstitutionId())) {
        return Response.status(Status.FORBIDDEN).build();
      }
      List<DataAccessAgreement> daaList = daaService.findDAAsInJsonArray(json, "daaList");
      for (DataAccessAgreement daa : daaList) {
        libraryCardService.removeDaaFromUserLibraryCardByInstitution(user, authedUser.getInstitutionId(), daa.getDaaId());
      }
      return Response.ok().build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @PUT
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed({ADMIN, CHAIRPERSON})
  @Path("{daaId}/dac/{dacId}")
  public Response addDacToDaa(
      @Auth AuthUser authUser,
      @PathParam("daaId") Integer daaId,
      @PathParam("dacId") Integer dacId) {
    try {
      dacService.findById(dacId);
      User user = userService.findUserByEmail(authUser.getEmail());
      // Assert that the user has the correct DAC permissions to add a DAC to a DAA for the provided DacId.
      // Admins can add a DAC to a DAA with any DAC, but chairpersons can only add DACs to DAAs for DACs they are a
      // chairperson for.
      if (!user.hasUserRole(UserRoles.ADMIN)) {
        List<Integer> dacIds = user.getRoles().stream().map(UserRole::getDacId).toList();
        if (!dacIds.contains(dacId)) {
          return Response.status(Status.FORBIDDEN).build();
        }
      }
      DataAccessAgreement daa = daaService.findById(daaId);
      Optional<Dac> matchingDac = Optional.empty();
      if (daa.getDacs() != null) {
        matchingDac = daa.getDacs().stream()
            .filter(dac -> Objects.equals(dac.getDacId(), dacId))
            .findFirst();
      }
      if (matchingDac.isEmpty()) {
        daaService.addDacToDaa(dacId,daaId);
      }
      DataAccessAgreement updatedDaa = daaService.findById(daaId);
      return Response.ok().entity(updatedDaa).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }
}
