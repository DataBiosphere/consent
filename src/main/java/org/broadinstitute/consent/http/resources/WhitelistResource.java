package org.broadinstitute.consent.http.resources;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import org.broadinstitute.consent.http.cloudstore.GCSStore;
import org.broadinstitute.consent.http.models.AuthUser;

import javax.annotation.security.RolesAllowed;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.logging.Logger;

@Path("api/whitelist")
public class WhitelistResource extends Resource {

    private GCSStore gcsStore;

    @Inject
    public WhitelistResource(GCSStore gcsStore) {
        this.gcsStore = gcsStore;
    }

    @POST
    @Consumes("application/json")
    @RolesAllowed(ADMIN)
    public Response postWhitelist(@Context String filePath, @Auth AuthUser authUser) {
        // define bucket (in a config?)
        // push file to bucket
        try {
            String filePathStr = "you are "+authUser;
            Logger.getLogger(filePathStr);
            return Response.ok().build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }
}
