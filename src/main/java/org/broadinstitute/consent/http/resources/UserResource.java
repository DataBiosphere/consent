package org.broadinstitute.consent.http.resources;


import io.dropwizard.auth.Auth;
import io.dropwizard.jersey.PATCH;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.dto.Error;
import org.broadinstitute.consent.http.models.dto.PatchOperation;
import org.broadinstitute.consent.http.service.users.UserAPI;

import javax.annotation.security.PermitAll;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;

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
    public Response createUser(@Context UriInfo info, DACUser userToCreate, @Auth User user) {
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

    @PUT
    @Consumes("application/json")
    @Produces("application/json")
    @PermitAll
    public Response update(DACUser userToUpdate, @Auth User user) {
        try {
            return Response.ok().entity(userAPI.updateUser(userToUpdate, user.getName())).build();
        } catch (Exception e){
            return createExceptionResponse(e);
        }
    }

    @PATCH
    @Consumes("application/json")
    @Produces("application/json")
    @PermitAll
    public Response partialUpdate(List<PatchOperation> patchOperations, @Auth User user) {
        try {
            return Response.ok().entity(userAPI.updatePartialUser(patchOperations, user.getName())).build();
        } catch (Exception e){
            return createExceptionResponse(e);
        }
    }

}
