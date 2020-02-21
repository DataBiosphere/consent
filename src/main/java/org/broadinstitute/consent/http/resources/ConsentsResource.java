package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.service.ConsentService;

import javax.annotation.security.PermitAll;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Path("api/consents")
public class ConsentsResource extends Resource {

    private final Logger logger = Logger.getLogger(this.getClass());
    private final ConsentService service;

    @Inject
    public ConsentsResource(ConsentService service) {
        this.service = service;
    }

    @GET
    @Produces("application/json")
    @PermitAll
    public Response findByIds(@QueryParam("ids") String ids) {
        if (StringUtils.isBlank(ids)) {
            throw new NotFoundException("Cannot find any consents without ids");
        }
        List<String> splitIds = new ArrayList<>();
        for (String id : ids.split(",")) {
            try {
                //noinspection ResultOfMethodCallIgnored
                UUID.fromString(id);
                splitIds.add(id);
            } catch (IllegalArgumentException e) {
                logger.error("Invalid id: " + id);
            }
        }
        if (splitIds.isEmpty()) {
            logger.error("Unable to parse ids from provided consent ids: " + ids);
            throw new NotFoundException("Cannot find some consents ids: " + ids);
        }
        Collection<Consent> consents = service.findByConsentIds(splitIds);
        List<String> foundIds = consents.stream().map(Consent::getConsentId).collect(Collectors.toList());
        if (foundIds.isEmpty()) {
            throw new NotFoundException("Cannot find consents with provided ids: " + StringUtils.join(splitIds, ","));
        }
        if (foundIds.containsAll(splitIds)) {
            return Response.ok().entity(consents).build();
        } else {
            List<String> diffIds = new ArrayList<>();
            Collections.copy(splitIds, diffIds);
            diffIds.removeAll(foundIds);
            throw new NotFoundException("Cannot find some consent ids: " + StringUtils.join(diffIds, ","));
        }
    }

    @GET
    @Produces("application/json")
    @Path("{associationType}")
    @PermitAll
    public Response findByAssociationType(@PathParam("associationType") String associationType) {
        if (StringUtils.isNotBlank(associationType)) {
            Collection<Consent> consents = service.findConsentsByAssociationType(associationType);
            if (consents == null || consents.isEmpty()) {
                throw new NotFoundException("Unable to find consents for the association type: " + associationType);
            }
            return Response.ok().entity(consents).build();
        } else {
            throw new NotFoundException("Unable to find consents without an association type");
        }
    }

}
