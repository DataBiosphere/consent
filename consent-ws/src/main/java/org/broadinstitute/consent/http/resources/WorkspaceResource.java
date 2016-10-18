package org.broadinstitute.consent.http.resources;

import org.apache.commons.collections.CollectionUtils;
import org.broadinstitute.consent.http.enumeration.AssociationType;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.dto.ElectionStatusDTO;
import org.broadinstitute.consent.http.models.dto.WorkspaceAssociationDTO;
import org.broadinstitute.consent.http.service.AbstractConsentAPI;
import org.broadinstitute.consent.http.service.AbstractDataAccessRequestAPI;
import org.broadinstitute.consent.http.service.AbstractDataSetAPI;
import org.broadinstitute.consent.http.service.AbstractElectionAPI;
import org.broadinstitute.consent.http.service.ConsentAPI;
import org.broadinstitute.consent.http.service.DataAccessRequestAPI;
import org.broadinstitute.consent.http.service.DataSetAPI;
import org.broadinstitute.consent.http.service.ElectionAPI;
import org.bson.Document;

import javax.annotation.security.PermitAll;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
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
                if(CollectionUtils.isNotEmpty(darList)) {
                    workspaceAssociationDTO.setDataAccessRequests(darList);
                    workspaceAssociationDTO.getElectionStatus().addAll(electionAPI.describeElectionByDARs(darList));
                }
            }
            return Response.ok().entity(workspaceAssociationDTO).build();
        }catch (Exception e){
            return createExceptionResponse(e);
        }
    }

}
