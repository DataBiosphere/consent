package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;
import org.broadinstitute.consent.http.cloudstore.GCSStore;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("api/whitelist")
public class WhitelistResource extends Resource {

    private GCSStore gcsStore;

    private Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    @Inject
    public WhitelistResource(GCSStore gcsStore) {
        this.gcsStore = gcsStore;
    }

    @POST
    @RolesAllowed(ADMIN)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response postWhitelist(@FormDataParam("fileData") String fileData) {
        // define bucket (in a config?)
        // push file to bucket
        try {
            logger.info(fileData);
            return Response.ok(fileData).build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }
}
