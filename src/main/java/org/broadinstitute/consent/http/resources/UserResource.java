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
import jakarta.ws.rs.QueryParam;
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
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.exceptions.SamAzureB2CException;
import org.broadinstitute.consent.http.models.Acknowledgement;
import org.broadinstitute.consent.http.models.ApprovedDataset;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.CreateDuosUserRequest;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.models.Error;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.models.UserUpdateFields;
import org.broadinstitute.consent.http.models.sam.UserStatusInfo;
import org.broadinstitute.consent.http.service.AcknowledgementService;
import org.broadinstitute.consent.http.service.DatasetService;
import org.broadinstitute.consent.http.service.NihService;
import org.broadinstitute.consent.http.service.UserService;
import org.broadinstitute.consent.http.service.UserService.SigningOfficialUser;
import org.broadinstitute.consent.http.service.UserService.SimplifiedUser;
import org.broadinstitute.consent.http.service.feature.InstitutionAndLibraryCardEnforcement;
import org.broadinstitute.consent.http.service.sam.SamService;
import org.broadinstitute.consent.http.util.gson.GsonUtil;

@Path("api/user")
public class UserResource extends Resource {

  private final UserService userService;
  private final Gson gson = GsonUtil.getInstance();
  private final SamService samService;
  private final DatasetService datasetService;
  private final AcknowledgementService acknowledgementService;
  private final NihService nihService;
  private final ServicesConfiguration servicesConfiguration;
  private final InstitutionAndLibraryCardEnforcement institutionAndLibraryCardEnforcement;

  @Inject
  public UserResource(
      SamService samService,
      UserService userService,
      DatasetService datasetService,
      AcknowledgementService acknowledgementService,
      NihService nihService,
      ServicesConfiguration servicesConfiguration,
      InstitutionAndLibraryCardEnforcement institutionAndLibraryCardEnforcement) {
    this.samService = samService;
    this.userService = userService;
    this.datasetService = datasetService;
    this.acknowledgementService = acknowledgementService;
    this.nihService = nihService;
    this.servicesConfiguration = servicesConfiguration;
    this.institutionAndLibraryCardEnforcement = institutionAndLibraryCardEnforcement;
  }

