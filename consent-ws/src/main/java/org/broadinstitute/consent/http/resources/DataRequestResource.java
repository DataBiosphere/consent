package org.broadinstitute.consent.http.resources;

import org.broadinstitute.consent.http.models.DataRequest;
import org.broadinstitute.consent.http.models.dto.DefaultErrorMessage;
import org.broadinstitute.consent.http.models.dto.Error;
import org.broadinstitute.consent.http.service.AbstractDataRequestAPI;
import org.broadinstitute.consent.http.service.DataRequestAPI;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.Status;
import java.net.URI;

@Path("{api : (api/)?}dataRequest")
public class DataRequestResource extends Resource {

    private final DataRequestAPI api;

    public DataRequestResource() {
        this.api = AbstractDataRequestAPI.getInstance();
    }

    @POST
    @Consumes("application/json")
    public Response createDataRequest(@Context UriInfo info, DataRequest rec) {
        URI uri;
        DataRequest dataRequest;
        try {
            dataRequest = api.createDataRequest(rec);
            uri = info.getRequestUriBuilder().path("{id}").build(dataRequest.getRequestId());
            return Response.created(uri).entity(dataRequest).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Status.BAD_REQUEST).entity(new Error(e.getMessage(), Status.BAD_REQUEST.getStatusCode())).build();
        }catch(Exception e){
            return Response.serverError().entity(new Error( DefaultErrorMessage.INTERNAL_SERVER_ERROR.getMessage() , Status.INTERNAL_SERVER_ERROR.getStatusCode())).build();
        }
    }

    @PUT
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/{id}")
    public Response updateDataRequest(@Context UriInfo info, DataRequest rec, @PathParam("id") Integer id) {
        try {
            DataRequest dataRequest = api.updateDataRequestById(rec, id);
            URI assocURI = buildDataRequestURI(id);
            return Response.ok(dataRequest).location(assocURI).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Status.BAD_REQUEST).entity(new Error(e.getMessage(), Status.BAD_REQUEST.getStatusCode())).build();
        } catch( NotFoundException e){
            return Response.status(Response.Status.NOT_FOUND).entity(new Error( e.getMessage(), Response.Status.NOT_FOUND.getStatusCode())).build();
        } catch (Exception e){
            return Response.serverError().entity(new Error(DefaultErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())).build();
        }
    }

    @GET
    @Produces("application/json")
    @Path("/{id}")
    public Response describe(@PathParam("id") Integer requestId) {
        try {
            return Response.ok(api.describeDataRequest(requestId)).build();
        } catch (NotFoundException e){
            return Response.status(Response.Status.NOT_FOUND).entity(new Error( e.getMessage(), Response.Status.NOT_FOUND.getStatusCode())).build();
        } catch (Exception e){
            return Response.serverError().entity(new Error(DefaultErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())).build();
        }
      }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id}")
    public Response deleteDataRequest(@PathParam("id") Integer requestId, @Context UriInfo info) {
        try{
        api.deleteDataRequest(requestId);
        return Response.ok().entity("DataRequest was deleted").build();
        } catch(NotFoundException e){
            return Response.status(Response.Status.NOT_FOUND).entity(new Error( e.getMessage(), Response.Status.NOT_FOUND.getStatusCode())).build();
        } catch (Exception e){
            return Response.serverError().entity(new Error(DefaultErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())).build();
        }
    }

    private URI buildDataRequestURI(Integer id) {
        return UriBuilder.fromResource(DataRequestResource.class).build(id);
    }

}
