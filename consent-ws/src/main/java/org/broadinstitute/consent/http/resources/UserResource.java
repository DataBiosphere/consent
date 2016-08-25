package org.broadinstitute.consent.http.resources;


import io.dropwizard.auth.Auth;
import io.dropwizard.jersey.PATCH;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.dto.Error;
import org.broadinstitute.consent.http.models.dto.PatchOperation;
import org.broadinstitute.consent.http.service.users.UserAPI;
import org.broadinstitute.consent.http.service.users.handler.UserRoleHandlerException;

import javax.annotation.security.PermitAll;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;

@Path("{api : (api/)?}user")
public class UserResource {

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
            validateUser(user);
            return Response.ok().entity(userAPI.updateUser(userToUpdate, user.getName())).build();
        } catch (UserRoleHandlerException e){
            return Response.status(Response.Status.CONFLICT).entity(new Error(e.getMessage(), Response.Status.CONFLICT.getStatusCode())).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(new Error(e.getMessage(), Response.Status.BAD_REQUEST.getStatusCode())).build();
        } catch (NotAuthorizedException e) {
            return Response.status(Response.Status.UNAUTHORIZED).entity(new Error(e.getMessage(), Response.Status.UNAUTHORIZED.getStatusCode())).build();
        }catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).entity(new Error(e.getMessage(), Response.Status.NOT_FOUND.getStatusCode())).build();
        } catch (Exception e) {
            return Response.serverError().entity(new Error(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())).build();
        }
    }

    @PATCH
    @Consumes("application/json")
    @Produces("application/json")
    @PermitAll
    public Response partialUpdate(List<PatchOperation> patchOperations, @Auth User user) {
        try {
            validateUser(user);
            return Response.ok().entity(userAPI.updatePartialUser(patchOperations, user.getName())).build();
        } catch (UserRoleHandlerException e){
            return Response.status(Response.Status.CONFLICT).entity(new Error(e.getMessage(), Response.Status.CONFLICT.getStatusCode())).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(new Error(e.getMessage(), Response.Status.BAD_REQUEST.getStatusCode())).build();
        } catch (NotAuthorizedException e) {
            return Response.status(Response.Status.UNAUTHORIZED).entity(new Error(e.getMessage(), Response.Status.UNAUTHORIZED.getStatusCode())).build();
        }catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).entity(new Error(e.getMessage(), Response.Status.NOT_FOUND.getStatusCode())).build();
        } catch (Exception e) {
            return Response.serverError().entity(new Error(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())).build();
        }
    }

    private void validateUser(User user){
        if(user.getName().equalsIgnoreCase("Anonymous")){
            throw new NotAuthorizedException("Invalid user");
        }
    }


}
