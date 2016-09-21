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
    @Path("/{objectId}")
    @Consumes("application/json")
    @Produces("application/json")
    @RolesAllowed("ADMIN")
    public Response associateDatasetWithUsers(@PathParam("objectId") String objectId, List<Integer> usersIdList) {
        try {
            return  Response.status(Response.Status.CREATED).entity(api.createDatasetUsersAssociation(objectId, usersIdList)).build() ;
        } catch (Exception e){
            return createExceptionResponse(e);
        }
    }

    @GET
    @Path("/{objectId}")
    @Consumes("application/json")
    @Produces("application/json")
    @PermitAll
    public Response getDatasetAssociations(@PathParam("objectId") String objectId) {
        try {
            return Response.ok(api.findDataOwnersRelationWithDataset(objectId)).build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

    @PUT
    @Path("/{objectId}")
    @Consumes("application/json")
    @Produces("application/json")
    @RolesAllowed("ADMIN")
    public Response updateDatasetAssociations(@PathParam("objectId") String objectId, List<Integer> usersIdList) {
        try {
            return  Response.ok(api.updateDatasetAssociations(objectId, usersIdList)).build() ;
        } catch (Exception e){
            return createExceptionResponse(e);
        }
    }
}
