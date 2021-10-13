package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import java.net.URI;
import java.util.List;
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
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.ConsentAssociation;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.dto.Error;
import org.broadinstitute.consent.http.service.ConsentService;
import org.broadinstitute.consent.http.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("{auth: (basic/|api/)?}consent/{consentId}/association")
public class ConsentAssociationResource extends Resource {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ConsentService consentService;
    private final UserService userService;

    @Inject
    public ConsentAssociationResource(ConsentService consentService, UserService userService) {
        this.consentService = consentService;
        this.userService = userService;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({RESEARCHER, DATAOWNER, ADMIN})
    public Response createAssociation(@Auth AuthUser user, @PathParam("consentId") String consentId, List<ConsentAssociation> body) {
        try {
            String msg = String.format("POSTing association to id '%s' with body '%s'", consentId, body.toString());
            logger.debug(msg);
            User dacUser = userService.findUserByEmail(user.getEmail());
            List<ConsentAssociation> result = consentService.createAssociation(consentId, body, dacUser.getEmail());
            URI assocURI = buildConsentAssociationURI(consentId);
            return Response.ok(result).location(assocURI).build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

    private URI buildConsentAssociationURI(String id) {
        return UriBuilder.fromResource(ConsentAssociationResource.class).build("api/", id);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({RESEARCHER, DATAOWNER, ADMIN})
    public Response updateAssociation(@Auth AuthUser user, @PathParam("consentId") String consentId, List<ConsentAssociation> body) {
        try {
            String msg = String.format("PUTing association to id '%s' with body '%s'", consentId, body.toString());
            logger.debug(msg);
            List<ConsentAssociation> result = consentService.updateAssociation(consentId, body, user.getEmail());
            URI assocURI = buildConsentAssociationURI(consentId);
            return Response.ok(result).location(assocURI).build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @PermitAll
    public Response getAssociation(@PathParam("consentId") String consentId, @QueryParam("associationType") String atype, @QueryParam("objectId") String objectId) {
        try {
            String msg = String.format("GETing association for id '%s' with associationType='%s' and id='%s'", consentId, (atype == null ? "<null>" : atype), (objectId == null ? "<null>" : objectId));
            logger.debug(msg);
            if (atype == null && objectId != null)
                return Response.status(Response.Status.BAD_REQUEST).build();
            List<ConsentAssociation> result = consentService.getAssociation(consentId, atype, objectId);
            URI assocURI = buildConsentAssociationURI(consentId);
            return Response.ok(result).location(assocURI).build();
        } catch (Exception e) {
            return Response.status(Response.Status.NOT_FOUND).entity(new Error(String.format("Could not find consent with id %s", consentId), Response.Status.NOT_FOUND.getStatusCode())).build();
        }
    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(ADMIN)
    public Response deleteAssociation(@PathParam("consentId") String consentId, @QueryParam("associationType") String atype, @QueryParam("objectId") String objectId) {
        try {
            String msg = String.format("DELETEing association for id '%s' with associationType='%s' and id='%s'", consentId, (atype == null ? "<null>" : atype), (objectId == null ? "<null>" : objectId));
            logger.debug(msg);
            if (atype == null && objectId != null)
                return Response.status(Response.Status.BAD_REQUEST).build();
            List<ConsentAssociation> result = consentService.deleteAssociation(consentId, atype, objectId);
            URI assocURI = buildConsentAssociationURI(consentId);
            return Response.ok(result).location(assocURI).build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }
}
