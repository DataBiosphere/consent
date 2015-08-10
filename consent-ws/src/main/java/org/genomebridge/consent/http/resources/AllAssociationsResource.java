package org.genomebridge.consent.http.resources;

import org.apache.log4j.Logger;
import org.genomebridge.consent.http.service.AbstractConsentAPI;
import org.genomebridge.consent.http.service.ConsentAPI;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;

@Path("consent/associations/{associationType}/{id}")
public class AllAssociationsResource extends Resource {
    private ConsentAPI api;

    @Context
    UriInfo uriInfo;

    public AllAssociationsResource() {
        this.api = AbstractConsentAPI.getInstance();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getConsentsForAssociation(@PathParam("associationType") String atype, @PathParam("id") String objectId) {
        try {
            String msg = String.format("GETing all consents with associations of type='%s' for object '%s'.", (atype == null ? "<null>" : atype), (objectId == null ? "<null>" : objectId));
            logger().debug(msg);
            if (atype == null || objectId == null)
                return Response.status(Response.Status.BAD_REQUEST).build();
            List<String> result = api.getConsentsForAssociation(uriInfo, atype, objectId);
            if (result.size() > 0) {
                URI uri = URI.create(result.get(0));
                return Response.ok(result).location(uri).build();
            }
            return Response.ok(result).build();
        } catch (Exception e) { //catch (UnknownIdentifierException e) {
            logger().debug(String.format("GETconsentsForAssociation:  Caught exception '%s' in getConsentsForAssociation", e.getMessage()));
            throw new NotFoundException("Could not find associations for object");
        }
    }

    @Override
    protected Logger logger() {
        return Logger.getLogger("AllAssociationsResource");
    }
}
