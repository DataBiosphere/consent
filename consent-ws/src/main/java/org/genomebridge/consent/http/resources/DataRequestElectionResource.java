package org.genomebridge.consent.http.resources;

import java.net.URI;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.genomebridge.consent.http.models.Election;
import org.genomebridge.consent.http.service.AbstractElectionAPI;
import org.genomebridge.consent.http.service.ElectionAPI;

import com.sun.jersey.api.NotFoundException;

@Path("dataRequest/{requestId}/election")
public class DataRequestElectionResource extends Resource {

	private ElectionAPI api;

	public DataRequestElectionResource() {
		this.api = AbstractElectionAPI.getInstance();
	}

	@POST
	@Consumes("application/json")
	public Response createDataRequestElection(@Context UriInfo info, Election rec,
			@PathParam("requestId") Integer requestId) {
		URI uri = null;
		Election election = null;
			try {
				election = api.createElection(rec, requestId.toString(),false);
				uri = info.getRequestUriBuilder().build();
			} catch (IllegalArgumentException e) {
				return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
			}
	
		return Response.created(uri).entity(election).build();
	}

	@PUT
	@Consumes("application/json")
	@Produces("application/json")
	@Path("/{id}")
	public Response updateDataRequestElection(@Context UriInfo info, Election rec,
			@PathParam("requestId") Integer requestId,@PathParam("id") Integer id) {
		try {
			Election election = api.updateElectionById(rec, id);
			URI assocURI = buildElectionURI(requestId);
	        return Response.ok(election).location(assocURI).build();
		} catch (IllegalArgumentException e) {
			return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
		}
	}

	@GET
	@Produces("application/json")
	public Election describe(@PathParam("requestId") Integer requestId) {
		try {
			return api.describeDataRequestElection(requestId);
		} catch (Exception e) {
			throw new NotFoundException(e.getMessage());
		}
	}

	@DELETE
	@Produces(MediaType.APPLICATION_JSON)
	public Response deleteElection(@PathParam("requestId") Integer requestId,@Context UriInfo info) {
		try {
			api.deleteElection(requestId.toString());
			return  Response.status(Response.Status.OK).entity("Election was deleted").build();
		} catch (Exception e) { 
			throw new NotFoundException(e.getMessage());
		}
	}
	
	 private URI buildElectionURI(Integer id) {
	    return UriBuilder.fromResource(DataRequestElectionResource.class).build(id);
	 }

}
