package org.broadinstitute.consent.http.resources;


import io.dropwizard.auth.Auth;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.DACUser;
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
    public Response createUser(@Context UriInfo info, String json, @Auth AuthUser user) {
        DACUser userToCreate = new DACUser(json);
        try {
            URI uri;
            userToCreate = userAPI.createUser(userToCreate, user.getName());
            uri = info.getRequestUriBuilder().path("{email}").build(userToCreate.getEmail());
            return Response.created(new URI(uri.toString().replace("user","dacuser"))).entity(userToCreate).build();
        } catch (IllegalArgumentException e) {
            if(e.getMessage().contains("Email should be unique.")) {
                return Response.status(Response.Status.CONFLICT).entity(new Error(e.getMessage(), Response.Status.CONFLICT.getStatusCode())).build();
            }
            return Response.status(Response.Status.BAD_REQUEST).entity(new Error(e.getMessage(), Response.Status.BAD_REQUEST.getStatusCode())).build();
        } catch (Exception e){
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new Error(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())).build();
        }
    }

}
