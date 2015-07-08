package org.genomebridge.consent.http.resources;

import org.genomebridge.consent.http.models.DACUser;
import org.genomebridge.consent.http.models.Vote;
import org.genomebridge.consent.http.service.AbstractDACUserAPI;
import org.genomebridge.consent.http.service.DACUserAPI;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

@Path("dacuser")
public class DACUserResource extends Resource {

    private DACUserAPI api;

    public DACUserResource() { this.api = AbstractDACUserAPI.getInstance(); }

    @POST
    @Consumes("application/json")
    public Integer createdDACUser( DACUser rec) {
            return api.createDACUser(rec);
    }

    @GET
    @Path("/{email}")
    @Produces("application/json")
    public DACUser describe(@PathParam("email") String email) {

        return  api.describeDACUserByEmail(email);
    }
}
