package org.genomebridge.consent.http.resources;

import org.genomebridge.consent.http.enumeration.DACUserRoles;
import org.genomebridge.consent.http.models.DACUser;
import org.genomebridge.consent.http.models.DACUserRole;
import org.genomebridge.consent.http.models.Election;
import org.genomebridge.consent.http.service.*;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Collection;
import java.util.List;

@Path("{api : (api/)?}dacuser")
public class DACUserResource extends Resource {

    private final DACUserAPI dacUserAPI;
    private final ElectionAPI electionAPI;
    private final VoteAPI voteAPI;

    public DACUserResource(){
        this.dacUserAPI = AbstractDACUserAPI.getInstance();
        this.electionAPI = AbstractElectionAPI.getInstance();
        this.voteAPI = AbstractVoteAPI.getInstance();
    }

    @POST
    @Consumes("application/json")
    public Response createdDACUser(@Context UriInfo info, DACUser dac) {
        URI uri;
        DACUser dacUser;
        try {
            dacUser = dacUserAPI.createDACUser(dac);
            if(isChairPerson(dacUser.getRoles())){
                dacUserAPI.updateExistentChairPersonToAlumni(dacUser.getDacUserId());
                List<Election> elections = electionAPI.cancelOpenElectionAndReopen();
                voteAPI.createVotesForElections(elections, true);
            }
            uri = info.getRequestUriBuilder().path("{email}").build(dacUser.getEmail());
            return Response.created(uri).entity(dacUser).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @GET
    @Produces("application/json")
    public Collection<DACUser> describeAllUsers() {
        return dacUserAPI.describeUsers();
    }

    @GET
    @Path("/{email}")
    @Produces("application/json")
    public DACUser describe(@PathParam("email") String email) {
        return dacUserAPI.describeDACUserByEmail(email);
    }

    @PUT
    @Path("/{id}")
    @Consumes("application/json")
    @Produces("application/json")
    public Response update(@Context UriInfo info, DACUser dac, @PathParam("id") Integer id) {
        try {
            URI uri = info.getRequestUriBuilder().path("{id}").build(id);
            DACUser dacUser = dacUserAPI.updateDACUserById(dac, id);
            return Response.ok(uri).entity(dacUser).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e).build();
        }

    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{email}")
    public Response delete(@PathParam("email") String email, @Context UriInfo info) {
        dacUserAPI.deleteDACUser(email);
        return Response.ok().entity("User was deleted").build();
    }

    private boolean isChairPerson(List<DACUserRole> roles) {
        boolean isChairPerson = false;
        for(DACUserRole role : roles){
            if(role.getName().equals(DACUserRoles.CHAIRPERSON.getValue())){
                isChairPerson = true;
                break;
            }
        }
        return isChairPerson;
    }
}
