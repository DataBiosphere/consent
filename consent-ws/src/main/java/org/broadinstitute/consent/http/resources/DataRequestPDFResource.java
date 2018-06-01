package org.broadinstitute.consent.http.resources;

import org.broadinstitute.consent.http.service.AbstractDataAccessRequestAPI;
import org.broadinstitute.consent.http.service.DataAccessRequestAPI;
import org.broadinstitute.consent.http.service.users.handler.ResearcherAPI;
import org.broadinstitute.consent.http.util.DarConstants;
import org.bson.Document;

import javax.annotation.security.PermitAll;
import javax.ws.rs.*;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Path("{api : (api/)?}dataRequest/{requestId}/pdf")
public class DataRequestPDFResource extends Resource {

    private final DataAccessRequestAPI darApi;

    private final ResearcherAPI researcherAPI;

    public DataRequestPDFResource(ResearcherAPI researcherAPI) {
        this.darApi = AbstractDataAccessRequestAPI.getInstance();
        this.researcherAPI = researcherAPI;
    }


    @GET
    @PermitAll
    @Produces( "application/pdf")
    public Response downloadDataRequestPdfFile(@PathParam("requestId") String requestId) {
        Document dar = new Document();//darApi.describeDataAccessRequestById(requestId);
        Map<String, String> researcherProperties = new HashMap<>();//researcherAPI.describeResearcherPropertiesForDAR(dar.getInteger(DarConstants.USER_ID));

        StreamingOutput fileStream =  new StreamingOutput()
        {
            @Override
            public void write(java.io.OutputStream output) throws IOException, WebApplicationException
            {
                try
                {
                    byte[] data = darApi.createDARDocument(dar, researcherProperties);
                    output.write(data);
                    output.flush();
                    output.close();
                }
                catch (Exception e)
                {
                    throw new WebApplicationException("File Not Found !!");
                }
            }
        };
        return Response
                .ok(fileStream, MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename = myfile.pdf")
                .build();
    }

}
