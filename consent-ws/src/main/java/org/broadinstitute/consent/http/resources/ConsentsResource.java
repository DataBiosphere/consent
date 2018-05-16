package org.broadinstitute.consent.http.resources;

import com.google.common.base.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.service.AbstractConsentAPI;
import org.broadinstitute.consent.http.service.ConsentAPI;

import javax.annotation.security.PermitAll;
import javax.ws.rs.*;
import java.util.*;

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
        // ADD LAST ELECTION STATUS HERE
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

}
