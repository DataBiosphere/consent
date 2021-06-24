package org.broadinstitute.consent.http.resources;


import com.google.api.client.http.HttpStatusCodes;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import java.net.URI;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
import org.broadinstitute.consent.http.authentication.GoogleUser;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.LibraryCard;
import org.broadinstitute.consent.http.models.UserProperty;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.models.dto.Error;
import org.broadinstitute.consent.http.service.LibraryCardService;
import org.broadinstitute.consent.http.service.UserService;

@Path("{api : (api/)?}user")
public class UserResource extends Resource {

    private final UserService userService;
    private final LibraryCardService libraryCardService;
    private final Gson gson;

    @Inject
    public UserResource(UserService userService, LibraryCardService libraryCardService) {
        this.userService = userService;
        this.libraryCardService = libraryCardService;
        this.gson = new Gson();
    }

    @GET
    @Produces("application/json")
    @RolesAllowed({ADMIN, SIGNINGOFFICIAL})
    public Response getUsers(@Auth AuthUser authUser, @QueryParam("roleName") String roleName) {
        try {
            User user = userService.findUserByEmail(authUser.getName());
            if (Objects.nonNull(roleName)) {
                boolean valid = EnumSet.allOf(UserRoles.class)
                  .stream()
                  .map(UserRoles::getRoleName)
                  .map(String::toLowerCase)
                  .anyMatch(roleName::equalsIgnoreCase);
                if (valid) {
                    //if the roleName is SO and the user does not have that role throw an exception
                    if (roleName.equals(UserRoles.SIGNINGOFFICIAL.getRoleName())) {
                        if (!user.hasUserRole(UserRoles.SIGNINGOFFICIAL)) {
                            throw new NotFoundException("User: " + user.getDisplayName() + ", " + " does not have Signing Official role.");
                        }
                    }
                    //if the roleName is Admin and the user does not have that role throw an exception
                    if (roleName.equals(UserRoles.ADMIN.getRoleName())) {
                        if (!user.hasUserRole(UserRoles.ADMIN)) {
                            throw new NotFoundException("User: " + user.getDisplayName() + ", " + " does not have Admin role.");
                        }
                    }
                    //if there is a valid roleName but it is not SO or Admin then throw an exception
                    if (!roleName.equals(UserRoles.ADMIN.getRoleName()) && !roleName.equals(UserRoles.SIGNINGOFFICIAL.getRoleName())) {
                        throw new BadRequestException("Unsupported role name: " + roleName);
                    }

                    List<User> users = userService.getUsersByUserRole(user, roleName);
                    return Response.ok().entity(users).build();
                }
                else {
                    throw new BadRequestException("Invalid role name: " + roleName);
                }
            } else {
                throw new BadRequestException("No user role specified.");
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
            User user = userService.findUserByEmail(authUser.getName());
            JsonObject userJson = constructUserJsonObject(user);
            return Response.ok(gson.toJson(userJson)).build();
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
            User user = userService.findUserById(userId);
            JsonObject userJson = constructUserJsonObject(user);
            return Response.ok(gson.toJson(userJson)).build();
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
            List<UserRoles> allowableRoles = Stream
                .of(UserRoles.ADMIN, UserRoles.ALUMNI, UserRoles.RESEARCHER, UserRoles.DATAOWNER)
                .collect(Collectors.toList());
            Optional<UserRoles> matchingRole = allowableRoles
                .stream()
                .filter(r -> r.getRoleId().equals(roleId))
                .findFirst();
            List<Integer> currentUserRoleIds = user
                .getRoles()
                .stream()
                .map(UserRole::getRoleId)
                .collect(Collectors.toList());
            if (matchingRole.isPresent()) {
                if (!currentUserRoleIds.contains(roleId)) {
                    UserRole role = new UserRole(roleId, matchingRole.get().getRoleName());
                    userService.insertUserRoles(Collections.singletonList(role), user.getDacUserId());
                    user = userService.findUserById(userId);
                    JsonObject userJson = constructUserJsonObject(user);
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

    /**
     * Convenience method for a generic user object with custom properties added
     * @param user The User
     * @return JsonObject version of the user with researcher properties and library card entries
     */
    private JsonObject constructUserJsonObject(User user) {
        List<UserProperty> props = userService.findAllUserProperties(user.getDacUserId());
        List<LibraryCard> entries = libraryCardService.findLibraryCardsByUserId(user.getDacUserId());
        JsonObject userJson = gson.toJsonTree(user).getAsJsonObject();
        JsonArray propsJson = gson.toJsonTree(props).getAsJsonArray();
        JsonArray entriesJson = gson.toJsonTree(entries).getAsJsonArray();
        userJson.add("researcherProperties", propsJson);
        userJson.add("libraryCards", entriesJson);
        return userJson;
    }

}
