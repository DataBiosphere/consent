package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import org.broadinstitute.consent.http.enumeration.EmailType;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.service.EmailService;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import static org.broadinstitute.consent.http.resources.Resource.ADMIN;

@Path("/api/mail")
public class MailResource {
    private final EmailService emailService;

    @Inject
    public MailResource(EmailService emailService) {
        this.emailService = emailService;
    }

    @GET
    @Produces("application/json")
    @Path("/type/{type}")
    @RolesAllowed({ADMIN})
    public Response getEmailByType(@Auth AuthUser authUser,
                                   @PathParam("type") EmailType emailType,
                                   @DefaultValue ("20") @QueryParam("limit") Integer limit,
                                   @DefaultValue("0") @QueryParam("offset") Integer offset) {
        return Response.ok().entity(emailService.fetchEmailMessages(emailType, limit, offset)).build();
    }
}
