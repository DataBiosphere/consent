package org.broadinstitute.consent.http.resources;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import java.util.stream.Stream;
import org.broadinstitute.consent.http.authentication.GoogleUser;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.dto.Error;
import org.broadinstitute.consent.http.service.UserService;
import org.broadinstitute.consent.http.service.users.handler.UserRolesHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Path("api/dacuser")
public class DACUserResource extends Resource {

    private final UserService userService;
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Inject
    public DACUserResource(UserService userService) {
        this.userService = userService;
    }

    @POST
    @Consumes("application/json")
    @RolesAllowed({ADMIN, SIGNINGOFFICIAL})
    public Response createDACUser(@Context UriInfo info, String json) {
        try {
            User user = userService.createUser(new User(json));
            // Update email preference
            getEmailPreferenceValueFromUserJson(json).ifPresent(aBoolean ->
                    userService.updateEmailPreference(aBoolean, user.getDacUserId())
            );
            URI uri = info.getRequestUriBuilder().path("{email}").build(user.getEmail());
            return Response.created(uri).entity(user).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(new Error(e.getMessage(), Response.Status.BAD_REQUEST.getStatusCode())).build();
        }
    }

    @GET
    @Produces("application/json")
    @RolesAllowed(ADMIN)
    public Collection<User> describeAllUsers() {
        return userService.describeUsers();
    }

    @GET
    @Path("/{email}")
    @Produces("application/json")
    @PermitAll
    public User describe(@Auth AuthUser authUser, @PathParam("email") String email) {
        User searchUser = userService.findUserByEmail(email);
        validateAuthedRoleUser(Stream
                .of(UserRoles.ADMIN, UserRoles.CHAIRPERSON, UserRoles.MEMBER)
                .collect(Collectors.toList()),
            findByAuthUser(authUser),
            searchUser.getDacUserId());
        return searchUser;
    }

    @PUT
    @Path("/{id}")
    @Consumes("application/json")
    @Produces("application/json")
    @PermitAll
    public Response update(@Auth AuthUser authUser, @Context UriInfo info, String json, @PathParam("id") Integer userId) {
        Map<String, User> userMap = constructUserMapFromJson(json);
        try {
            validateAuthedRoleUser(Collections.singletonList(UserRoles.ADMIN), findByAuthUser(authUser), userId);
            URI uri = info.getRequestUriBuilder().path("{id}").build(userId);
            User user = userService.updateDACUserById(userMap, userId);
            // Update email preference
            JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
            JsonElement updateUser = jsonObject.get(UserRolesHandler.UPDATED_USER_KEY);
            getEmailPreferenceValueFromUserJson(updateUser.toString()).ifPresent(aBoolean ->
                    userService.updateEmailPreference(aBoolean, user.getDacUserId())
            );
            return Response.ok(uri).entity(user).build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

    /**
     * Convenience method to find a member from legacy json structure.
     *
     * @param json       Raw json string from client
     * @param memberName The name of the member to find in the json
     * @return Optional value of memberName
     */
    private Optional<String> getMemberNameStringFromJson(String json, String memberName) {
        Optional<String> aString = Optional.empty();
        JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
        if (jsonObject.has(memberName) && !jsonObject.get(memberName).isJsonNull()) {
            try {
                aString = Optional.of(jsonObject.get(memberName).getAsString());
            } catch (Exception e) {
                logger.debug(e.getMessage());
            }
        }
        return aString;
    }

    /**
     * Convenience method to find the email preference from legacy json structure.
     *
     * @param json Raw json string from client
     * @return Optional value of the "emailPreference" boolean value set in either the legacy json
     *         or the new DacUser model.
     */
    private Optional<Boolean> getEmailPreferenceValueFromUserJson(String json) {
        String memberName = "emailPreference";
        Optional<Boolean> aBoolean = Optional.empty();
        try {
            JsonElement updateUser = JsonParser.parseString(json).getAsJsonObject();
            if (updateUser != null && !updateUser.isJsonNull()) {
                JsonObject userObj = updateUser.getAsJsonObject();
                if (userObj.has(memberName) && !userObj.get(memberName).isJsonNull()) {
                    aBoolean = Optional.of(userObj.get(memberName).getAsBoolean());
                } else if (userObj.has("roles") && !userObj.get("roles").isJsonNull()) {
                    List<JsonElement> rolesElements = new ArrayList<>();
                    userObj.get("roles").getAsJsonArray().forEach(rolesElements::add);
                    List<Boolean> emailPrefs = rolesElements.
                            stream().
                            filter(e -> e.getAsJsonObject().has(memberName)).
                            map(e -> e.getAsJsonObject().get(memberName).getAsBoolean()).
                            distinct().
                            collect(Collectors.toList());
                    // In practice, there should only be a single email preference value, if any.
                    if (emailPrefs.size() == 1) {
                        aBoolean = Optional.of(emailPrefs.get(0));
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Unable to extract email preference from: " + json + " : " + e.getMessage());
        }
        return aBoolean;
    }

    /**
     * Convenience method to handle legacy json user map structure
     *
     * @param json Raw json string from client
     * @return Map of operation to DACUser
     */
    private Map<String, User> constructUserMapFromJson(String json) {
        JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
        Map<String, User> userMap = new HashMap<>();
        JsonElement updatedUser = jsonObject.get(UserRolesHandler.UPDATED_USER_KEY);
        if (updatedUser != null && !updatedUser.isJsonNull()) {
            userMap.put(UserRolesHandler.UPDATED_USER_KEY, new User(updatedUser.toString()));
        }
        return userMap;
    }

    private User findByAuthUser(AuthUser user) {
        GoogleUser googleUser = user.getGoogleUser();
        User dacUser = userService.findUserByEmail(googleUser.getEmail());
        if (dacUser == null) {
            throw new NotFoundException("Unable to find user :" + user.getName());
        }
        return dacUser;
    }

}
