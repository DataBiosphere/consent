package org.broadinstitute.consent.http.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import freemarker.template.TemplateException;
import org.apache.commons.collections.CollectionUtils;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.DACUserRole;
import org.broadinstitute.consent.http.models.darsummary.DARModalDetailsDTO;
import org.broadinstitute.consent.http.models.dto.Error;
import org.broadinstitute.consent.http.models.grammar.UseRestriction;
import org.broadinstitute.consent.http.service.AbstractConsentAPI;
import org.broadinstitute.consent.http.service.AbstractDataAccessRequestAPI;
import org.broadinstitute.consent.http.service.AbstractDataSetAPI;
import org.broadinstitute.consent.http.service.AbstractEmailNotifierAPI;
import org.broadinstitute.consent.http.service.AbstractMatchProcessAPI;
import org.broadinstitute.consent.http.service.AbstractTranslateServiceAPI;
import org.broadinstitute.consent.http.service.ConsentAPI;
import org.broadinstitute.consent.http.service.DataAccessRequestAPI;
import org.broadinstitute.consent.http.service.DataSetAPI;
import org.broadinstitute.consent.http.service.ElectionAPI;
import org.broadinstitute.consent.http.service.EmailNotifierAPI;
import org.broadinstitute.consent.http.service.MatchProcessAPI;
import org.broadinstitute.consent.http.service.TranslateServiceAPI;
import org.broadinstitute.consent.http.service.users.DACUserAPI;
import org.broadinstitute.consent.http.service.validate.AbstractUseRestrictionValidatorAPI;
import org.broadinstitute.consent.http.service.validate.UseRestrictionValidatorAPI;
import org.broadinstitute.consent.http.util.DarConstants;
import org.bson.Document;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.mail.MessagingException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
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
    private final DataSetAPI dataSetAPI = AbstractDataSetAPI.getInstance();
    private static final Logger logger = Logger.getLogger(DataAccessRequestResource.class.getName());
    private final UseRestrictionValidatorAPI useRestrictionValidatorAPI;
    private final DACUserAPI dacUserAPI;
    private final ElectionAPI electionAPI;

    public DataAccessRequestResource(DACUserAPI dacUserAPI, ElectionAPI electionAPI) {
        this.dataAccessRequestAPI = AbstractDataAccessRequestAPI.getInstance();
        this.consentAPI = AbstractConsentAPI.getInstance();
        this.matchProcessAPI = AbstractMatchProcessAPI.getInstance();
        this.emailApi = AbstractEmailNotifierAPI.getInstance();
        this.useRestrictionValidatorAPI = AbstractUseRestrictionValidatorAPI.getInstance();
        this.dacUserAPI = dacUserAPI;
        this.electionAPI = electionAPI;
    }

    @POST
    @Consumes("application/json")
    @Produces("application/json")
    @RolesAllowed("RESEARCHER")
    public Response createdDataAccessRequest(@Context UriInfo info, Document dar) {
        URI uri;
        List<Document> result;
        UseRestriction useRestriction;
        try {
            Boolean needsManualReview = requiresManualReview(dar);
            if (!needsManualReview) {
                // generates research purpose, if needed, and store it on Document rus
                useRestriction = dataAccessRequestAPI.createStructuredResearchPurpose(dar);
                dar.append(DarConstants.RESTRICTION, Document.parse(useRestriction.toString()));
                useRestrictionValidatorAPI.validateUseRestriction(useRestriction.toString());
                dar.append(DarConstants.VALID_RESTRICTION, true);
            }
            dar.append(DarConstants.TRANSLATED_RESTRICTION, translateServiceAPI.generateStructuredTranslatedRestriction(dar, needsManualReview));

        }catch (IllegalArgumentException ex) {
            return Response.status(Response.Status.BAD_REQUEST).entity(ex.getMessage()).build();
        }
        catch (Exception ex) {
            logger.log(Level.SEVERE, "while creating useRestriction " + dar.toJson(), ex);
        }
        dar.append(DarConstants.SORT_DATE, new Date());
        result = dataAccessRequestAPI.createDataAccessRequest(dar);
        uri = info.getRequestUriBuilder().build();
        result.forEach(r -> {
            try {
                matchProcessAPI.processMatchesForPurpose(r.get(DarConstants.ID).toString());
                emailApi.sendNewDARRequestMessage(r.getString(DarConstants.DAR_CODE));
            } catch (Exception e) {
                logger.log(Level.SEVERE, " Couldn't send email notification to CHAIRPERSON for new DAR request case id " + r.getString(DarConstants.DAR_CODE) + ". Error caused by:", e);
            }
        });
        return Response.created(uri).build();

    }

    @PUT
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/{id}")
    @RolesAllowed("RESEARCHER")
    public Response updateDataAccessRequest(Document dar, @PathParam("id") String id) {
        try {
            if (dar.containsKey(DarConstants.RESTRICTION)) {
                dar.remove(DarConstants.RESTRICTION);
            }
            Boolean needsManualReview = requiresManualReview(dar);
            if (!needsManualReview) {
                // generates research purpose, if needed, and store it on Document rus
                UseRestriction useRestriction = dataAccessRequestAPI.createStructuredResearchPurpose(dar);
                dar.append(DarConstants.RESTRICTION, Document.parse(useRestriction.toString()));
            }
            dar.append(DarConstants.TRANSLATED_RESTRICTION, translateServiceAPI.generateStructuredTranslatedRestriction(dar, needsManualReview));
            dar = dataAccessRequestAPI.updateDataAccessRequest(dar, id);
            matchProcessAPI.processMatchesForPurpose(dar.get(DarConstants.ID).toString());
            return Response.ok().entity(dataAccessRequestAPI.updateDataAccessRequest(dar, id)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }

    }

    @GET
    @Produces("application/json")
    @Path("/modalSummary/{id}")
    @PermitAll
    public DARModalDetailsDTO getDataAcessRequestModalSummary(@PathParam("id") String id) {
        Document dar = dataAccessRequestAPI.describeDataAccessRequestById(id);
        Integer userId = obtainUserId(dar);
        DACUserRole role = dacUserAPI.getRoleStatus(userId);
        return new DARModalDetailsDTO(dar, dacUserAPI.describeDACUserById(dar.getInteger("userId")), electionAPI, role.getStatus(), role.getRationale());
    }

    @GET
    @Produces("application/json")
    @Path("/invalid")
    @RolesAllowed("ADMIN")
    public Response getInvalidDataAccessRequest() {
        try{
            return Response.status(Response.Status.OK).entity(dataAccessRequestAPI.getInvalidDataAccessRequest()).build();
        }catch (Exception e){
            return createExceptionResponse(e);
        }

    }

    @GET
    @Produces("application/json")
    @PermitAll
    public List<Document> describeDataAccessRequests() {
        return dataAccessRequestAPI.describeDataAccessRequests();
    }

    @GET
    @Path("/{id}")
    @Produces("application/json")
    @PermitAll
    public Document describe(@PathParam("id") String id) {
        return dataAccessRequestAPI.describeDataAccessRequestById(id);
    }


    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id}")
    @RolesAllowed({"RESEARCHER", "ADMIN"})
    public Response delete(@PathParam("id") String id) {
        try {
            dataAccessRequestAPI.deleteDataAccessRequestById(id);
            matchProcessAPI.removeMatchesForPurpose(id);
            return Response.status(Response.Status.OK).entity("Research Purpose was deleted").build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

    @GET
    @Path("/find/{id}")
    @Produces("application/json")
    @PermitAll
    public Document describeSpecificFields(@PathParam("id") String id, @QueryParam("fields") List<String> fields) {
        if (CollectionUtils.isNotEmpty(fields)) {
            List<String> fieldValues = Arrays.asList(fields.get(0).split(","));
            return dataAccessRequestAPI.describeDataAccessRequestFieldsById(id, fieldValues);
        } else {
            return dataAccessRequestAPI.describeDataAccessRequestById(id);
        }
    }

    @GET
    @Path("/find/{id}/consent")
    @Produces("application/json")
    @PermitAll
    public Consent describeConsentForDAR(@PathParam("id") String id) {
        List<String> datasetId = (dataAccessRequestAPI.describeDataAccessRequestFieldsById(id, Arrays.asList(DarConstants.DATASET_ID))).get("datasetId", List.class);
        Consent c;
        if (CollectionUtils.isNotEmpty(datasetId)) {
            c = consentAPI.getConsentFromDatasetID(datasetId.get(0));
            if (c == null) {
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
    @RolesAllowed({"RESEARCHER", "ADMIN"})
    public Response describeManageDataAccessRequests(@QueryParam("userId") Integer userId) {
        return Response.ok().entity(dataAccessRequestAPI.describeDataAccessRequestManage(userId)).build();
    }

    @GET
    @Path("cases/unreviewed")
    @Produces("application/json")
    @RolesAllowed("ADMIN")
    public Response getTotalUnReviewedDAR() {
        return Response.ok("{\"darUnReviewedCases\":" + dataAccessRequestAPI.getTotalUnReviewedDAR() + "}").build();
    }

    // Partial Data Access Requests Methods

    @GET
    @Produces("application/json")
    @Path("/partials")
    @RolesAllowed("RESEARCHER")
    public List<Document> describePartialDataAccessRequests() {
        return dataAccessRequestAPI.describePartialDataAccessRequests();
    }

    @POST
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/partial")
    @RolesAllowed("RESEARCHER")
    public Response createPartialDataAccessRequest(@Context UriInfo info, Document dar) {
        URI uri;
        Document result = null;
        if((dar.size() == 1 && dar.containsKey("userId")) || (dar.size() == 0)){
            return Response.status(Response.Status.BAD_REQUEST).entity(new Error("The Data Access Request is empty. Please, complete the form with the information you want to save.", Response.Status.BAD_REQUEST.getStatusCode())).build();
        }
        try {
            result = savePartialDarRequest(dar);
            uri = info.getRequestUriBuilder().path("{id}").build(result.get(DarConstants.ID));
            return Response.created(uri).entity(result).build();
        }
        catch (Exception e) {
            dataAccessRequestAPI.deleteDataAccessRequest(result);
            return createExceptionResponse(e);
        }
    }

    @POST
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/partial/datasetCatalog")
    @RolesAllowed("RESEARCHER")
    public Response createPartialDataAccessRequestFromCatalog(@QueryParam("userId") Integer userId, List<String> datasetIds) {
        Document dar = new Document();
        dar.append(DarConstants.USER_ID, userId);
        try {
            List<Map<String, String>> datasets = new ArrayList<>();
            for(String datasetId: datasetIds){
                List<Map<String, String>> ds = dataSetAPI.autoCompleteDataSets(datasetId);
                datasets.add(ds.get(0));
            }
            dar.append(DarConstants.DATASET_ID, datasets);
            return Response.ok().entity(dar).build();
        } catch (Exception e) {
            logger.log(Level.SEVERE, " while fetching dataset details to export to DAR formulary. UserId: " + userId + ", datasets: " + datasetIds.toString()+". Cause: "+ e.getLocalizedMessage());
            return createExceptionResponse(e);
        }
    }


    @PUT
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/partial")
    @RolesAllowed("RESEARCHER")
    public Response updatePartialDataAccessRequest(@Context UriInfo info, Document dar) {
        try {
            dar = dataAccessRequestAPI.updatePartialDataAccessRequest(dar);
            return Response.ok().entity(dar).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @GET
    @Produces("application/json")
    @Path("/partial/{id}")
    @RolesAllowed("RESEARCHER")
    public Document describePartialDar(@PathParam("id") String id) {
        return dataAccessRequestAPI.describePartialDataAccessRequestById(id);
    }


    @DELETE
    @Produces("application/json")
    @Path("/partial/{id}")
    @RolesAllowed("RESEARCHER")
    public Response deletePartialDar(@PathParam("id") String id, @Context UriInfo info) {
        try {
            dataAccessRequestAPI.deletePartialDataAccessRequestById(id);
            return Response.ok().build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

    @GET
    @Produces("application/json")
    @Path("/partials/manage")
    @RolesAllowed("RESEARCHER")
    public Response describePartialManageDataAccessRequests(@QueryParam("userId") Integer userId) {
        return Response.ok().entity(dataAccessRequestAPI.describePartialDataAccessRequestManage(userId)).build();
    }



    @PUT
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/cancel/{referenceId}")
    @RolesAllowed("RESEARCHER")
    public Response cancelDataAccessRequest(@PathParam("referenceId") String referenceId) {
        try {
            List<DACUser> usersToNotify = dataAccessRequestAPI.getUserEmailAndCancelElection(referenceId);
            Document dar = dataAccessRequestAPI.cancelDataAccessRequest(referenceId);
            if(CollectionUtils.isNotEmpty(usersToNotify)) {
                emailApi.sendCancelDARRequestMessage(usersToNotify, dar.getString(DarConstants.DAR_CODE));
            }
            return Response.ok().entity(dar).build();
        } catch (MessagingException | TemplateException | IOException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(new Error("The Data Access Request was cancelled but the DAC/Admin couldn't be notified. Contact Support. ", Response.Status.BAD_REQUEST.getStatusCode())).build();
        } catch (Exception e){
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new Error("Internal server error on delete. Please try again later. ", Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())).build();
        }
    }

    @GET
    @Produces("application/json")
    @Path("/hasUseRestriction/{referenceId}")
    @PermitAll
    public Response hasUseRestriction(@PathParam("referenceId") String referenceId){
        try{
            return Response.ok("{\"hasUseRestriction\":"+dataAccessRequestAPI.hasUseRestriction(referenceId)+"}").build();
        }catch (Exception e){
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new Error(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())).build();
        }
    }

    @PUT
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/restriction")
    @PermitAll
    public Response getUseRestrictionFromQuestions(Document dar) {
        try{
            Boolean needsManualReview = requiresManualReview(dar);
            if (!needsManualReview){
                UseRestriction useRestriction = dataAccessRequestAPI.createStructuredResearchPurpose(dar);
                dar.append(DarConstants.RESTRICTION, Document.parse(useRestriction.toString()));
                return Response.ok(useRestriction).build();
            }else{
                return Response.ok("{\"useRestriction\":\"Manual Review\"}").build();
            }
        }catch(Exception e){
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new Error(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())).build();
        }
    }

    private Map<String, Object> parseAsMap(String str) throws IOException {
        ObjectReader reader = mapper.readerFor(Map.class);
        return reader.readValue(str);
    }


    private Document savePartialDarRequest(Document dar) throws Exception{
        dar.append(DarConstants.SORT_DATE,new Date());
        return dataAccessRequestAPI.createPartialDataAccessRequest(dar);
    }

    private boolean requiresManualReview(Document dar) throws IOException {
        Map<String, Object> form = parseAsMap(dar.toJson());
        for (String field : fieldsForManualReview) {
            if (form.containsKey(field)) {
                if (Boolean.valueOf(form.get(field).toString())) {
                    return true;
                }
            }
        }
        return false;
    }

    private Integer obtainUserId (Document dar) {
        try{
            return dar.getInteger("userId");
        }catch (Exception e) {
            return Integer.valueOf(dar.getString("userId"));
        }
    }

// Fields that trigger manual review flag.
    String[] fieldsForManualReview = {
            "population",
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
}