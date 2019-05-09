package org.broadinstitute.consent.http.resources;

import org.broadinstitute.consent.http.service.AbstractDataSetAssociationAPI;
import org.broadinstitute.consent.http.service.DataSetAssociationAPI;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("{api : (api/)?}datasetAssociation")
public class DataSetAssociationsResource extends Resource {


    private final DataSetAssociationAPI api;
    public DataSetAssociationsResource() {
        this.api = AbstractDataSetAssociationAPI.getInstance();
    }


    @POST
    @Path("/{dataSetId}")
    @Consumes("application/json")
    @Produces("application/json")
    @RolesAllowed(ADMIN)
    public Response associateDatasetWithUsers(@PathParam("dataSetId") Integer dataSetId, List<Integer> usersIdList) {
        try {
            return  Response.status(Response.Status.CREATED).entity(api.createDatasetUsersAssociation(dataSetId, usersIdList)).build() ;
        } catch (Exception e){
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
            return Response.ok(api.findDataOwnersRelationWithDataset(dataSetId)).build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

    @PUT
    @Path("/{dataSetId}")
    @Consumes("application/json")
    @Produces("application/json")
    @RolesAllowed(ADMIN)
    public Response updateDatasetAssociations(@PathParam("dataSetId") Integer dataSetId, List<Integer> usersIdList) {
        try {
            return  Response.ok(api.updateDatasetAssociations(dataSetId, usersIdList)).build() ;
        } catch (Exception e){
            return createExceptionResponse(e);
        }
    }
}
