package org.broadinstitute.consent.http.resources;

import com.google.api.client.http.GenericUrl;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.service.WhitelistService;
import org.glassfish.jersey.media.multipart.FormDataParam;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("api/whitelist")
public class WhitelistResource extends Resource {

    private final WhitelistService whitelistService;

    @Inject
    public WhitelistResource(WhitelistService whitelistService) {
        this.whitelistService = whitelistService;
    }

    @POST
    @RolesAllowed(ADMIN)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response postWhitelist(@Auth AuthUser user, @FormDataParam("data") String data) {
        try {
            GenericUrl url = whitelistService.postWhitelist(data);
            return Response.created(url.toURI()).build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

}
