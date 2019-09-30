package org.broadinstitute.consent.http.resources;


import io.dropwizard.auth.Auth;
import org.broadinstitute.consent.http.authentication.GoogleUser;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.models.dto.Error;
import org.broadinstitute.consent.http.service.users.UserAPI;

import javax.annotation.security.PermitAll;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Collections;

@Path("{api : (api/)?}user")
public class UserResource extends Resource {

    private final UserAPI userAPI;

    public UserResource(UserAPI userAPI) {
        this.userAPI = userAPI;
    }

    @POST
    @Consumes("application/json")
    @Produces("application/json")
    @PermitAll
    public Response createResearcher(@Context UriInfo info, String json, @Auth AuthUser user) {
        GoogleUser googleUser = user.getGoogleUser();
        if (googleUser == null || googleUser.getEmail() == null || googleUser.getName() == null) {
            return Response.
                    status(Response.Status.BAD_REQUEST).
                    entity(new Error("Unable to verify google identity", Response.Status.BAD_REQUEST.getStatusCode())).
                    build();
        }
        if (userAPI.findUserByEmail(googleUser.getEmail()) != null) {
            return Response.
                    status(Response.Status.CONFLICT).
                    entity(new Error("Registered user exists", Response.Status.CONFLICT.getStatusCode())).
                    build();
        }
        DACUser dacUser = new DACUser(googleUser);
        UserRole researcher = new UserRole(UserRoles.RESEARCHER.getRoleId(), UserRoles.RESEARCHER.getRoleName());
        dacUser.setRoles(Collections.singletonList(researcher));
        try {
            URI uri;
            dacUser = userAPI.createUser(dacUser);
            uri = info.getRequestUriBuilder().path("{email}").build(dacUser.getEmail());
            return Response.created(new URI(uri.toString().replace("user", "dacuser"))).entity(dacUser).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new Error(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())).build();
        }
    }

}
