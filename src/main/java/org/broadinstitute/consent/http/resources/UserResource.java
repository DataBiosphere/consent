package org.broadinstitute.consent.http.resources;


import com.google.api.client.http.HttpStatusCodes;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import org.broadinstitute.consent.http.authentication.GoogleUser;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.models.UserUpdateFields;
import org.broadinstitute.consent.http.models.dto.DatasetDTO;
import org.broadinstitute.consent.http.models.dto.Error;
import org.broadinstitute.consent.http.service.DatasetService;
import org.broadinstitute.consent.http.service.ResearcherService;
import org.broadinstitute.consent.http.service.SupportRequestService;
import org.broadinstitute.consent.http.service.UserService;
import org.broadinstitute.consent.http.service.UserService.SimplifiedUser;
import org.broadinstitute.consent.http.service.sam.SamService;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Path("api/user")
public class UserResource extends Resource {

    private final UserService userService;
    private final ResearcherService researcherService;
    private final Gson gson = new Gson();
    private final SamService samService;
    private final DatasetService datasetService;
    private final SupportRequestService supportRequestService;

    @Inject
    public UserResource(ResearcherService researcherService, SamService samService, UserService userService,
                        DatasetService datasetService, SupportRequestService supportRequestService) {
        this.researcherService = researcherService;
        this.samService = samService;
        this.userService = userService;
        this.datasetService = datasetService;
        this.supportRequestService = supportRequestService;
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
                if (!roleName.equals(UserRoles.ADMIN.getRoleName()) && !roleName.equals(UserRoles.SIGNINGOFFICIAL.getRoleName())) {
                    throw new BadRequestException("Unsupported role name: " + roleName);
                }
                if (!user.hasUserRole(UserRoles.getUserRoleFromName(roleName))) {
                    throw new NotFoundException("User: " + user.getDisplayName() + ", does not have " + roleName + " role.");
                }
                List<User> users = userService.getUsersAsRole(user, roleName);
                return Response.ok().entity(users).build();
            }
            else {
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
            JsonObject userJson = userService.findUserWithPropertiesByIdAsJsonObject(authUser, user.getUserId());
            return Response.ok(gson.toJson(userJson)).build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

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
        } catch(Exception e) {
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
    public Response update(@Auth AuthUser authUser, @Context UriInfo info, @PathParam("id") Integer userId, String json) {
        try {
            UserUpdateFields userUpdateFields = gson.fromJson(json, UserUpdateFields.class);
            // Ensure that we have a real user with this ID, fail if we do not.
            userService.findUserById(userId);
            URI uri = info.getRequestUriBuilder().path("{id}").build(userId);
            User user = userService.updateUserFieldsById(userUpdateFields, userId);
            supportRequestService.sendSuggestedPropertiesToSupport(userUpdateFields, user, authUser);
            Gson gson = new Gson();
            JsonObject jsonUser = userService.findUserWithPropertiesByIdAsJsonObject(authUser, user.getUserId());
            return Response.ok(uri).entity(gson.toJson(jsonUser)).build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

    @PUT
    @Path("/{userId}/{roleId}")
    @Produces("application/json")
    @RolesAllowed({ADMIN})
    public Response addRoleToUser(@Auth AuthUser authUser, @PathParam("userId") Integer userId, @PathParam("roleId") Integer roleId) {
        try {
            User user = userService.findUserById(userId);
            List<Integer> currentUserRoleIds = user.getUserRoleIdsFromUser();
            if (UserRoles.isValidNonDACRoleId(roleId)) {
                if (!currentUserRoleIds.contains(roleId)) {
                    UserRole role = new UserRole(roleId, UserRoles.getUserRoleFromId(roleId).getRoleName());
                    userService.insertUserRoles(Collections.singletonList(role), user.getUserId());
                    JsonObject userJson = userService.findUserWithPropertiesByIdAsJsonObject(authUser, userId);
                    return Response.ok().entity(gson.toJson(userJson)).build();
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

    @DELETE
    @Path("/{userId}/{roleId}")
    @Produces("application/json")
    @RolesAllowed({ADMIN})
    public Response deleteRoleFromUser(@Auth AuthUser authUser, @PathParam("userId") Integer userId, @PathParam("roleId") Integer roleId) {
        try {
            User user = userService.findUserById(userId);
            if (!UserRoles.isValidNonDACRoleId(roleId)) {
                throw new BadRequestException("Invalid Role Id");
            }
            List<Integer> currentUserRoleIds = user.getUserRoleIdsFromUser();
            if (!currentUserRoleIds.contains(roleId)) {
                JsonObject userJson = userService.findUserWithPropertiesByIdAsJsonObject(authUser, userId);
                return Response.ok().entity(gson.toJson(userJson)).build();
            }
            User auth = userService.findUserByEmail(authUser.getEmail());
            userService.deleteUserRole(auth, userId, roleId);
            JsonObject userJson = userService.findUserWithPropertiesByIdAsJsonObject(authUser, userId);
            return Response.ok().entity(gson.toJson(userJson)).build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

    @POST
    @Consumes("application/json")
    @Produces("application/json")
    @PermitAll
    public Response createResearcher(@Context UriInfo info, @Auth AuthUser user) {
        GoogleUser googleUser = user.getGoogleUser();
        if (googleUser == null || googleUser.getEmail() == null || googleUser.getName() == null) {
            return Response.
                    status(Response.Status.BAD_REQUEST).
                    entity(new Error("Unable to verify google identity", Response.Status.BAD_REQUEST.getStatusCode())).
                    build();
        }
        try {
            if (userService.findUserByEmail(googleUser.getEmail()) != null) {
                return Response.
                        status(Response.Status.CONFLICT).
                        entity(new Error("Registered user exists", Response.Status.CONFLICT.getStatusCode())).
                        build();
            }
        } catch (NotFoundException nfe) {
            // no-op, we expect to not find the new user in this case.
        }
        User dacUser = new User(googleUser);
        UserRole researcher = new UserRole(UserRoles.RESEARCHER.getRoleId(), UserRoles.RESEARCHER.getRoleName());
        dacUser.setRoles(Collections.singletonList(researcher));
        try {
            URI uri;
            dacUser = userService.createUser(dacUser);
            uri = info.getRequestUriBuilder().path("{email}").build(dacUser.getEmail());
            return Response.created(new URI(uri.toString().replace("user", "dacuser"))).entity(dacUser).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new Error(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())).build();
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
                List<SimplifiedUser> signingOfficials = userService.findSOsByInstitutionId(user.getInstitutionId());
                return Response.ok().entity(signingOfficials).build();
            }
            return Response.ok().entity(Collections.emptyList()).build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

    @POST
    @Consumes("application/json")
    @Path("/profile")
    @PermitAll
    public Response registerProperties(@Auth AuthUser authUser, @Context UriInfo info, Map<String, String> userPropertiesMap) {
        try {
            User user = userService.findUserByEmail(authUser.getEmail());
            researcherService.setProperties(userPropertiesMap, authUser);
            JsonObject userJson = userService.findUserWithPropertiesByIdAsJsonObject(authUser, user.getUserId());
            return Response.created(info.getRequestUriBuilder().build()).entity(gson.toJson(userJson)).build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

    @PUT
    @Consumes("application/json")
    @Path("/profile")
    @PermitAll
    public Response updateProperties(@Auth AuthUser authUser, @QueryParam("validate") Boolean validate, Map<String, String> userProperties) {
        try {
            User user = userService.findUserByEmail(authUser.getEmail());
            researcherService.updateProperties(userProperties, authUser, validate);
            JsonObject userJson = userService.findUserWithPropertiesByIdAsJsonObject(authUser, user.getUserId());
            return Response.ok().entity(gson.toJson(userJson)).build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }
}