  @GET
  @Produces("application/json")
  @Path("/role/{roleName}")
  @RolesAllowed({ADMIN, SIGNINGOFFICIAL})
  public Response getUsers(@Auth DuosUser duosUser, @PathParam("roleName") String roleName) {
    try {
      User user = duosUser.getUser();
      boolean valid = UserRoles.isValidRole(roleName);
      if (valid) {
        // if there is a valid roleName but it is not SO or Admin then throw an exception
        if (!roleName.equals(UserRoles.ADMIN.getRoleName())
            && !roleName.equals(UserRoles.SIGNINGOFFICIAL.getRoleName())) {
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
  public Response getUser(@Auth DuosUser duosUser) {
    try {
      UserStatusInfo userStatusInfo = duosUser.getUserStatusInfo();
      if (userStatusInfo == null) {
        samService.asyncPostRegistrationInfo(duosUser);
        // Refresh the user status info after posting registration info to Sam
        userStatusInfo = getUserStatusInfo(duosUser);
      }
      User user = nihService.syncAccount(duosUser);
      if (userStatusInfo != null) {
        user.setUserStatusInfo(userStatusInfo);
      }
      return Response.ok(gson.toJson(user)).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  private UserStatusInfo getUserStatusInfo(DuosUser duosUser) throws SamAzureB2CException {
    try {
      return samService.getCombinedUserStatusInfo(duosUser);
    } catch (SamAzureB2CException e) {
      logWarn(
          "Sam azure b2c exception: %s for user: %s"
              .formatted(e.getMessage(), duosUser.getEmail()));
      throw e;
    } catch (Exception ex) {
      logWarn("Unable to retrieve user status info from Sam: " + ex.getMessage());
      // Intentionally ignore Sam errors here to avoid failing /me on transient outages.
      return null;
    }
  }

  @Deprecated // Use getDatasetsFromUserDacsV2
  @GET
  @Path("/me/dac/datasets")
  @Produces("application/json")
  @RolesAllowed({CHAIRPERSON, MEMBER})
  public Response getDatasetsFromUserDacs(@Auth DuosUser duosUser) {
    return getDatasetsFromUserDacsV2(duosUser);
  }

  @GET
  @Path("/me/dac/datasets/v2")
  @Produces("application/json")
  @RolesAllowed({CHAIRPERSON, MEMBER})
  public Response getDatasetsFromUserDacsV2(@Auth DuosUser duosUser) {
    try {
      User user = duosUser.getUser();
      List<Integer> dacIds =
          user.getRoles().stream().map(UserRole::getDacId).filter(Objects::nonNull).toList();
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
  public Response getUserById(@Auth DuosUser duosUser, @PathParam("userId") Integer userId) {
    try {
      JsonObject userJson = userService.findUserWithPropertiesByIdAsJsonObject(duosUser, userId);
      return Response.ok(gson.toJson(userJson)).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @GET
  @Path("/institution/{institutionId}")
  @Produces("application/json")
  @RolesAllowed({ADMIN})
  public Response getUsersByInstitution(
      @Auth DuosUser duosUser, @PathParam("institutionId") Integer institutionId) {
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
  public Response update(@Auth DuosUser duosUser, @PathParam("id") Integer userId, String json) {
    try {
      UserUpdateFields userUpdateFields = gson.fromJson(json, UserUpdateFields.class);
      // Ensure that we have a real user with this ID, fail if we do not.
      userService.findUserById(userId);
      User updatedUser = userService.updateUserFieldsById(userUpdateFields, userId);
      JsonObject jsonUser =
          userService.findUserWithPropertiesByIdAsJsonObject(duosUser, updatedUser.getUserId());
      return Response.ok().entity(gson.toJson(jsonUser)).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @PUT
  @Consumes("application/json")
  @Produces("application/json")
  @PermitAll
  public Response updateSelf(@Auth DuosUser duosUser, @Context UriInfo info, String json) {
    try {
      User user = duosUser.getUser();
      UserUpdateFields userUpdateFields = gson.fromJson(json, UserUpdateFields.class);

      if (Objects.nonNull(userUpdateFields.getUserRoleIds())
          && !user.hasUserRole(UserRoles.ADMIN)) {
        throw new BadRequestException("Cannot change user's roles.");
      }

      user = userService.updateUserFieldsById(userUpdateFields, user.getUserId());
      JsonObject jsonUser =
          userService.findUserWithPropertiesByIdAsJsonObject(duosUser, user.getUserId());

      return Response.ok().entity(gson.toJson(jsonUser)).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @PUT
  @Path("/{userId}/{roleId}")
  @Produces("application/json")
  @RolesAllowed({ADMIN, SIGNINGOFFICIAL})
  public Response addRoleToUser(
      @Auth DuosUser duosUser,
      @PathParam("userId") Integer userId,
      @PathParam("roleId") Integer roleId) {
    UserRoles targetRole = UserRoles.getUserRoleFromId(roleId);
    if (Objects.isNull(targetRole)) {
      return Response.status(HttpStatusCodes.STATUS_CODE_BAD_REQUEST).build();
    }
    UserRole role = new UserRole(roleId, targetRole.getRoleName());
    try {
      User activeUser = duosUser.getUser();
      User user = userService.findUserById(userId);
      List<Integer> currentUserRoleIds = user.getUserRoleIdsFromUser();
      if ((activeUser.hasUserRole(UserRoles.ADMIN) && UserRoles.isValidNonDACRoleId(targetRole))
          || signingOfficialMeetsRequirements(targetRole, activeUser, user)) {
        if (!currentUserRoleIds.contains(roleId)) {
          userService.insertRoleAndInstitutionForUser(role, user);
          return getUserResponse(duosUser, userId);
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

  private static boolean signingOfficialMeetsRequirements(
      UserRoles role, User activeUser, User user) {
    return activeUser.hasUserRole(UserRoles.SIGNINGOFFICIAL)
        && activeUser.getInstitutionId() != null
        && UserRoles.isValidSoAdjustableRoleId(role)
        && (user.getInstitutionId() == null
            || user.getInstitutionId().equals(activeUser.getInstitutionId()));
  }

  private Response getUserResponse(DuosUser duosUser, Integer userId) {
    JsonObject userJson = userService.findUserWithPropertiesByIdAsJsonObject(duosUser, userId);
    return Response.ok().entity(gson.toJson(userJson)).build();
  }

  @DELETE
  @Path("/{userId}/{roleId}")
  @Produces("application/json")
  @RolesAllowed({ADMIN, SIGNINGOFFICIAL})
  public Response deleteRoleFromUser(
      @Auth DuosUser duosUser,
      @PathParam("userId") Integer userId,
      @PathParam("roleId") Integer roleId) {
    UserRoles targetRole = UserRoles.getUserRoleFromId(roleId);
    if (Objects.isNull(targetRole)) {
      return Response.status(HttpStatusCodes.STATUS_CODE_BAD_REQUEST).build();
    }
    try {
      User activeUser = duosUser.getUser();
      User user = userService.findUserById(userId);
      if (activeUser.hasUserRole(UserRoles.ADMIN)) {
        if (!UserRoles.isValidNonDACRoleId(targetRole)) {
          throw new BadRequestException("Invalid Role Id");
        }
        return doDelete(duosUser, userId, roleId, activeUser, user);
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
          return doDelete(duosUser, userId, roleId, activeUser, user);
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

  private Response doDelete(
      DuosUser duosUser, Integer userId, Integer roleId, User activeUser, User user) {
    List<Integer> currentUserRoleIds = user.getUserRoleIdsFromUser();
    if (!currentUserRoleIds.contains(roleId)) {
      JsonObject userJson = userService.findUserWithPropertiesByIdAsJsonObject(duosUser, userId);
      return Response.ok().entity(gson.toJson(userJson)).build();
    }
    userService.deleteUserRole(activeUser, userId, roleId);
    JsonObject userJson = userService.findUserWithPropertiesByIdAsJsonObject(duosUser, userId);
    return Response.ok().entity(gson.toJson(userJson)).build();
  }

  @POST
  @Consumes("application/json")
  @Produces("application/json")
  @PermitAll
  public Response createResearcher(@Context UriInfo info, @Auth AuthUser authUser) {
    if (authUser == null || authUser.getEmail() == null || authUser.getName() == null) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(
              new Error(
                  "Unable to verify google identity", Response.Status.BAD_REQUEST.getStatusCode()))
          .build();
    }
    try {
      if (userService.findUserByEmail(authUser.getEmail()) != null) {
        return Response.status(Response.Status.CONFLICT)
            .entity(new Error("Registered user exists", Response.Status.CONFLICT.getStatusCode()))
            .build();
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
      return Response.created(new URI(uri.toString().replace("user", "dacuser")))
          .entity(user)
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
  public Response getSOsForInstitution(@Auth DuosUser duosUser) {
    try {
      User user = duosUser.getUser();
      if (Objects.nonNull(user.getInstitutionId())) {
        List<SimplifiedUser> signingOfficials =
            userService.findSOsByInstitutionId(user.getInstitutionId());
        return Response.ok().entity(signingOfficials).build();
      }
      return Response.ok().entity(Collections.emptyList()).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/institution/{institutionId}/signing-officials")
  @RolesAllowed({ADMIN, CHAIRPERSON, MEMBER, RESEARCHER})
  public Response getSigningOfficialsByInstitution(
      @Auth DuosUser duosUser, @PathParam("institutionId") Integer institutionId) {
    try {
      User user = duosUser.getUser();
      if (!user.hasUserRole(UserRoles.ADMIN)
          && !user.hasUserRole(UserRoles.CHAIRPERSON)
          && !user.hasUserRole(UserRoles.MEMBER)
          && !Objects.equals(user.getInstitutionId(), institutionId)) {
        throw new ForbiddenException("Researchers may only query their own institution.");
      }
      List<SigningOfficialUser> signingOfficials =
          userService.findSOsWithDataByInstitutionId(institutionId);
      return Response.ok().entity(signingOfficials).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/acknowledgements")
  @PermitAll
  public Response getUserAcknowledgements(@Auth DuosUser duosUser) {
    try {
      User user = duosUser.getUser();
      Map<String, Acknowledgement> acknowledgementMap =
          acknowledgementService.findAcknowledgementsForUser(user);
      return Response.ok().entity(acknowledgementMap).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/acknowledgements/{key}")
  @PermitAll
  public Response getUserAcknowledgement(@Auth DuosUser duosUser, @PathParam("key") String key) {
    try {
      User user = duosUser.getUser();
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
  public Response deleteUserAcknowledgement(@Auth DuosUser duosUser, @PathParam("key") String key) {
    try {
      User user = duosUser.getUser();
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
      return Response.status(Status.UNAUTHORIZED)
          .entity(new Error("Invalid acknowledgement", HttpStatusCodes.STATUS_CODE_UNAUTHORIZED))
          .build();
    }

    try {
      Map<String, Acknowledgement> acknowledgementMap =
          acknowledgementService.makeAcknowledgements(keys, user);
      return Response.ok().entity(acknowledgementMap).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/me/researcher/datasets")
  @PermitAll
  public Response getApprovedDatasets(@Auth DuosUser duosUser) {
    try {
      User user = duosUser.getUser();
      List<ApprovedDataset> approvedDatasets = datasetService.getApprovedDatasets(user);
      return Response.ok().entity(approvedDatasets).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/create")
  @RolesAllowed({ADMIN, CHAIRPERSON, SIGNINGOFFICIAL})
  public Response createNewUser(@Auth DuosUser duosUser, String json) {
    try {
      CreateDuosUserRequest createDuosUserRequest =
          gson.fromJson(json, CreateDuosUserRequest.class);
      createDuosUserRequest.validate();

      // Non-admins have additional restrictions when creating new users
      if (!duosUser.getUser().hasUserRole(UserRoles.ADMIN)) {
        String existingUserEmail = duosUser.getUser().getEmail();
        String newUserEmail = createDuosUserRequest.newUser().getEmail();

        // Enforce that the new user email domain matches the creator's institution domains
        institutionAndLibraryCardEnforcement.validateEmailsFromSameInstitution(
            newUserEmail, existingUserEmail);

        // Non-admins can only create users with the Researcher role
        createDuosUserRequest
            .roles()
            .forEach(
                role -> {
                  if (!role.getName().equals(UserRoles.RESEARCHER.getRoleName())) {
                    throw new ForbiddenException(
                        "You can only create users with the Researcher role.");
                  }
                });
      }
      User user = userService.createUser(createDuosUserRequest.newUser());
      String localUrl =
          servicesConfiguration.getLocalURL().endsWith("/")
              ? servicesConfiguration.getLocalURL()
              : servicesConfiguration.getLocalURL() + "/";
      URI uri = new URI("%sapi/user/%d".formatted(localUrl, user.getUserId()));
      return Response.created(uri).entity(user).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  /**
   * Redact PII from a user account. The user's email and display name are replaced with anonymized
   * placeholders and an audit record is created to preserve the original values. Only admins may
   * call this endpoint.
   *
   * @param duosUser the authenticated admin user
   * @param email the email address of the user to redact (as a query parameter)
   * @return 200 OK on success
   */
  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/redact")
  @RolesAllowed({ADMIN})
  public Response redactUser(@Auth DuosUser duosUser, @QueryParam("email") String email) {
    try {
      if (email == null || email.isBlank()) {
        throw new BadRequestException("email query parameter is required");
      }
      userService.redactUser(duosUser.getUser(), email);
      return Response.ok().build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }
}
