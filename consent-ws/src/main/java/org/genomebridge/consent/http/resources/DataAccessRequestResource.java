package org.genomebridge.consent.http.resources;

import java.io.IOException;
import java.net.URI;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import org.genomebridge.consent.http.models.grammar.UseRestriction;
import org.genomebridge.consent.http.service.*;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bson.Document;

@Path("{api : (api/)?}dar")
public class DataAccessRequestResource extends Resource {

    private final DataAccessRequestAPI dataAccessRequestAPI;
    private static final ObjectMapper mapper = new ObjectMapper();
    private final ResearchPurposeAPI researchPurposeAPI;

    public DataAccessRequestResource() {
        this.dataAccessRequestAPI = AbstractDataAccessRequestAPI.getInstance();
        this.researchPurposeAPI = AbstractResearchPurposeAPI.getInstance();
    }

    @POST
    @Consumes("application/json")
    @Produces("application/json")
    public Response createdDataAccessRequest(@Context UriInfo info, Document dar) {

        URI uri;
        Document result = null;
        UseRestriction useRestriction = null;
        Document rus = null;

        try {
            if (!requiresManualReview(dar)) {
                // generates research purpose, if needed, and store it on Document rus
                useRestriction = dataAccessRequestAPI.createStructuredResearchPurpose(dar);
                rus = Document.parse(useRestriction.toString());
                dar.append("restriction", rus);
            }
        } catch (IOException ex) {
            Logger.getLogger(DataAccessRequestResource.class.getName()).log(Level.SEVERE, "while creating useRestriction " + dar.toJson(), ex);
        }

        try {
            result = dataAccessRequestAPI.createDataAccessRequest(dar);
            uri = info.getRequestUriBuilder().path("{id}").build(result.get("_id"));
            return Response.created(uri).entity(result).build();
        } catch (Exception e) {
            dataAccessRequestAPI.deleteDataAccessRequest(result);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

    @GET
    @Produces("application/json")
    public List<Document> describeDataAccessRequests() {
        return dataAccessRequestAPI.describeDataAccessRequests();
    }

    @GET
    @Path("/{id}")
    @Produces("application/json")
    public Document describe(@PathParam("id") String id) {
        return dataAccessRequestAPI.describeDataAccessRequestById(id);
    }

    // Fields that trigger manual review flag.
    String[] fieldsForManualReview = {
        "other",
        "illegalbehave",
        "addiction",
        "sexualdiseases",
        "stigmatizediseases",
        "vulnerablepop",
        "popmigration",
        "psychtraits",
        "nothealth"
    };

    private boolean requiresManualReview(Document dar) throws IOException {
        Map<String, Object> form = parseAsMap(dar.toJson());
        for (String field : fieldsForManualReview) {
            if (form.containsKey(field)) {
                if ((boolean) form.get(field)) {
                    return true;
                }
            }
        }
        return false;
    }

    private Map<String, Object> parseAsMap(String str) throws IOException {
        ObjectReader reader = mapper.reader(Map.class);
        return reader.readValue(str);
    }
}
