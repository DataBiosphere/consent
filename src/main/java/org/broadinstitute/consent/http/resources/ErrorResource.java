package org.broadinstitute.consent.http.resources;

import org.broadinstitute.consent.http.models.Error;
import org.eclipse.jetty.server.Request;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.net.URLDecoder;
import java.nio.charset.Charset;

@Path("error")
public class ErrorResource {

    @GET
    @Path("404")
    @Produces("application/json")
    public Response notFound(@Context HttpServletRequest request) {
        String originalUri = ((Request) request).getOriginalURI();
        String decodedUri = URLDecoder.decode(originalUri, Charset.defaultCharset());
        String msg = String.format("Unable to find requested path: '%s'", decodedUri);
        Error error = new Error(msg, 404);
        return Response.status(error.code()).entity(error).build();
    }
}
