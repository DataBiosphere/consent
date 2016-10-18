package org.broadinstitute.consent.http.resources;

import io.dropwizard.auth.Auth;
import org.apache.log4j.Logger;
import org.broadinstitute.consent.http.enumeration.AssociationType;
import org.broadinstitute.consent.http.models.ConsentAssociation;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.dto.Error;
import org.broadinstitute.consent.http.service.AbstractAuditServiceAPI;
import org.broadinstitute.consent.http.service.AbstractConsentAPI;
import org.broadinstitute.consent.http.service.AbstractDataAccessRequestAPI;
import org.broadinstitute.consent.http.service.AbstractDataSetAPI;
import org.broadinstitute.consent.http.service.AbstractElectionAPI;
import org.broadinstitute.consent.http.service.AuditServiceAPI;
import org.broadinstitute.consent.http.service.ConsentAPI;
import org.broadinstitute.consent.http.service.DataAccessRequestAPI;
import org.broadinstitute.consent.http.service.DataSetAPI;
import org.broadinstitute.consent.http.service.ElectionAPI;
import org.broadinstitute.consent.http.service.users.AbstractDACUserAPI;
import org.broadinstitute.consent.http.service.users.DACUserAPI;

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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Path("{auth: (basic/|api/)?}consent/{id}/association")
public class ConsentAssociationResource extends Resource {

    private final ConsentAPI api;
    private final DACUserAPI dacUserAPI;

    public ConsentAssociationResource() {
        this.api = AbstractConsentAPI.getInstance();
        this.dacUserAPI = AbstractDACUserAPI.getInstance();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"RESEARCHER", "DATAOWNER", "ADMIN"})
    public Response createAssociation(@Auth User user, @PathParam("id") String consentId, ArrayList<ConsentAssociation> body) {
        try {
            String msg = String.format("POSTing association to id '%s' with body '%s'", consentId, body.toString());
            for (ConsentAssociation association : body) {
                if(association.getAssociationType().equals(AssociationType.WORKSPACE.getValue()) && api.hasWorkspaceAssociation(association.getElements().get(0))){
                    return Response.status(Response.Status.CONFLICT).entity(new Error("Workspace associations can only be created once.", Response.Status.CONFLICT.getStatusCode())).build();
                }
            }
            logger().debug(msg);
            DACUser dacUser = dacUserAPI.describeDACUserByEmail(user.getName());
            List<ConsentAssociation> result = api.createAssociation(consentId, body, dacUser.getEmail());
            URI assocURI = buildConsentAssociationURI(consentId);
            return Response.ok(result).location(assocURI).build();
        }catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

    private URI buildConsentAssociationURI(String id) {
        return UriBuilder.fromResource(ConsentAssociationResource.class).build("api/", id);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"RESEARCHER", "DATAOWNER", "ADMIN"})
    public Response updateAssociation(@Auth User user, @PathParam("id") String consentId, ArrayList<ConsentAssociation> body) {
        try {
            String msg = String.format("PUTing association to id '%s' with body '%s'", consentId, body.toString());
            for (ConsentAssociation association : body) {
                if(association.getAssociationType().equals(AssociationType.WORKSPACE.getValue())){
                    return Response.status(Response.Status.CONFLICT).entity(new Error("Workspace associations can't be updated.", Response.Status.CONFLICT.getStatusCode())).build();
                }
            }
            logger().debug(msg);
            List<ConsentAssociation> result = api.updateAssociation(consentId, body, user.getName());
            URI assocURI = buildConsentAssociationURI(consentId);
            return Response.ok(result).location(assocURI).build();
        }catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @PermitAll
    public Response getAssociation(@PathParam("id") String consentId, @QueryParam("associationType") String atype, @QueryParam("id") String objectId) {
        try {
            String msg = String.format("GETing association for id '%s' with associationType='%s' and id='%s'", consentId, (atype == null ? "<null>" : atype), (objectId == null ? "<null>" : objectId));
            logger().debug(msg);
            if (atype == null && objectId != null)
                return Response.status(Response.Status.BAD_REQUEST).build();
            List<ConsentAssociation> result = api.getAssociation(consentId, atype, objectId);
            URI assocURI = buildConsentAssociationURI(consentId);
            return Response.ok(result).location(assocURI).build();
        } catch (Exception e) {
            return Response.status(Response.Status.NOT_FOUND).entity(new Error(String.format("Could not find consent with id %s", consentId), Response.Status.NOT_FOUND.getStatusCode())).build();
        }
    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("ADMIN")
    public Response deleteAssociation(@PathParam("id") String consentId, @QueryParam("associationType") String atype, @QueryParam("id") String objectId) {
        try {
            String msg = String.format("DELETEing association for id '%s' with associationType='%s' and id='%s'", consentId, (atype == null ? "<null>" : atype), (objectId == null ? "<null>" : objectId));
            logger().debug(msg);
            if (atype == null && objectId != null)
                return Response.status(Response.Status.BAD_REQUEST).build();
            List<ConsentAssociation> result = api.deleteAssociation(consentId, atype, objectId);
            URI assocURI = buildConsentAssociationURI(consentId);
            return Response.ok(result).location(assocURI).build();
        }catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

    @Override
    protected Logger logger() {
        return Logger.getLogger("ConsentAssociationResource");
    }
}
