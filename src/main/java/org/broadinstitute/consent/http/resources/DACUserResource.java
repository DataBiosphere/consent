package org.broadinstitute.consent.http.resources;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.broadinstitute.consent.http.models.Error;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.UserService;

@Path("api/dacuser")
public class DACUserResource extends Resource {

  private final UserService userService;

  @Inject
  public DACUserResource(UserService userService) {
    this.userService = userService;
  }

  @POST
  @Consumes("application/json")
  @RolesAllowed({ADMIN})
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
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(new Error(e.getMessage(), Response.Status.BAD_REQUEST.getStatusCode())).build();
    }
  }

  /**
   * Convenience method to find the email preference from legacy json structure.
   *
   * @param json Raw json string from client
   * @return Optional value of the "emailPreference" boolean value set in either the legacy json or
   * the new DacUser model.
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
              toList();
          // In practice, there should only be a single email preference value, if any.
          if (emailPrefs.size() == 1) {
            aBoolean = Optional.of(emailPrefs.get(0));
          }
        }
      }
    } catch (Exception e) {
      logWarn("Unable to extract email preference from: " + json + " : " + e.getMessage());
    }
    return aBoolean;
  }

}
