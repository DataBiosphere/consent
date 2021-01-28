package org.broadinstitute.consent.http.resources;
import com.google.api.client.http.HttpStatusCodes;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import org.broadinstitute.consent.http.authentication.GoogleUser;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.*;
import org.broadinstitute.consent.http.models.dto.Error;
import org.broadinstitute.consent.http.service.UserService;
import org.broadinstitute.consent.http.service.WhitelistService;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Path("{api : (api/)?}user")
public class UserResource extends Resource {

    private final UserService userService;
    private final WhitelistService whitelistService;
    private final Gson gson;

    @Inject
    public UserResource(UserService userService, WhitelistService whitelistService) {
        this.userService = userService;
        this.whitelistService = whitelistService;
        this.gson = new Gson();
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
     * @return JsonObject version of the user with researcher properties and whitelist entries
     */
    private JsonObject constructUserJsonObject(User user) {
        List<ResearcherProperty> props = userService.findAllUserProperties(user.getDacUserId());
        List<WhitelistEntry> entries = whitelistService.findWhitelistEntriesForUser(user, props);
        JsonObject userJson = gson.toJsonTree(user).getAsJsonObject();
        JsonArray propsJson = gson.toJsonTree(props).getAsJsonArray();
        JsonArray entriesJson = gson.toJsonTree(entries).getAsJsonArray();
        userJson.add("researcherProperties", propsJson);
        userJson.add("whitelistEntries", entriesJson);
        return userJson;
    }

    @POST
    @Produces("application/json")
    @Path("/user/{user}")
    @RolesAllowed({ADMIN, CHAIRPERSON})
    public Response addViewedAlgoDecision(@PathParam("user") User user) {
        userService.addViewedAlgoDecision(user);
        return Response.ok().build();
    }

}
