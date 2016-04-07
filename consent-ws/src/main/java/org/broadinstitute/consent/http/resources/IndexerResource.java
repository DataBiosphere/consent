package org.broadinstitute.consent.http.resources;

import org.broadinstitute.consent.http.enumeration.OntologyTypes;
import org.broadinstitute.consent.http.models.ontology.StreamRec;
import org.broadinstitute.consent.http.service.ontologyIndexer.IndexerService;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;

import org.broadinstitute.consent.http.models.dto.Error;

/**
 * Created by SantiagoSaucedo on 3/11/2016.
 */
@Path("ontology/")
public class IndexerResource {

    private final IndexerService indexerService;
    private final IndexerHelper elasticSearchHelper = new IndexerHelper();

    public IndexerResource(IndexerService indexerService) {
        this.indexerService = indexerService;
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces("application/json")
    public Response saveAndIndex( FormDataMultiPart formParams)   {
        try {
            List<StreamRec> fileCompList =  elasticSearchHelper.filesCompBuilder(formParams);
            return indexerService.saveAndIndex(fileCompList);
        }catch (IOException | InternalServerErrorException e){
            return Response.serverError().entity(new Error(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())).build();
        }catch (BadRequestException e){
            return Response.status(Response.Status.BAD_REQUEST).entity(new Error(e.getMessage(), Response.Status.BAD_REQUEST.getStatusCode())).build();
        }
    }


    @GET
    @Produces("application/json")
    public Response getIndexedFiles(){
        try {
            indexerService.getIndexedFiles();
           return Response.ok().entity(indexerService.getIndexedFiles()).build();
        } catch(Exception e){
            return Response.serverError().build();
        }
    }

    @GET
    @Produces("application/json")
    @Path("types")
    public Response getOntologyTypes(){
       return  Response.ok().entity(OntologyTypes.values()).build();
    }

    @PUT
    @Produces("application/json")
    public Response deleteIndexedFile(String fileURL) {
        try {
           return indexerService.deleteOntologiesByType(fileURL);
        }catch (Exception e){
            return Response.serverError().entity(new Error(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())).build();
        }
    }
 }

