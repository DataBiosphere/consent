package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.User;
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
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.List;

@Path("api/dac")
public class DacResource extends Resource {

    private DacService dacService;

    @Inject
    public DacResource(DacService dacService) {
        this.dacService = dacService;
    }

    @GET
    @Produces("application/json")
    @RolesAllowed({ADMIN, MEMBER, CHAIRPERSON})
    public Response findAll() {
        List<Dac> dacs = dacService.findAll();
        return Response.ok().entity(dacs).build();
    }

    @POST
    @Produces("application/json")
    @RolesAllowed({ADMIN})
    public Response createDac(@Context UriInfo info, @Auth User user, Dac dac) throws Exception {
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
    public Response updateDac(@Context UriInfo info, @Auth User user, Dac dac) {
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
        Dac dac = dacService.findById(dacId);
        if (dac != null) {
            return Response.ok().entity(dac).build();
        }
        throw new NotFoundException("Unable to find Data Access Committee with the provided id: " + dacId);
    }

    @DELETE
    @Path("{dacId}")
    @Produces("application/json")
    @RolesAllowed({ADMIN})
    public Response deleteDac(@Context UriInfo info, @Auth User user, @PathParam("dacId") Integer dacId) {
        Dac dac = dacService.findById(dacId);
        if (dac == null) {
            throw new NotFoundException("Unable to find Data Access Committee with the provided id: " + dacId);
        }
        dacService.deleteDac(dacId);
        return Response.ok().build();
    }

}
