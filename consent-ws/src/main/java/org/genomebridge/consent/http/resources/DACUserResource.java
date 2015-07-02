package org.genomebridge.consent.http.resources;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.genomebridge.consent.http.models.DACUser;
import org.genomebridge.consent.http.service.AbstractDACUserAPI;
import org.genomebridge.consent.http.service.DACUserAPI;

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
