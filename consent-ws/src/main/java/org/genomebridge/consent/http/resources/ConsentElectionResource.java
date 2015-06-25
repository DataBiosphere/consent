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
import javax.ws.rs.core.UriInfo;

import org.genomebridge.consent.http.models.Election;
import org.genomebridge.consent.http.service.AbstractElectionAPI;
import org.genomebridge.consent.http.service.ElectionAPI;

import com.sun.jersey.api.NotFoundException;

@Path("consent/{consentId}/election")
public class ConsentElectionResource extends Resource {

	private ElectionAPI api;

	public ConsentElectionResource() {
		this.api = AbstractElectionAPI.getInstance();
	}

	@POST
	@Consumes("application/json")
	public Response createConsentElection(@Context UriInfo info, Election rec,
			@PathParam("consentId") String consentId) {
		URI uri = null;
		try {
			api.createElection(rec, consentId,true);
			uri = info.getRequestUriBuilder().build();
		} catch (IllegalArgumentException e) {
			return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
		}
		return Response.created(uri).build();
	}

	@PUT
	@Consumes("application/json")
	@Produces("application/json")
	@Path("/{id}")
	public Response updateConsentElection(@Context UriInfo info, Election rec,
			@PathParam("consentId") String consentId,@PathParam("id") Integer id) {
		try {
			Election election = api.updateElectionById(rec, id);
			URI uri = info.getRequestUriBuilder().build(ConsentElectionResource.class);
	        return Response.ok(election).location(uri).build();
		}catch (IllegalArgumentException e) {
			return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
		}
	}

	@GET
	@Produces("application/json")
	public Election describe(@PathParam("consentId") String consentId) {
		try {
			return api.describeConsentElection(consentId);
		} catch (Exception e) {
			throw new NotFoundException("Invalid id:"+consentId);
		}
	}

	@DELETE
	@Produces(MediaType.APPLICATION_JSON)
	public Response deleteElection(@PathParam("consentId") String consentId,@Context UriInfo info) {
		try {
			api.deleteElection(consentId);
			return  Response.status(Response.Status.OK).entity("Election was deleted").build();
		} catch (Exception e) { 
			throw new NotFoundException(e.getMessage());
		}
	}
	
	

}
