package org.broadinstitute.consent.http.resources;


import org.broadinstitute.consent.http.models.dto.Error;
import org.broadinstitute.consent.http.service.*;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.*;

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
    public Response associateDatasetWithUsers(@PathParam("objectId") String objectId, List<Integer> usersIdList) {
        try {
            return  Response.status(Response.Status.CREATED).entity(api.createDatasetUsersAssociation(objectId, usersIdList)).build() ;
        } catch (BadRequestException e){
            return Response.status(Response.Status.BAD_REQUEST).entity(new Error(e.getMessage(), Response.Status.BAD_REQUEST.getStatusCode())).build();
        } catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).entity(new Error(e.getMessage(), Response.Status.NOT_FOUND.getStatusCode())).build();
        } catch (Exception e){
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new Error(null, Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())).build();
        }
    }

    @GET
    @Path("/{objectId}")
    @Consumes("application/json")
    @Produces("application/json")
    public Response getDatasetAssociations(@PathParam("objectId") String objectId) {
        try {
            return Response.ok(api.findDataOwnersRelationWithDataset(objectId)).build();
        } catch (NotFoundException e){
            //change null value* pending merge to dev
            return Response.status(Response.Status.NOT_FOUND).entity(new Error(e.getMessage(), Response.Status.BAD_REQUEST.getStatusCode())).build();
        }catch (Exception e){
            //change null value* pending merge to dev
            return Response.serverError().entity(new Error(null, Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())).build();
        }
    }

    @PUT
    @Path("/{objectId}")
    @Consumes("application/json")
    @Produces("application/json")
    public Response updateDatasetAssociations(@PathParam("objectId") String objectId, List<Integer> usersIdList) {
        try {
            return  Response.ok(api.updateDatasetAssociations(objectId, usersIdList)).build() ;
        } catch (NotFoundException e){
            return Response.status(Response.Status.NOT_FOUND).entity(new Error(e.getMessage(), Response.Status.NOT_FOUND.getStatusCode())).build();
        } catch (BadRequestException e){
            return Response.status(Response.Status.BAD_REQUEST).entity(new Error(e.getMessage(), Response.Status.BAD_REQUEST.getStatusCode())).build();
        } catch (Exception e){
            //change null value* pending merge to dev
            return Response.serverError().entity(new Error(null, Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())).build();
        }
    }
}
