package org.genomebridge.consent.autocomplete.resources;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.genomebridge.ontology.TranslationHelper;

@Path("/translate")
public class TranslateResource {

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response translate(
            @QueryParam("for") String forParam,
            String restriction ) {
        if ( "purpose".equals(forParam) )
            return Response.ok().entity(TranslationHelper.translatePurpose(restriction)).build();
        if ( "sampleset".equals(forParam) )
            return Response.ok().entity(TranslationHelper.translateSample(restriction)).build();
        return Response.status(Response.Status.BAD_REQUEST).build();
    }
}
