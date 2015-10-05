package org.genomebridge.consent.http.resources;

import org.genomebridge.consent.http.models.*;
import org.genomebridge.consent.http.service.*;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

@Path("{api : (api/)?}match")
public class MatchResource extends Resource {

    private final MatchAPI matchAPI;

    public MatchResource() {
        this.matchAPI = AbstractMatchAPI.getInstance();
    }

    @POST
    @Consumes("application/json")
    public Response createMatch(@Context UriInfo info, Match match) {
        try {
           match = matchAPI.create(match);
           URI uri =  info.getRequestUriBuilder().path("{id}").build(match.getId());
           return Response.created(uri).entity(match).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @PUT
    @Path("/{id}")
    @Consumes("application/json")
    @Produces("application/json")
    public Response update(@Context UriInfo info, Match match, @PathParam("id") Integer id) {
        try {
            URI uri = info.getRequestUriBuilder().path("{id}").build(id);
            return Response.ok(uri).entity(matchAPI.update(match, id)).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }

    }

    @GET
    @Path("/{id}")
    @Produces("application/json")
    public Response describeMatchById(@PathParam("id") Integer id) {
        try{
            return Response.status(Response.Status.OK).entity(matchAPI.findMatchById(id)).build();
        }catch (NotFoundException e){
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        }
    }

    @DELETE
    @Path("/{id}")
    @Produces("application/json")
    public Response delete(@PathParam("id") Integer id) {
        try{
            return Response.status(Response.Status.OK).entity("Match was deleted").build();
        }catch (NotFoundException e){
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        }
    }
}
