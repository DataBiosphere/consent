package org.broadinstitute.consent.http.resources;


import com.google.api.client.http.HttpStatusCodes;
import com.google.common.annotations.VisibleForTesting;
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
import jakarta.ws.rs.core.UriInfo;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.Acknowledgement;
import org.broadinstitute.consent.http.models.ApprovedDataset;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.Error;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.models.UserUpdateFields;
import org.broadinstitute.consent.http.models.dto.DatasetDTO;
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
    try {
      Set<DatasetDTO> datasets;
      User user = userService.findUserByEmail(authUser.getEmail());
      List<Integer> dacIds = user.getRoles().stream()
          .filter(r -> Objects.nonNull(r.getDacId()))
          .map(UserRole::getDacId)
          .collect(Collectors.toList());
      datasets = dacIds.isEmpty() ? Set.of() : datasetService.findDatasetsByDacIds(dacIds);
      return Response.ok().entity(datasets).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
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
  @RolesAllowed({ADMIN, CHAIRPERSON, MEMBER})
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

      if (Objects.nonNull(userUpdateFields.getUserRoleIds()) && !user.hasUserRole(
          UserRoles.ADMIN)) {
        throw new BadRequestException("Cannot change user's roles.");
      }

      if (!canUpdateInstitution(user, userUpdateFields.getInstitutionId())) {
        throw new BadRequestException("Cannot update user's institution id.");
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

  @VisibleForTesting
  protected boolean canUpdateInstitution(User user, Integer newInstitutionId) {
    if ((!Objects.isNull(user.getUserId()) || !Objects.isNull(newInstitutionId)) && !Objects.equals(
        user.getInstitutionId(), newInstitutionId)) {
      if (user.hasUserRole(UserRoles.ADMIN)) {
        return true; // admins can do everything.
      }
      if (user.hasUserRole(UserRoles.SIGNINGOFFICIAL) || user.hasUserRole(UserRoles.ITDIRECTOR)) {
        // can only update institution if not set.
        return Objects.isNull(user.getInstitutionId()) && Objects.nonNull(newInstitutionId);
      }
      // User is not restricted based on role
      return true;
    } else {
      return true; // no op, no change, supports keeping no institution set to no institution.
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
      if (activeUser.hasUserRole(UserRoles.ADMIN) && UserRoles.isValidNonDACRoleId(roleId)) {
        if (!currentUserRoleIds.contains(roleId)) {
          userService.insertUserRoles(Collections.singletonList(role), user.getUserId());
          return getUserResponse(authUser, userId);
        } else {
          return Response.notModified().build();
        }
      } else if (signingOfficialMeetsRequirements(roleId, activeUser, user)) {
        // update the user role with the active user's institution id.
        if (!currentUserRoleIds.contains(roleId)) {
          // update the user's institution if it was set to null and add the role.
          if (Optional.ofNullable(user.getInstitutionId()).isEmpty()) {
            userService.insertRoleAndInstitutionForUser(role, activeUser.getInstitutionId(),
                user.getUserId());
          } else {
            userService.insertUserRoles(Collections.singletonList(role), user.getUserId());
          }
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

  private static boolean signingOfficialMeetsRequirements(Integer roleId, User activeUser,
      User user) {
    return activeUser.hasUserRole(UserRoles.SIGNINGOFFICIAL)
        && Objects.nonNull(activeUser.getInstitutionId())
        && UserRoles.isValidSoAdjustableRoleId(roleId)
        && (Objects.equals(user.getInstitutionId(), activeUser.getInstitutionId()) ||
        Optional.ofNullable(user.getInstitutionId()).isEmpty());
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
        if (!UserRoles.isValidNonDACRoleId(roleId)) {
          throw new BadRequestException("Invalid Role Id");
        }
        return doDelete(authUser, userId, roleId, activeUser, user);
      } else if (activeUser.hasUserRole(UserRoles.SIGNINGOFFICIAL)) {
        if (!UserRoles.isValidSoAdjustableRoleId(roleId)) {
          throw new ForbiddenException(
              "A Signing Official may only remove the following role ids: [6, 7, 8] ");
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
    }    try {
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
    UserRole researcher = new UserRole(UserRoles.RESEARCHER.getRoleId(),
        UserRoles.RESEARCHER.getRoleName());
    user.setRoles(Collections.singletonList(researcher));
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

  @DELETE
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/{email}")
  @RolesAllowed(ADMIN)
  public Response delete(@PathParam("email") String email, @Context UriInfo info) {
    userService.deleteUserByEmail(email);
    return Response.ok().build();
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
  public Response postAcknowledgements(@Auth AuthUser authUser, String json) {
    ArrayList<String> keys;
    try {
      Type listOfStringsType = new TypeToken<ArrayList<String>>() {
      }.getType();
      keys = gson.fromJson(json, listOfStringsType);
      if (keys == null || keys.isEmpty()) {
        return Response.status(Response.Status.BAD_REQUEST).build();
      }
    } catch (Exception e) {
      return Response.status(Response.Status.BAD_REQUEST).build();
    }

    try {
      User user = userService.findUserByEmail(authUser.getEmail());
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
