package org.broadinstitute.consent.http.resources;


import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import io.dropwizard.jersey.PATCH;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.dto.Error;
import org.broadinstitute.consent.http.models.dto.PatchOperation;
import org.broadinstitute.consent.http.service.users.UserService;

import javax.annotation.security.PermitAll;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;

@Path("{api : (api/)?}user")
public class UserResource extends Resource {

    private UserService userService;

    @Inject
    public UserResource(UserService userService) {
        this.userService = userService;
    }

    @POST
    @Consumes("application/json")
    @Produces("application/json")
    @PermitAll
    public Response createUser(@Context UriInfo info, User userToCreate, @Auth AuthUser authUser) {
        try {
            URI uri;
            userToCreate = userService.createUser(userToCreate, authUser.getName());
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
    public Response update(User userToUpdate, @Auth AuthUser authUser) {
        try {
            return Response.ok().entity(userService.updateUser(userToUpdate, authUser.getName())).build();
        } catch (Exception e){
            return createExceptionResponse(e);
        }
    }

    @PATCH
    @Consumes("application/json")
    @Produces("application/json")
    @PermitAll
    public Response partialUpdate(List<PatchOperation> patchOperations, @Auth AuthUser authUser) {
        try {
            return Response.ok().entity(userService.updatePartialUser(patchOperations, authUser.getName())).build();
        } catch (Exception e){
            return createExceptionResponse(e);
        }
    }

}
