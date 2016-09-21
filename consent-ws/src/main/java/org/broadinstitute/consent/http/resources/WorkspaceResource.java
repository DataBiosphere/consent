package org.broadinstitute.consent.http.resources;

import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;
import org.broadinstitute.consent.http.enumeration.AssociationType;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.ConsentAssociation;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.dto.ElectionStatusDTO;
import org.broadinstitute.consent.http.models.dto.WorkspaceAssociationDTO;
import org.broadinstitute.consent.http.service.*;
import org.bson.Document;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Path("{api : (api/)?}workspace/{workspaceId}")
public class WorkspaceResource extends Resource{

    private final ConsentAPI api;
    private final DataSetAPI dataSetAPI;
    private final DataAccessRequestAPI dataAccessRequestAPI;
    private final ElectionAPI electionAPI;

    public WorkspaceResource() {
        this.api = AbstractConsentAPI.getInstance();
        this.dataSetAPI = AbstractDataSetAPI.getInstance();
        this.dataAccessRequestAPI = AbstractDataAccessRequestAPI.getInstance();
        this.electionAPI = AbstractElectionAPI.getInstance();
    }

    @POST
    @Produces("application/json")
    @Path("/{consentId}")
    @RolesAllowed({"RESEARCHER", "DATAOWNER"})
    public Response createAssociation(@PathParam("workspaceId") String workspaceId, @PathParam("consentId") String consentId) {
        try {
            List<ConsentAssociation> association = Arrays.asList(new ConsentAssociation(AssociationType.WORKSPACE.getValue(), Arrays.asList(workspaceId)));
            logger().debug(String.format("POSTing association to id '%s' with body '%s'", consentId, association.toString()));
            List<ConsentAssociation> result = api.createAssociation(consentId, association);
            URI assocURI = buildConsentAssociationURI(consentId);
            return Response.ok(result).location(assocURI).build();
        }catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

    @GET
    @Produces("application/json")
    @PermitAll
    public Response getWorkspaceAssociation(@PathParam("workspaceId") String workspaceId){
        try{
            Consent consent = api.getConsentFromObjectIdAndType(workspaceId, AssociationType.WORKSPACE.getValue());
            List<ElectionStatusDTO> electionsDTO = electionAPI.describeElectionsByConsentId(consent.getConsentId());
            List<DataSet> dataSets = dataSetAPI.getDataSetsForConsent(consent.getConsentId());
            WorkspaceAssociationDTO workspaceAssociationDTO = new WorkspaceAssociationDTO(consent, electionsDTO);
            if(CollectionUtils.isNotEmpty(dataSets)){
                List<String> dataSetIds = dataSets.stream().map(DataSet::getObjectId).collect(Collectors.toList());
                List<Document> darList = dataAccessRequestAPI.describeDataAccessWithDataSetId(dataSetIds);
                if(CollectionUtils.isNotEmpty(darList)) workspaceAssociationDTO.setDataAccessRequests(darList);
            }
            return Response.ok().entity(workspaceAssociationDTO).build();
        }catch (Exception e){
            return createExceptionResponse(e);
        }
    }

    private URI buildConsentAssociationURI(String id) {
        return UriBuilder.fromResource(ConsentAssociationResource.class).build("api/", id);
    }

    @Override
    protected Logger logger() {
        return Logger.getLogger("WorkspaceResource");
    }
}
