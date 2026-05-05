package org.broadinstitute.consent.http.resources;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
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
import java.lang.reflect.Type;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.DaaBulkAssignmentResult;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataAccessAgreement;
import org.broadinstitute.consent.http.models.DuosUser;
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
  public DaaResource(
      DaaService daaService,
      DacService dacService,
      UserService userService,
      LibraryCardService libraryCardService) {
    this.daaService = daaService;
    this.dacService = dacService;
    this.userService = userService;
    this.libraryCardService = libraryCardService;
  }

  @POST
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed({CHAIRPERSON})
  @Path("/dac/{dacId}")
  public Response createDaaForDac(
      @Context UriInfo info,
      @Auth DuosUser duosUser,
      @PathParam("dacId") Integer dacId,
      @FormDataParam("file") InputStream uploadInputStream,
      @FormDataParam("file") FormDataContentDisposition fileDetail) {
    try {
      validateFileDetails(fileDetail);
      dacService.findById(dacId);
      User user = duosUser.getUser();
      // Assert that the user has the correct DAC permissions to add a DAA for the provided dacId.
      List<Integer> chairpersonDacIds =
          user.getRoles().stream()
              .filter(role -> Objects.equals(role.getRoleId(), UserRoles.CHAIRPERSON.getRoleId()))
              .map(UserRole::getDacId)
              .filter(Objects::nonNull)
              .toList();
      if (!chairpersonDacIds.contains(dacId)) {
        return Response.status(Status.FORBIDDEN).build();
      }
      DataAccessAgreement daa =
          daaService.createDaaWithFso(user.getUserId(), dacId, uploadInputStream, fileDetail);
      URI uri =
          info.getBaseUriBuilder()
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
  @RolesAllowed({SIGNINGOFFICIAL})
  @Path("{daaId}/{userId}")
  public Response createLibraryCardDaaRelation(
      @Context UriInfo info,
      @Auth DuosUser duosUser,
      @PathParam("daaId") Integer daaId,
      @PathParam("userId") Integer userId) {
    try {
      User authedUser = duosUser.getUser();
      Integer authedUserInstitutionId = authedUser.getInstitutionId();
      User user = userService.findUserById(userId);
      Integer userInstitutionId = user.getInstitutionId();
      // Assert that the user has the correct institution permissions to add a DAA-LC relationship.
      // Signing officials can only create relationships for library cards associated with the same
      // institution they are associated with.
      if (authedUserInstitutionId == null || userInstitutionId == null) {
        return Response.status(Status.BAD_REQUEST)
            .entity("Both the signing official and the target user must have an institutionId set.")
            .build();
      }
      if (!Objects.equals(authedUserInstitutionId, userInstitutionId)) {
        return Response.status(Status.FORBIDDEN).build();
      }
      LibraryCard libraryCard =
          user.getLibraryCard() == null
              ? libraryCardService.createLibraryCardForSigningOfficial(user, authedUser)
              : user.getLibraryCard();
      libraryCardService.addDaaToLibraryCard(
          user.getUserId(), authedUser.getUserId(), libraryCard.getId(), daaId);
      LibraryCard updatedLibraryCard =
          libraryCardService.findLibraryCardWithDaasById(libraryCard.getId());
      URI uri =
          info.getBaseUriBuilder()
              .replacePath("api/libraryCards/{libraryCardId}")
              .build(libraryCard.getId());
      return Response.ok().location(uri).entity(updatedLibraryCard).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @PermitAll
  public Response findAll(@Auth DuosUser ignoredDuosUser) {
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
  public Response findDaaById(@Auth DuosUser ignoredDuosUser, @PathParam("daaId") Integer daaId) {
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
  public Response findFileById(@Auth DuosUser ignoredDuosUser, @PathParam("daaId") Integer daaId) {
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
  @RolesAllowed({SIGNINGOFFICIAL})
  @Path("{daaId}/{userId}")
  public Response deleteDaaForUser(
      @Auth DuosUser duosUser,
      @PathParam("daaId") Integer daaId,
      @PathParam("userId") Integer userId) {
    try {
      User authedUser = duosUser.getUser();
      User user = userService.findUserById(userId);
      if (!Objects.equals(authedUser.getInstitutionId(), user.getInstitutionId())) {
        return Response.status(Status.FORBIDDEN).build();
      }
      if (user.getLibraryCard() != null) {
        libraryCardService.removeDaaFromLibraryCard(
            user.getUserId(), authedUser.getUserId(), user.getLibraryCard().getId(), daaId);
      }
      return Response.ok().build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @RolesAllowed({SIGNINGOFFICIAL})
  @Path("/bulk/{daaId}")
  public Response bulkAddUsersToDaa(
      @Auth DuosUser duosUser, @PathParam("daaId") Integer daaId, String json) {
    try {
      User authedUser = duosUser.getUser();
      List<User> users = userService.findUsersInJsonArray(json, "users");
      for (User user : users) {
        if (!Objects.equals(authedUser.getInstitutionId(), user.getInstitutionId())) {
          return Response.status(Status.FORBIDDEN).build();
        }
      }
      daaService.findById(daaId);
      for (User user : users) {
        libraryCardService.addDaaToUserLibraryCard(user, authedUser, daaId);
      }
      return Response.ok().build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed({ADMIN})
  @Path("/{daaId}/assign-all-eligible-users")
  public Response assignDaaToAllEligibleUsers(
      @Auth DuosUser duosUser, @PathParam("daaId") Integer daaId) {
    try {
      User authedUser = duosUser.getUser();
      DaaBulkAssignmentResult result = daaService.assignDaaToAllEligibleUsers(daaId, authedUser);
      return Response.ok(result).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @DELETE
  @Consumes(MediaType.APPLICATION_JSON)
  @RolesAllowed({SIGNINGOFFICIAL})
  @Path("/bulk/{daaId}")
  public Response bulkRemoveUsersFromDaa(
      @Auth DuosUser duosUser, @PathParam("daaId") Integer daaId, String json) {
    try {
      User authedUser = duosUser.getUser();
      List<User> users = userService.findUsersInJsonArray(json, "users");
      for (User user : users) {
        if (!Objects.equals(authedUser.getInstitutionId(), user.getInstitutionId())) {
          return Response.status(Status.FORBIDDEN).build();
        }
      }
      daaService.findById(daaId);
      for (User user : users) {
        libraryCardService.removeDaaFromUserLibraryCard(user, authedUser, daaId);
      }
      return Response.ok().build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @RolesAllowed({SIGNINGOFFICIAL})
  @Path("/bulk/user/{userId}")
  public Response bulkAddDAAsToUser(
      @Auth DuosUser duosUser, @PathParam("userId") Integer userId, String json) {
    try {
      User authedUser = duosUser.getUser();
      User user = userService.findUserById(userId);
      if (!Objects.equals(authedUser.getInstitutionId(), user.getInstitutionId())) {
        return Response.status(Status.FORBIDDEN).build();
      }
      List<DataAccessAgreement> daaList = daaService.findDAAsInJsonArray(json, "daaList");
      for (DataAccessAgreement daa : daaList) {
        libraryCardService.addDaaToUserLibraryCard(user, authedUser, daa.getDaaId());
      }
      return Response.ok().build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @DELETE
  @Consumes(MediaType.APPLICATION_JSON)
  @RolesAllowed({SIGNINGOFFICIAL})
  @Path("/bulk/user/{userId}")
  public Response bulkRemoveDAAsFromUser(
      @Auth DuosUser duosUser, @PathParam("userId") Integer userId, String json) {
    try {
      User authedUser = duosUser.getUser();
      User user = userService.findUserById(userId);
      if (!Objects.equals(authedUser.getInstitutionId(), user.getInstitutionId())) {
        return Response.status(Status.FORBIDDEN).build();
      }
      List<DataAccessAgreement> daaList = daaService.findDAAsInJsonArray(json, "daaList");
      for (DataAccessAgreement daa : daaList) {
        libraryCardService.removeDaaFromUserLibraryCard(user, authedUser, daa.getDaaId());
      }
      return Response.ok().build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @PUT
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed({CHAIRPERSON})
  @Path("/{daaId}/dac/{dacId}")
  public Response modifyDacDaaRelationship(
      @Auth DuosUser duosUser,
      @PathParam("daaId") Integer daaId,
      @PathParam("dacId") Integer dacId) {
    try {
      dacService.findById(dacId);
      User user = duosUser.getUser();
      // Assert that the user has the correct DAC permissions to add a DAC to a DAA for the provided
      // DacId.
      if (user.getRoles().stream()
          .filter(r -> r.getRoleId().equals(UserRoles.Chairperson().getRoleId()))
          .map(UserRole::getDacId)
          .noneMatch(dacId::equals)) {
        return Response.status(Status.FORBIDDEN).build();
      }
      DataAccessAgreement daa = daaService.findById(daaId);
      Optional<Dac> matchingDac = Optional.empty();
      if (daa.getDacs() != null) {
        matchingDac =
            daa.getDacs().stream().filter(dac -> Objects.equals(dac.getDacId(), dacId)).findFirst();
      }
      if (matchingDac.isEmpty()) {
        daaService.addDacToDaa(user.getUserId(), dacId, daaId);
      }
      DataAccessAgreement updatedDaa = daaService.findById(daaId);
      return Response.ok().entity(updatedDaa).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @DELETE
  @Consumes(MediaType.APPLICATION_JSON)
  @RolesAllowed({CHAIRPERSON})
  @Path("/{daaId}/dac/{dacId}")
  public Response removeDacDaaRelationship(
      @Auth DuosUser duosUser,
      @PathParam("daaId") Integer daaId,
      @PathParam("dacId") Integer dacId) {
    try {
      dacService.findById(dacId);
      User user = duosUser.getUser();
      // Assert that the user has the correct DAC permissions to add a DAC to a DAA for the provided
      // DacId.
      List<Integer> matchedChairpersonDacIds =
          user.getRoles().stream()
              .filter(r -> r.getRoleId().equals(UserRoles.Chairperson().getRoleId()))
              .map(UserRole::getDacId)
              .filter(id -> Objects.equals(id, dacId))
              .toList();
      if (matchedChairpersonDacIds.isEmpty()) {
        return Response.status(Status.FORBIDDEN).build();
      }
      DataAccessAgreement daa = daaService.findById(daaId);
      Optional<Dac> matchingDac = Optional.empty();
      if (daa.getDacs() != null) {
        matchingDac =
            daa.getDacs().stream().filter(dac -> Objects.equals(dac.getDacId(), dacId)).findFirst();
      }
      if (matchingDac.isEmpty()) {
        throw new BadRequestException("The given DAC is not associated with the provided DAA.");
      } else {
        daaService.removeDacFromDaa(user.getUserId(), dacId, daaId);
      }
      DataAccessAgreement updatedDaa = daaService.findById(daaId);
      return Response.ok().entity(updatedDaa).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @POST
  @RolesAllowed({CHAIRPERSON})
  @Path("{dacId}/updated/{oldDaaId}/{newDaaName}")
  public Response sendNewDaaMessage(
      @Auth DuosUser duosUser,
      @PathParam("dacId") Integer dacId,
      @PathParam("oldDaaId") Integer oldDaaId,
      @PathParam("newDaaName") String newDaaName) {
    try {
      daaService.findById(oldDaaId);
      User user = duosUser.getUser();
      Dac dac = dacService.findById(dacId);
      String dacName = dac.getName();
      daaService.sendNewDaaEmails(user, oldDaaId, dacName, newDaaName);
      return Response.ok().build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @POST
  @RolesAllowed({ADMIN, CHAIRPERSON, MEMBER, SIGNINGOFFICIAL, RESEARCHER})
  @Path("datasets")
  public Response findDaaForDatasets(@Auth DuosUser duosUser, String json) {
    try {
      Gson gson = new Gson();
      Type setType = new TypeToken<Set<Integer>>() {}.getType();
      Set<Integer> set = gson.fromJson(json, setType);
      return Response.ok(daaService.findDaaIdsByDatasetIds(set)).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }
}
