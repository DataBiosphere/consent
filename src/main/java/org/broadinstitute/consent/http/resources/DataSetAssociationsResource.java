package org.broadinstitute.consent.http.resources;

import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.DatasetAssociation;
import org.broadinstitute.consent.http.service.AbstractDataSetAssociationAPI;
import org.broadinstitute.consent.http.service.DataSetAssociationAPI;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Path("api/datasetAssociation")
public class DataSetAssociationsResource extends Resource {


    private final DataSetAssociationAPI api;

    DataSetAssociationsResource() {
        this.api = AbstractDataSetAssociationAPI.getInstance();
    }


    @POST
    @Path("/{datasetId}")
    @Consumes("application/json")
    @Produces("application/json")
    @RolesAllowed(ADMIN)
    public Response associateDatasetWithUsers(@PathParam("datasetId") Integer datasetId, List<Integer> userIdList) {
        try {
            List<DatasetAssociation> associations = api.createDatasetUsersAssociation(datasetId, userIdList);
            return Response.status(Response.Status.CREATED).entity(associations).build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

    @GET
    @Path("/{dataSetId}")
    @Consumes("application/json")
    @Produces("application/json")
    @PermitAll
    public Response getDatasetAssociations(@PathParam("dataSetId") Integer dataSetId) {
        try {
            Map<String, Collection<DACUser>> userMap = api.findDataOwnersRelationWithDataset(dataSetId);
            return Response.ok().entity(userMap).build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

    @PUT
    @Path("/{dataSetId}")
    @Consumes("application/json")
    @Produces("application/json")
    @RolesAllowed(ADMIN)
    public Response updateDatasetAssociations(@PathParam("dataSetId") Integer dataSetId, List<Integer> userIdList) {
        try {
            List<DatasetAssociation> associations = api.updateDatasetAssociations(dataSetId, userIdList);
            return Response.ok().entity(associations).build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }
}
