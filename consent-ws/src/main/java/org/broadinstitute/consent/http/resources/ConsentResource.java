package org.broadinstitute.consent.http.resources;

import org.broadinstitute.consent.http.enumeration.TranslateType;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.dto.Error;
import org.broadinstitute.consent.http.service.*;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.net.URI;

@Path("{api : (api/)?}consent")
public class ConsentResource extends Resource {

    private final ConsentAPI api;
    private final MatchProcessAPI matchProcessAPI;
    private final MatchAPI matchAPI;
    private final TranslateServiceAPI translateServiceAPI = AbstractTranslateServiceAPI.getInstance();


    @Path("{id}")
    @GET
    @Produces("application/json")
    public Response describe(@PathParam("id") String id) {
        try {
            return Response.ok(populateFromApi(id))
                    .build();
        } catch (UnknownIdentifierException e) {
            return Response.status(Response.Status.NOT_FOUND).entity(new Error(String.format("Could not find consent with id %s", id), Response.Status.NOT_FOUND.getStatusCode())).build();
        }
    }

    @POST
    @Consumes("application/json")
    public Response createConsent(@Context UriInfo info, Consent rec) {
        try {
            if (rec.getTranslatedUseRestriction() == null) {
                rec.setTranslatedUseRestriction(translateServiceAPI.translate(TranslateType.SAMPLESET.getValue(),rec.getUseRestriction()));
            }
            Consent consent = api.create(rec);
            URI uri = info.getRequestUriBuilder().path("{id}").build(consent.consentId);
            matchProcessAPI.processMatchesForConsent(consent.consentId);
            return Response.created(uri).build();
        }  catch (IOException ie) {
            return Response.status(Response.Status.BAD_REQUEST).entity(new Error(ie.getMessage(), Response.Status.BAD_REQUEST.getStatusCode())).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(new Error(e.getMessage(), Response.Status.BAD_REQUEST.getStatusCode())).build();
        }
        catch (Exception e){
            return Response.serverError().entity(new Error(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())).build();
        }
    }

    @Path("{id}")
    @PUT
    @Consumes("application/json")
    @Produces("application/json")
    public Response update(@PathParam("id") String id, Consent updated) {
        try {
            if (updated.getTranslatedUseRestriction() == null) {
                updated.setTranslatedUseRestriction(translateServiceAPI.translate(TranslateType.SAMPLESET.getValue(),updated.getUseRestriction()));
            }
            updated = api.update(id, updated);
            matchProcessAPI.processMatchesForConsent(id);
            return Response.ok(updated).build();
        } catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).entity(new Error(String.format("Could not find consent with id %s to update", id), Response.Status.NOT_FOUND.getStatusCode())).build();
        } catch (IOException ie) {
            return Response.status(Response.Status.BAD_REQUEST).entity(new Error(ie.getMessage(), Response.Status.BAD_REQUEST.getStatusCode())).build();
        } catch (Exception e) {
            return Response.serverError().entity(new Error(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())).build();
        }
    }


    @DELETE
    @Produces("application/json")
    @Path("{id}")
    public Response delete(@PathParam("id") String consentId) {
        try {
            api.delete(consentId);
            return Response.ok().build();
        }catch (NotFoundException e){
            return Response.status(Response.Status.NOT_FOUND).entity(new Error(e.getMessage(), Response.Status.NOT_FOUND.getStatusCode())).build();
        }catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(new Error(e.getMessage(), Response.Status.BAD_REQUEST.getStatusCode())).build();
        } catch (Exception e) {
            return Response.serverError().entity(new Error(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())).build();
      }
    }


    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id}/matches")
    public Response getMatches(@PathParam("id") String purposeId, @Context UriInfo info) {
        try {
            return Response.status(Response.Status.OK).entity(matchAPI.findMatchByConsentId(purposeId)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.NOT_FOUND).entity(new Error(e.getMessage(), Response.Status.NOT_FOUND.getStatusCode())).build();
        }
    }


    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getByName(@QueryParam("name") String name, @Context UriInfo info) {
        
        String id;
        try {
            id = api.getByName(name);
            if (id == null) {
                return Response.status(Response.Status.NOT_FOUND).entity(new Error(String.format("Requested name = %s not found on consents", name), Response.Status.NOT_FOUND.getStatusCode())).build();
            }
            return Response.status(Response.Status.OK).entity(id).build();            
        } catch(Exception ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new Error(ex.getMessage(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())).build();
        }
    }
    

   private Consent populateFromApi(String id) throws UnknownIdentifierException {
      return api.retrieve(id);
   }


    public ConsentResource() {
        this.api = AbstractConsentAPI.getInstance();
        this.matchProcessAPI = AbstractMatchProcessAPI.getInstance();
        this.matchAPI = AbstractMatchAPI.getInstance();
    }

}
