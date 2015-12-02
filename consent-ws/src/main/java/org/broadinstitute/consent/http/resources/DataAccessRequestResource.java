package org.broadinstitute.consent.http.resources;

import org.broadinstitute.consent.http.enumeration.TranslateType;
import org.broadinstitute.consent.http.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import freemarker.template.TemplateException;
import org.apache.commons.collections.CollectionUtils;
import org.bson.Document;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.darsummary.DARModalDetailsDTO;
import org.broadinstitute.consent.http.models.grammar.UseRestriction;

import javax.mail.MessagingException;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Path("{api : (api/)?}dar")
public class DataAccessRequestResource extends Resource {

    private final DataAccessRequestAPI dataAccessRequestAPI;
    private static final ObjectMapper mapper = new ObjectMapper();
    private final ConsentAPI consentAPI;
    private final MatchProcessAPI matchProcessAPI;
    private final EmailNotifierAPI emailApi;
    private final TranslateServiceAPI translateServiceAPI = AbstractTranslateServiceAPI.getInstance();
    private static final Logger logger = Logger.getLogger(DataAccessRequestResource.class.getName());

    public DataAccessRequestResource() {
        this.dataAccessRequestAPI = AbstractDataAccessRequestAPI.getInstance();
        this.consentAPI = AbstractConsentAPI.getInstance();
        this.matchProcessAPI = AbstractMatchProcessAPI.getInstance();
        this.emailApi = AbstractEmailNotifierAPI.getInstance();
    }

    @POST
    @Consumes("application/json")
    @Produces("application/json")
    public Response createdDataAccessRequest(@Context UriInfo info, Document dar) {

        URI uri;
        Document result = null;
        UseRestriction useRestriction;
        Document rus;

        try {
            if (!requiresManualReview(dar)) {
                // generates research purpose, if needed, and store it on Document rus
                useRestriction = dataAccessRequestAPI.createStructuredResearchPurpose(dar);
                dar.append("restriction", Document.parse(useRestriction.toString()));
                dar.append("translated_restriction", translateServiceAPI.translate(TranslateType.PURPOSE.getValue(), useRestriction));
            }
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "while creating useRestriction " + dar.toJson(), ex);
        }

        try {
            dar.append("sortDate",new Date());
            result = dataAccessRequestAPI.createDataAccessRequest(dar);
            uri = info.getRequestUriBuilder().path("{id}").build(result.get("_id"));
            matchProcessAPI.processMatchesForPurpose(result.get("_id").toString());
            try {
                emailApi.sendNewDARRequestMessage(result.getString("dar_code"));
            } catch(MessagingException | IOException | TemplateException ex){
                logger.log(Level.SEVERE, " Couldn't send email notification to CHAIRPERSON for new DAR request case id "+ result.getString("dar_code") + ". Error caused by:", ex);
            }
            return Response.created(uri).entity(result).build();
        }
         catch (Exception e) {
            dataAccessRequestAPI.deleteDataAccessRequest(result);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

    @PUT
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/{id}")
    public Response updateDataAccessRequest(@Context UriInfo info, Document dar, @PathParam("id") String id) {
        try {
            if(dar.containsKey("restriction")){
                dar.remove("restriction");
            }
            if (!requiresManualReview(dar)) {
                // generates research purpose, if needed, and store it on Document rus
                UseRestriction useRestriction = dataAccessRequestAPI.createStructuredResearchPurpose(dar);
                dar.append("restriction", Document.parse(useRestriction.toString()));
                dar.append("translated_restriction", translateServiceAPI.translate(TranslateType.PURPOSE.getValue(), useRestriction));
            }
            dar = dataAccessRequestAPI.updateDataAccessRequest(dar, id);
            matchProcessAPI.processMatchesForPurpose(dar.get("_id").toString());
            return Response.ok().entity(dataAccessRequestAPI.updateDataAccessRequest(dar, id)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }

    }

    @GET
    @Produces("application/json")
    @Path("/modalSummary/{id}")
    public DARModalDetailsDTO getDataAcessRequestModalSummary(@PathParam("id") String id){
        Document dar = dataAccessRequestAPI.describeDataAccessRequestById(id);
        return new DARModalDetailsDTO(dar);
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


    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id}")
    public Response delete(@PathParam("id") String id, @Context UriInfo info) {
        try {
            dataAccessRequestAPI.deleteDataAccessRequestById(id);
            matchProcessAPI.removeMatchesForPurpose(id);
            return Response.status(Response.Status.OK).entity("Research Purpose was deleted").build();
        } catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("/find/{id}")
    @Produces("application/json")
    public Document describeSpecificFields(@PathParam("id") String id, @QueryParam("fields") List<String> fields){
        if(CollectionUtils.isNotEmpty(fields)){
            List<String> fieldValues = Arrays.asList(fields.get(0).split(","));
            return dataAccessRequestAPI.describeDataAccessRequestFieldsById(id, fieldValues);
        }else {
            return dataAccessRequestAPI.describeDataAccessRequestById(id);
        }
    }

    @GET
    @Path("/find/{id}/consent")
    @Produces("application/json")
    public Consent describeConsentForDAR(@PathParam("id") String id){
        String datasetId = (dataAccessRequestAPI.describeDataAccessRequestFieldsById(id, Arrays.asList("datasetId"))).getString("datasetId");
        Consent c;
        if(datasetId != null){
            c = consentAPI.getConsentFromDatasetID(datasetId);
            if(c == null){
                throw new NotFoundException("Unable to find the consent related to the datasetId present in the DAR.");
            }
        } else {
            throw new NotFoundException("Unable to find the datasetId related to the DAR.");
        }
        return c;
    }


    @GET
    @Produces("application/json")
    @Path("/manage")
    public Response describeManageDataAccessRequests(@QueryParam("userId") Integer userId) {
        return Response.ok().entity(dataAccessRequestAPI.describeDataAccessRequestManage(userId)).build();
    }

    @GET
    @Path("cases/unreviewed")
    @Produces("application/json")
    public Response getTotalUnReviewedDAR(){
        return Response.ok("{\"darUnReviewedCases\":"+dataAccessRequestAPI.getTotalUnReviewedDAR()+"}").build();
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
