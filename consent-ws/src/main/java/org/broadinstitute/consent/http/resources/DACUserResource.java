package org.broadinstitute.consent.http.resources;

import org.broadinstitute.consent.http.enumeration.DACUserRoles;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.DACUserRole;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.dto.Error;
import org.broadinstitute.consent.http.models.user.ValidateDelegationResponse;
import org.broadinstitute.consent.http.service.AbstractElectionAPI;
import org.broadinstitute.consent.http.service.AbstractVoteAPI;
import org.broadinstitute.consent.http.service.ElectionAPI;
import org.broadinstitute.consent.http.service.VoteAPI;
import org.broadinstitute.consent.http.service.users.AbstractDACUserAPI;
import org.broadinstitute.consent.http.service.users.DACUserAPI;
import org.broadinstitute.consent.http.service.users.handler.UserRoleHandlerException;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Path("{api : (api/)?}dacuser")
public class DACUserResource extends Resource {

    private final DACUserAPI dacUserAPI;
    private final ElectionAPI electionAPI;
    private final VoteAPI voteAPI;

    public DACUserResource() {
        this.electionAPI = AbstractElectionAPI.getInstance();
        this.voteAPI = AbstractVoteAPI.getInstance();
        this.dacUserAPI = AbstractDACUserAPI.getInstance();
    }

    @POST
    @Consumes("application/json")
    @RolesAllowed("ADMIN")
    public Response createdDACUser(@Context UriInfo info, DACUser dac) {
        URI uri;
        DACUser dacUser;
        try {
            dacUser = dacUserAPI.createDACUser(dac);
            if (isChairPerson(dacUser.getRoles())) {
                dacUserAPI.updateExistentChairPersonToAlumni(dacUser.getDacUserId());
                List<Election> elections = electionAPI.cancelOpenElectionAndReopen();
                voteAPI.createVotesForElections(elections, true);
            }
            uri = info.getRequestUriBuilder().path("{email}").build(dacUser.getEmail());
            return Response.created(uri).entity(dacUser).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(new Error(e.getMessage(), Response.Status.BAD_REQUEST.getStatusCode())).build();
        }
    }

    @GET
    @Produces("application/json")
    @RolesAllowed("ADMIN")
    public Collection<DACUser> describeAllUsers() {
        return dacUserAPI.describeUsers();
    }

    @GET
    @Path("/{email}")
    @Produces("application/json")
    @PermitAll
    public DACUser describe(@PathParam("email") String email) {
        return dacUserAPI.describeDACUserByEmail(email);
    }

    @PUT
    @Path("/{id}")
    @Consumes("application/json")
    @Produces("application/json")
    @RolesAllowed("ADMIN")
    public Response update(@Context UriInfo info, Map<String, DACUser> dac, @PathParam("id") Integer id) {
        try {
            URI uri = info.getRequestUriBuilder().path("{id}").build(id);
            DACUser dacUser = dacUserAPI.updateDACUserById(dac, id);
            return Response.ok(uri).entity(dacUser).build();
        } catch (UserRoleHandlerException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(new Error(e.getMessage(), Response.Status.BAD_REQUEST.getStatusCode())).build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{email}")
    @RolesAllowed("ADMIN")
    public Response delete(@PathParam("email") String email, @Context UriInfo info) {
        dacUserAPI.deleteDACUser(email);
        return Response.ok().entity("User was deleted").build();
    }


    @POST
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/validateDelegation")
    @PermitAll
    public Response validateDelegation(@QueryParam("role") String role, DACUser dac) {
        DACUser dacUser;
        try {
            dacUser = dacUserAPI.describeDACUserByEmail(dac.getEmail());
            ValidateDelegationResponse delegationResponse = dacUserAPI.validateNeedsDelegation(dacUser, role);
            return Response.ok().entity(delegationResponse).build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

    @PUT
    @Path("/status/{userId}")
    @Consumes("application/json")
    @Produces("application/json")
    @RolesAllowed("ADMIN")
    public Response updateStatus(@PathParam("userId") Integer userId, DACUserRole dACUserRole) {
        try {
            return Response.ok(dacUserAPI.updateRoleStatus(dACUserRole, userId)).build();
        }catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

    @GET
    @Path("/status/{userId}")
    @Consumes("application/json")
    @Produces("application/json")
    @RolesAllowed("ADMIN")
    public Response getUserStatus(@PathParam("userId") Integer userId) {
        try {
            return Response.ok(dacUserAPI.getRoleStatus(userId)).build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

    @PUT
    @Path("/name/{id}")
    @Consumes("application/json")
    @Produces("application/json")
    @RolesAllowed({"ADMIN","RESEARCHER"})
    public Response updateName(DACUser user, @PathParam("id") Integer id) {
        try {
            DACUser dacUser = dacUserAPI.updateNameById(user, id);
            return Response.ok().entity(dacUser).build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

    private boolean isChairPerson(List<DACUserRole> roles) {
        boolean isChairPerson = false;
        for (DACUserRole role : roles) {
            if (role.getName().equalsIgnoreCase(DACUserRoles.CHAIRPERSON.getValue())) {
                isChairPerson = true;
                break;
            }
        }
        return isChairPerson;
    }

}

