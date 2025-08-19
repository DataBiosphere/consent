package org.broadinstitute.consent.http.resources;


import com.codahale.metrics.annotation.Timed;
import com.google.api.client.http.HttpStatusCodes;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
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
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.Acknowledgement;
import org.broadinstitute.consent.http.models.ApprovedDataset;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.models.Error;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.models.UserUpdateFields;
import org.broadinstitute.consent.http.service.AcknowledgementService;
import org.broadinstitute.consent.http.service.DatasetService;
import org.broadinstitute.consent.http.service.UserService;
import org.broadinstitute.consent.http.service.UserService.SimplifiedUser;
import org.broadinstitute.consent.http.service.sam.SamService;

@Path("api/user")
public class UserResource extends Resource {

  private final UserService userService;
  private final Gson gson = new Gson();
  private final SamService samService;
  private final DatasetService datasetService;
  private final AcknowledgementService acknowledgementService;

  @Inject
  public UserResource(SamService samService, UserService userService,
      DatasetService datasetService, AcknowledgementService acknowledgementService) {
    this.samService = samService;
    this.userService = userService;
    this.datasetService = datasetService;
    this.acknowledgementService = acknowledgementService;
  }

  @GET
  @Produces("application/json")
  @Path("/role/{roleName}")
  @RolesAllowed({ADMIN, SIGNINGOFFICIAL})
  public Response getUsers(@Auth AuthUser authUser, @PathParam("roleName") String roleName) {
    try {
      User user = userService.findUserByEmail(authUser.getEmail());
      boolean valid = UserRoles.isValidRole(roleName);
      if (valid) {
        //if there is a valid roleName but it is not SO or Admin then throw an exception
        if (!roleName.equals(UserRoles.ADMIN.getRoleName()) && !roleName.equals(
            UserRoles.SIGNINGOFFICIAL.getRoleName())) {
          throw new BadRequestException("Unsupported role name: " + roleName);
        }
        if (!user.hasUserRole(UserRoles.getUserRoleFromName(roleName))) {
          throw new NotFoundException(
              "User: " + user.getDisplayName() + ", does not have " + roleName + " role.");
        }
        List<User> users = userService.getUsersAsRole(user, roleName);
        return Response.ok().entity(users).build();
      } else {
        throw new BadRequestException("Invalid role name: " + roleName);
      }
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @GET
  @Path("/me")
  @Produces("application/json")
  @PermitAll
  @Timed
  public Response getUser(@Auth AuthUser authUser) {
    try {
      User user = userService.findUserByEmail(authUser.getEmail());
      if (Objects.isNull(authUser.getUserStatusInfo())) {
        samService.asyncPostRegistrationInfo(authUser);
      }
      JsonObject userJson = userService.findUserWithPropertiesByIdAsJsonObject(authUser,
          user.getUserId());
      return Response.ok(gson.toJson(userJson)).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @Deprecated // Use getDatasetsFromUserDacsV2
  @GET
  @Path("/me/dac/datasets")
  @Produces("application/json")
  @RolesAllowed({CHAIRPERSON, MEMBER})
  public Response getDatasetsFromUserDacs(@Auth AuthUser authUser) {
    return getDatasetsFromUserDacsV2(authUser);
  }

  @GET
  @Path("/me/dac/datasets/v2")
  @Produces("application/json")
  @RolesAllowed({CHAIRPERSON, MEMBER})
  public Response getDatasetsFromUserDacsV2(@Auth AuthUser authUser) {
    try {
      User user = userService.findUserByEmail(authUser.getEmail());
      List<Integer> dacIds = user.getRoles().stream()
          .map(UserRole::getDacId)
          .filter(Objects::nonNull)
          .collect(Collectors.toList());
      List<Dataset> datasets =
          dacIds.isEmpty() ? List.of() : datasetService.findDatasetListByDacIds(dacIds);
      if (datasets.isEmpty()) {
        throw new NotFoundException("No datasets found for current user");
      }
      return Response.ok().entity(datasets).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @GET
  @Path("/{userId}")
  @Produces("application/json")
  @RolesAllowed({ADMIN, CHAIRPERSON, MEMBER, DATASUBMITTER, SIGNINGOFFICIAL})
  public Response getUserById(@Auth AuthUser authUser, @PathParam("userId") Integer userId) {
    try {
      JsonObject userJson = userService.findUserWithPropertiesByIdAsJsonObject(authUser, userId);
      return Response.ok(gson.toJson(userJson)).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @GET
  @Path("/institution/unassigned")
  @Produces("application/json")
  @RolesAllowed({ADMIN, SIGNINGOFFICIAL})
  public Response getUnassignedUsers(@Auth AuthUser user) {
    try {
      List<User> unassignedUsers = userService.findUsersWithNoInstitution();
      return Response.ok().entity(unassignedUsers).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @GET
  @Path("/institution/{institutionId}")
  @Produces("application/json")
  @RolesAllowed({ADMIN})
  public Response getUsersByInstitution(
      @Auth AuthUser user, @PathParam("institutionId") Integer institutionId) {
    try {
      List<User> users = userService.findUsersByInstitutionId(institutionId);
      return Response.ok().entity(users).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @PUT
  @Path("/{id}")
  @Consumes("application/json")
  @Produces("application/json")
  @RolesAllowed({ADMIN})
  public Response update(@Auth AuthUser authUser, @Context UriInfo info,
      @PathParam("id") Integer userId, String json) {
    try {
      UserUpdateFields userUpdateFields = gson.fromJson(json, UserUpdateFields.class);
      // Ensure that we have a real user with this ID, fail if we do not.
      userService.findUserById(userId);
      User updatedUser = userService.updateUserFieldsById(userUpdateFields, userId);
      Gson gson = new Gson();
      JsonObject jsonUser = userService.findUserWithPropertiesByIdAsJsonObject(authUser,
          updatedUser.getUserId());
      return Response.ok().entity(gson.toJson(jsonUser)).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @PUT
  @Consumes("application/json")
  @Produces("application/json")
  @PermitAll
  public Response updateSelf(@Auth AuthUser authUser, @Context UriInfo info, String json) {
    try {
      User user = userService.findUserByEmail(authUser.getEmail());
      UserUpdateFields userUpdateFields = gson.fromJson(json, UserUpdateFields.class);

      // Users cannot update their own institution id through this service
      if (userUpdateFields.getInstitutionId() != null) {
        throw new BadRequestException("Institution ID is not updatable");
      }

      if (Objects.nonNull(userUpdateFields.getUserRoleIds()) && !user.hasUserRole(
          UserRoles.ADMIN)) {
        throw new BadRequestException("Cannot change user's roles.");
      }

      user = userService.updateUserFieldsById(userUpdateFields, user.getUserId());
      Gson gson = new Gson();
      JsonObject jsonUser = userService.findUserWithPropertiesByIdAsJsonObject(authUser,
          user.getUserId());

      return Response.ok().entity(gson.toJson(jsonUser)).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @PUT
  @Path("/{userId}/{roleId}")
  @Produces("application/json")
  @RolesAllowed({ADMIN, SIGNINGOFFICIAL})
  public Response addRoleToUser(@Auth AuthUser authUser, @PathParam("userId") Integer userId,
      @PathParam("roleId") Integer roleId) {
    UserRoles targetRole = UserRoles.getUserRoleFromId(roleId);
    if (Objects.isNull(targetRole)) {
      return Response.status(HttpStatusCodes.STATUS_CODE_BAD_REQUEST).build();
    }
    UserRole role = new UserRole(roleId, targetRole.getRoleName());
    try {
      User activeUser = userService.findUserByEmail(authUser.getEmail());
      User user = userService.findUserById(userId);
      List<Integer> currentUserRoleIds = user.getUserRoleIdsFromUser();
      if ((activeUser.hasUserRole(UserRoles.ADMIN) && UserRoles.isValidNonDACRoleId(targetRole)) ||
          signingOfficialMeetsRequirements(targetRole, activeUser, user)) {
        if (!currentUserRoleIds.contains(roleId)) {
          userService.insertRoleAndInstitutionForUser(role, user);
          return getUserResponse(authUser, userId);
        } else {
          return Response.notModified().build();
        }
      } else {
        return Response.status(HttpStatusCodes.STATUS_CODE_BAD_REQUEST).build();
      }
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  private static boolean signingOfficialMeetsRequirements(UserRoles role, User activeUser,
      User user) {
    return activeUser.hasUserRole(UserRoles.SIGNINGOFFICIAL)
        && activeUser.getInstitutionId() != null
        && UserRoles.isValidSoAdjustableRoleId(role)
        && (user.getInstitutionId() == null || user.getInstitutionId()
        .equals(activeUser.getInstitutionId()));
  }

  private Response getUserResponse(AuthUser authUser, Integer userId) {
    JsonObject userJson = userService.findUserWithPropertiesByIdAsJsonObject(authUser, userId);
    return Response.ok().entity(gson.toJson(userJson)).build();
  }

  @DELETE
  @Path("/{userId}/{roleId}")
  @Produces("application/json")
  @RolesAllowed({ADMIN, SIGNINGOFFICIAL})
  public Response deleteRoleFromUser(@Auth AuthUser authUser, @PathParam("userId") Integer userId,
      @PathParam("roleId") Integer roleId) {
    UserRoles targetRole = UserRoles.getUserRoleFromId(roleId);
    if (Objects.isNull(targetRole)) {
      return Response.status(HttpStatusCodes.STATUS_CODE_BAD_REQUEST).build();
    }
    try {
      User activeUser = userService.findUserByEmail(authUser.getEmail());
      User user = userService.findUserById(userId);
      if (activeUser.hasUserRole(UserRoles.ADMIN)) {
        if (!UserRoles.isValidNonDACRoleId(targetRole)) {
          throw new BadRequestException("Invalid Role Id");
        }
        return doDelete(authUser, userId, roleId, activeUser, user);
      } else if (activeUser.hasUserRole(UserRoles.SIGNINGOFFICIAL)) {
        if (!UserRoles.isValidSoAdjustableRoleId(targetRole)) {
          throw new ForbiddenException(
              "A Signing Official may only remove the following role ids: [7, 8, 9] ");
        }
        if (Objects.equals(user.getUserId(), activeUser.getUserId())
            && (UserRoles.getUserRoleFromId(roleId) == UserRoles.SIGNINGOFFICIAL)) {
          throw new BadRequestException(
              "You cannot remove the SIGNINGOFFICIAL role from yourself.");
        }
        if (Objects.nonNull(activeUser.getInstitutionId())
            && Objects.equals(activeUser.getInstitutionId(), user.getInstitutionId())) {
          return doDelete(authUser, userId, roleId, activeUser, user);
        } else {
          throw new ForbiddenException("Not authorized to remove roles");
        }
      } else {
        throw new ForbiddenException("Not authorized to remove roles.");
      }
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  private Response doDelete(AuthUser authUser, Integer userId, Integer roleId, User activeUser,
      User user) {
    List<Integer> currentUserRoleIds = user.getUserRoleIdsFromUser();
    if (!currentUserRoleIds.contains(roleId)) {
      JsonObject userJson = userService.findUserWithPropertiesByIdAsJsonObject(authUser, userId);
      return Response.ok().entity(gson.toJson(userJson)).build();
    }
    userService.deleteUserRole(activeUser, userId, roleId);
    JsonObject userJson = userService.findUserWithPropertiesByIdAsJsonObject(authUser, userId);
    return Response.ok().entity(gson.toJson(userJson)).build();
  }

  @POST
  @Consumes("application/json")
  @Produces("application/json")
  @PermitAll
  public Response createResearcher(@Context UriInfo info, @Auth AuthUser authUser) {
    if (authUser == null || authUser.getEmail() == null || authUser.getName() == null) {
      return Response.
          status(Response.Status.BAD_REQUEST).
          entity(new Error("Unable to verify google identity",
              Response.Status.BAD_REQUEST.getStatusCode())).
          build();
    }
    try {
      if (userService.findUserByEmail(authUser.getEmail()) != null) {
        return Response.
            status(Response.Status.CONFLICT).
            entity(new Error("Registered user exists", Response.Status.CONFLICT.getStatusCode())).
            build();
      }
    } catch (NotFoundException nfe) {
      // no-op, we expect to not find the new user in this case.
    }
    User user = new User();
    user.setEmail(authUser.getEmail());
    user.setDisplayName(authUser.getName());
    user.setResearcherRole();
    try {
      URI uri;
      user = userService.createUser(user);
      uri = info.getRequestUriBuilder().path("{email}").build(user.getEmail());
      return Response.created(new URI(uri.toString().replace("user", "dacuser"))).entity(user)
          .build();
    } catch (Exception e) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(new Error(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()))
          .build();
    }
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/signing-officials")
  @RolesAllowed(RESEARCHER)
  public Response getSOsForInstitution(@Auth AuthUser authUser) {
    try {
      User user = userService.findUserByEmail(authUser.getEmail());
      if (Objects.nonNull(user.getInstitutionId())) {
        List<SimplifiedUser> signingOfficials = userService.findSOsByInstitutionId(
            user.getInstitutionId());
        return Response.ok().entity(signingOfficials).build();
      }
      return Response.ok().entity(Collections.emptyList()).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/acknowledgements")
  @PermitAll
  public Response getUserAcknowledgements(@Auth AuthUser authUser) {
    try {
      User user = userService.findUserByEmail(authUser.getEmail());
      Map<String, Acknowledgement> acknowledgementMap = acknowledgementService.findAcknowledgementsForUser(
          user);
      return Response.ok().entity(acknowledgementMap).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/acknowledgements/{key}")
  @PermitAll
  public Response getUserAcknowledgement(@Auth AuthUser authUser, @PathParam("key") String key) {
    try {
      User user = userService.findUserByEmail(authUser.getEmail());
      Acknowledgement ack = acknowledgementService.findAcknowledgementForUserByKey(user, key);
      if (ack == null) {
        return Response.status(Response.Status.NOT_FOUND).build();
      }
      return Response.ok().entity(ack).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @DELETE
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/acknowledgements/{key}")
  @RolesAllowed(ADMIN)
  public Response deleteUserAcknowledgement(@Auth AuthUser authUser, @PathParam("key") String key) {
    try {
      User user = userService.findUserByEmail(authUser.getEmail());
      Acknowledgement ack = acknowledgementService.findAcknowledgementForUserByKey(user, key);
      if (Objects.isNull(ack)) {
        return Response.status(Response.Status.NOT_FOUND).build();
      }
      acknowledgementService.deleteAcknowledgementForUserByKey(user, key);
      return Response.ok().entity(ack).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/acknowledgements")
  @PermitAll
  public Response postAcknowledgements(@Auth DuosUser duosUser, String json) {
    User user = duosUser.getUser();
    ArrayList<String> keys;
    try {
      keys = gson.fromJson(json, new TypeToken<>() {});
      if (keys == null || keys.isEmpty()) {
        return Response.status(Response.Status.BAD_REQUEST).build();
      }
    } catch (Exception e) {
      return Response.status(Response.Status.BAD_REQUEST).build();
    }

    if (keys.stream().anyMatch(k -> k.startsWith(AcknowledgementService.DAR_CLOSEOUT_CHAIR_REF))
        && !user.hasUserRole(UserRoles.CHAIRPERSON)) {
      return Response.status(Status.BAD_REQUEST)
          .entity(new Error("Invalid acknowledgement", HttpStatusCodes.STATUS_CODE_BAD_REQUEST))
          .build();
    }

    try {
      Map<String, Acknowledgement> acknowledgementMap = acknowledgementService.makeAcknowledgements(
          keys, user);
      return Response.ok().entity(acknowledgementMap).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/me/researcher/datasets")
  @PermitAll
  public Response getApprovedDatasets(@Auth AuthUser authUser) {
    try {
      User user = userService.findUserByEmail(authUser.getEmail());
      List<ApprovedDataset> approvedDatasets = datasetService.getApprovedDatasets(user);
      return Response.ok().entity(approvedDatasets).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }


}
