package org.genomebridge.consent.http.resources;

import com.google.common.base.Optional;
import com.sun.jersey.api.NotFoundException;
import org.apache.commons.lang.StringUtils;
import org.genomebridge.consent.http.models.Consent;
import org.genomebridge.consent.http.service.AbstractConsentAPI;
import org.genomebridge.consent.http.service.ConsentAPI;
import org.genomebridge.consent.http.service.UnknownIdentifierException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import java.util.*;

/**
 * This service will find all consents for the provided ids.
 */
@Path("/consents")
public class ConsentsResource extends Resource {

    private ConsentAPI api;

    public ConsentsResource() { this.api = AbstractConsentAPI.getInstance(); }

    @GET
    @Produces("application/json")
    public Collection<Consent> retrieve(@QueryParam("ids") Optional<String> ids) {
        if (ids.isPresent()) {
            List<String> splitIds = Arrays.asList(ids.get().split(","));
            Collection<Consent> consents = api.retrieve(splitIds);
            List<String> foundIds = new ArrayList<>();
            for (Consent consent: consents) {
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
        }
        else {
            throw new NotFoundException("Cannot find any consents without ids");
        }
    }

}
