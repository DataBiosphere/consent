package org.broadinstitute.consent.http.resources;

import com.google.common.base.Optional;
import io.dropwizard.auth.Auth;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.dto.ConsentGroupNameDTO;
import org.broadinstitute.consent.http.service.AbstractConsentAPI;
import org.broadinstitute.consent.http.service.ConsentAPI;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.*;
import java.util.List;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * This service will find all consents for a comma-separated list of ids or for an association type.
 */
@Path("{api : (api/)?}consents")
public class ConsentsResource extends Resource {

    private final ConsentAPI api;

    public ConsentsResource() {
        this.api = AbstractConsentAPI.getInstance();
    }

    @GET
    @Produces("application/json")
    @PermitAll
    public Collection<Consent> findByIds(@QueryParam("ids") Optional<String> ids) {
        if (ids.isPresent()) {
            List<String> splitIds = new ArrayList<>();
            for (String id : ids.get().split(",")) {
                try {
                    //noinspection ResultOfMethodCallIgnored
                    UUID.fromString(id);
                    splitIds.add(id);
                } catch (IllegalArgumentException e) {
                    logger().error("Invalid id: " + id);
                }
            }
            if (splitIds.isEmpty()) {
                logger().error("Unable to parse ids from provided consent ids: " + ids);
                throw new NotFoundException("Cannot find some consents ids: " + ids);
            }
            Collection<Consent> consents = api.retrieve(splitIds);
            List<String> foundIds = new ArrayList<>();
            for (Consent consent : consents) {
                foundIds.add(consent.consentId);
            }
            if (splitIds.containsAll(foundIds)) {
                return consents;
            } else {
                List<String> diffIds = new ArrayList<>();
                Collections.copy(splitIds, diffIds);
                diffIds.removeAll(foundIds);
                throw new NotFoundException("Cannot find some consents ids: " + StringUtils.join(diffIds, ","));
            }
        } else {
            throw new NotFoundException("Cannot find any consents without ids");
        }
    }

    /*
     * Prefer to use Optional<String> but jersey complains with "SEVERE: Missing dependency for method..."
     * when used in combination with PathParam
     */
    @GET
    @Produces("application/json")
    @Path("{associationType}")
    @PermitAll
    public Collection<Consent> findByAssociationType(@PathParam("associationType") String associationType) {
        if (associationType != null) {
            Collection<Consent> consents = api.findConsentsByAssociationType(associationType);
            if (consents == null || consents.isEmpty()) {
                throw new NotFoundException("Unable to find consents for the association type: " + associationType);
            }
            return consents;
        } else {
            throw new NotFoundException("Unable to find consents without an association type");
        }
    }

    @Override
    protected Logger logger() {
        return Logger.getLogger("ConsentsResource");
    }


    /**
     * Given that these end-point isn't mapped in swagger follow this steps:
     * 1. Works only for Admin users. It should be used via a REST client, such as Postman.
     * What it's needed?
     *     Admin user token
     *     point to /api/consents/group-names/
     *     Headers: Accept: application/json Authorization: Bearer token_admin Content-Type: multipart/json
     *     Body of the request: Key -> data; Value -> file
     *
     * 2. The info of the group names comes from the ORSP db. Run this query:
     * <code>select  dur.vault_consent_id as consentId, i.summary as groupName
     * from issue i
     * inner join data_use_restriction dur on dur.consent_group_key = i.project_key
     * where i.type = 'Consent Group'
     * and dur.vault_export_date is not null;</code>
     *
     * 3. Export to csv file. This file should be exported to JSON with the following format:
     * [
     *     {
     *         "consentId": "testId",
     *         "groupName": "lorem ipsum / 123"
     *     },
     *     {
     *         "consentId": "testId2",
     *         "groupName": "lorem ipsum / 124"
     *     }
     * ]
     * @param info
     * @param user
     * @param data
     * @return
     */
    @POST
    @Path("group-names")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("ADMIN")
    public Response updateGroupNames(@Context UriInfo info, @Auth User user, List<ConsentGroupNameDTO> data) {
        try {
            List<ConsentGroupNameDTO> errors = api.updateConsentGroupNames(data);
            if (errors.isEmpty()) {
                return Response.status(Response.Status.OK).build();
            } else {
                return Response.status(Response.Status.BAD_REQUEST).entity(errors).build();
            }
        } catch (Exception e) {
            return createExceptionResponse(e);
        }

    }

}
