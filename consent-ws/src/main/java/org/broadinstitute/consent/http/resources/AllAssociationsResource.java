package org.broadinstitute.consent.http.resources;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.broadinstitute.consent.http.models.dto.Error;
import org.broadinstitute.consent.http.service.AbstractConsentAPI;
import org.broadinstitute.consent.http.service.ConsentAPI;

import javax.annotation.security.PermitAll;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;

@Path("{auth: (basic/|api/)?}consent/associations/{associationType}/{id}")
public class AllAssociationsResource extends Resource {
    private final ConsentAPI api;

    @Context
    UriInfo uriInfo;

    public AllAssociationsResource() {
        this.api = AbstractConsentAPI.getInstance();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @PermitAll
    public Response getConsentsForAssociation(@PathParam("associationType") String atype, @PathParam("id") String objectId) {
        try {
            String msg = String.format("GETing all consents with associations of type='%s' for object '%s'.", (atype == null ? "<null>" : atype), (objectId == null ? "<null>" : objectId));
            logger().debug(msg);
            if (StringUtils.isEmpty(atype)  || StringUtils.isEmpty(objectId))
                return Response.status(Response.Status.BAD_REQUEST).build();
            List<String> result = api.getConsentsForAssociation(uriInfo, atype, objectId);
            if (CollectionUtils.isNotEmpty(result)) {
                URI uri = URI.create(result.get(0));
                return Response.ok(result).location(uri).build();
            }else{
                return Response.status(Response.Status.NOT_FOUND).entity(new Error("Could not find associations for object", Response.Status.NOT_FOUND.getStatusCode())).build();
            }
        } catch (Exception e) {
            logger().debug(String.format("GETconsentsForAssociation:  Caught exception '%s' in getConsentsForAssociation", e.getMessage()));
            return Response.serverError().entity(new Error(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())).build();
        }
    }

    @Override
    protected Logger logger() {
        return Logger.getLogger("AllAssociationsResource");
    }
}
