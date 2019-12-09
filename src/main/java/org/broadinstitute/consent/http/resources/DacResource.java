package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.Role;
import org.broadinstitute.consent.http.models.dto.DataSetDTO;
import org.broadinstitute.consent.http.service.DacService;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

@Path("api/dac")
public class DacResource extends Resource {

    private DacService dacService;
    private static final Logger logger = Logger.getLogger(DacResource.class.getName());

    @Inject
    public DacResource(DacService dacService) {
        this.dacService = dacService;
    }

    @GET
    @Produces("application/json")
    @RolesAllowed({ADMIN, MEMBER, CHAIRPERSON})
    public Response findAll() {
        List<Dac> dacs = dacService.findAllDacsWithMembers();
        return Response.ok().entity(dacs).build();
    }

    @POST
    @Produces("application/json")
    @RolesAllowed({ADMIN})
    public Response createDac(Dac dac) throws Exception {
        if (dac == null) {
            throw new BadRequestException("DAC is required");
        }
        if (dac.getName() == null) {
            throw new BadRequestException("DAC Name is required");
        }
        if (dac.getDescription() == null) {
            throw new BadRequestException("DAC Description is required");
        }
        Integer dacId = dacService.createDac(dac.getName(), dac.getDescription());
        if (dacId == null) {
            throw new Exception("Unable to create DAC with name: " + dac.getName() + " and description: " + dac.getDescription());
        }
        Dac savedDac = dacService.findById(dacId);
        return Response.ok().entity(savedDac).build();
    }

    @PUT
    @Produces("application/json")
    @RolesAllowed({ADMIN})
    public Response updateDac(Dac dac) {
        if (dac == null) {
            throw new BadRequestException("DAC is required");
        }
        if (dac.getDacId() == null) {
            throw new BadRequestException("DAC ID is required");
        }
        if (dac.getName() == null) {
            throw new BadRequestException("DAC Name is required");
        }
        if (dac.getDescription() == null) {
            throw new BadRequestException("DAC Description is required");
        }
        dacService.updateDac(dac.getName(), dac.getDescription(), dac.getDacId());
        Dac savedDac = dacService.findById(dac.getDacId());
        return Response.ok().entity(savedDac).build();
    }

    @GET
    @Path("{dacId}")
    @Produces("application/json")
    @RolesAllowed({ADMIN, MEMBER, CHAIRPERSON})
    public Response findById(@PathParam("dacId") Integer dacId) {
        Dac dac = findDacById(dacId);
        return Response.ok().entity(dac).build();
    }

    @DELETE
    @Path("{dacId}")
    @Produces("application/json")
    @RolesAllowed({ADMIN})
    public Response deleteDac(@PathParam("dacId") Integer dacId) {
        findDacById(dacId);
        try {
            dacService.deleteDac(dacId);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error deleting DAC with id: " + dacId + "; " + e);
            return Response.status(500).entity("Unable to delete Data Access Committee with the provided id: " + dacId).build();
        }
        return Response.ok().build();
    }

    @POST
    @Path("{dacId}/member/{userId}")
    @RolesAllowed({ADMIN})
    public Response addDacMember(@PathParam("dacId") Integer dacId, @PathParam("userId") Integer userId) {
        checkMembership(dacId, userId);
        Role role = dacService.getMemberRole();
        DACUser user = findDacUser(userId);
        Dac dac = findDacById(dacId);
        DACUser member = dacService.addDacMember(role, user, dac);
        return Response.ok().entity(member).build();
    }

    @DELETE
    @Path("{dacId}/member/{userId}")
    @RolesAllowed({ADMIN})
    public Response removeDacMember(@PathParam("dacId") Integer dacId, @PathParam("userId") Integer userId) {
        Role role = dacService.getMemberRole();
        DACUser user = findDacUser(userId);
        Dac dac = findDacById(dacId);
        dacService.removeDacMember(role, user, dac);
        return Response.ok().build();
    }

    @POST
    @Path("{dacId}/chair/{userId}")
    @RolesAllowed({ADMIN})
    public Response addDacChair(@PathParam("dacId") Integer dacId, @PathParam("userId") Integer userId) {
        checkMembership(dacId, userId);
        Role role = dacService.getChairpersonRole();
        DACUser user = findDacUser(userId);
        Dac dac = findDacById(dacId);
        DACUser chair = dacService.addDacMember(role, user, dac);
        return Response.ok().entity(chair).build();
    }

    @DELETE
    @Path("{dacId}/chair/{userId}")
    @RolesAllowed({ADMIN})
    public Response removeDacChair(@PathParam("dacId") Integer dacId, @PathParam("userId") Integer userId) {
        Role role = dacService.getChairpersonRole();
        DACUser user = findDacUser(userId);
        Dac dac = findDacById(dacId);
        dacService.removeDacMember(role, user, dac);
        return Response.ok().build();
    }

    @GET
    @Path("{dacId}/datasets")
    @Produces("application/json")
    @RolesAllowed({ADMIN, MEMBER, CHAIRPERSON})
    public Response findAllDacDatasets(@Auth AuthUser user, @PathParam("dacId") Integer dacId) {
        findDacById(dacId);
        Set<DataSetDTO> datasets = dacService.findDatasetsByDacId(user, dacId);
        return Response.ok().entity(datasets).build();
    }

    @GET
    @Path("{dacId}/membership")
    @Produces("application/json")
    @RolesAllowed({ADMIN, MEMBER, CHAIRPERSON})
    public Response findAllDacMembers(@PathParam("dacId") Integer dacId) {
        findDacById(dacId);
        List<DACUser> members = dacService.findMembersByDacId(dacId);
        return Response.ok().entity(members).build();
    }

    @GET
    @Path("users/{term}")
    @Produces("application/json")
    @RolesAllowed({ADMIN, MEMBER, CHAIRPERSON})
    public Response filterUsers(@PathParam("term") String term) {
        List<DACUser> users = dacService.findAllDACUsersBySearchString(term);
        return Response.ok().entity(users).build();
    }

    private DACUser findDacUser(Integer userId) {
        DACUser user = dacService.findUserById(userId);
        if (user == null) {
            throw new NotFoundException("Unable to find User with the provided id: " + userId);
        }
        return user;
    }

    private Dac findDacById(Integer dacId) {
        Dac dac = dacService.findById(dacId);
        if (dac == null) {
            throw new NotFoundException("Unable to find Data Access Committee with the provided id: " + dacId);
        }
        return dac;
    }

    private void checkMembership(Integer dacId, Integer userId) {
        List<DACUser> currentMembers = dacService.findMembersByDacId(dacId);
        Optional<DACUser> isMember = currentMembers.
                stream().
                filter(u -> u.getDacUserId().equals(userId)).
                findFirst();
        if (isMember.isPresent()) {
            // This is handled as a 409 Conflict
            throw new UnsupportedOperationException("User with id " + userId + " is already a member of this DAC");
        }
    }

}
