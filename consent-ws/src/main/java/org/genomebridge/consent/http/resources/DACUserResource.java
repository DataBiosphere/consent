package org.genomebridge.consent.http.resources;

import org.genomebridge.consent.http.models.DACUser;
import org.genomebridge.consent.http.service.AbstractDACUserAPI;
import org.genomebridge.consent.http.service.DACUserAPI;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

@Path("dacuser")
public class DACUserResource extends Resource {

    private DACUserAPI api;

    public DACUserResource() {
        this.api = AbstractDACUserAPI.getInstance();
    }

    @POST
    @Consumes("application/json")
    public Response createdDACUser(@Context UriInfo info, DACUser dac) {
        URI uri;
        DACUser dacUser;
        try {
            dacUser = api.createDACUser(dac);
            uri = info.getRequestUriBuilder().path("{email}").build(dacUser.getEmail());
            return Response.created(uri).entity(dacUser).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("/{email}")
    @Produces("application/json")
    public DACUser describe(@PathParam("email") String email) {
        return api.describeDACUserByEmail(email);
    }

    @PUT
    @Consumes("application/json")
    @Produces("application/json")
    public DACUser update(DACUser dac) {
        return api.updateDACUserByEmail(dac);
    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{email}")
    public Response delete(@PathParam("email") String email, @Context UriInfo info) {
        api.deleteDACUser(email);
        return Response.ok().entity("User was deleted").build();
    }
}
