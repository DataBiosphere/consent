package org.broadinstitute.consent.http.resources;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import org.broadinstitute.consent.http.authentication.GoogleUser;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.dto.Error;
import org.broadinstitute.consent.http.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
                    userService.updateEmailPreference(aBoolean, user.getUserId())
            );
            URI uri = info.getRequestUriBuilder().path("{email}").build(user.getEmail());
            return Response.created(uri).entity(user).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(new Error(e.getMessage(), Response.Status.BAD_REQUEST.getStatusCode())).build();
        }
    }

    @Deprecated
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
            searchUser.getUserId());
        return searchUser;
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

    private User findByAuthUser(AuthUser user) {
        GoogleUser googleUser = user.getGoogleUser();
        User dacUser = userService.findUserByEmail(googleUser.getEmail());
        if (dacUser == null) {
            throw new NotFoundException("Unable to find user :" + user.getEmail());
        }
        return dacUser;
    }

}
