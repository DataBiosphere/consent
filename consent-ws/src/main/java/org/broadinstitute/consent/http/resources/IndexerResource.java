package org.broadinstitute.consent.http.resources;

import com.google.api.client.http.HttpResponse;
import org.apache.commons.io.FileUtils;
import org.broadinstitute.consent.http.cloudstore.GCSStore;
import org.broadinstitute.consent.http.enumeration.OntologyTypes;
import org.broadinstitute.consent.http.models.ontology.StreamRec;
import org.broadinstitute.consent.http.service.ontologyIndexer.IndexerService;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.List;

import org.broadinstitute.consent.http.models.dto.Error;

/**
 * Created by SantiagoSaucedo on 3/11/2016.
 */
@Path("ontology/")
public class IndexerResource {

    private final IndexerService indexerService;
    private final IndexerHelper elasticSearchHelper = new IndexerHelper();
    private final GCSStore store;

    public IndexerResource(IndexerService indexerService, GCSStore store) {
        this.indexerService = indexerService;
        this.store = store;
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces("application/json")
    @RolesAllowed("ADMIN")
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
    @RolesAllowed("ADMIN")
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
    @PermitAll
    public Response getOntologyTypes(){
       return  Response.ok().entity(OntologyTypes.values()).build();
    }

    @PUT
    @Produces("application/json")
    @RolesAllowed("ADMIN")
    public Response deleteIndexedFile(String fileURL) {
        try {
           return indexerService.deleteOntologiesByType(fileURL);
        }catch (Exception e){
            return Response.serverError().entity(new Error(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())).build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Path("file")
    @PermitAll
    public Response getFile(@QueryParam("fileUrl") String fileUrl, @QueryParam("fileName") String fileName) {
        try {
            String url = URLDecoder.decode(fileUrl, "UTF-8");
            HttpResponse r = store.getStorageDocument(url);
            File targetFile = new File(fileName);
            FileUtils.copyInputStreamToFile(r.getContent(), targetFile);
            return Response.ok(targetFile)
                    .type(r.getContentType())
                    .header("Content-Disposition", "attachment; filename=" + targetFile.getName())
                    .build();
        } catch (IOException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(new Error(e.getMessage(), Response.Status.BAD_REQUEST.getStatusCode())).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new Error(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())).build();
        }
    }
 }

