package org.broadinstitute.consent.http.resources;

import com.google.api.client.http.HttpResponse;
import org.apache.commons.io.FileUtils;
import org.broadinstitute.consent.http.cloudstore.GCSStore;
import org.broadinstitute.consent.http.enumeration.OntologyTypes;
import org.broadinstitute.consent.http.models.ontology.StreamRec;
import org.broadinstitute.consent.http.service.ontology.IndexerService;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.net.URLDecoder;
import java.util.List;

/**
 * Created by SantiagoSaucedo on 3/11/2016.
 */
@Path("{api : (api/)?}ontology/")
public class IndexerResource extends Resource {

    private final IndexerService indexerService;
    private final IndexerHelper elasticSearchHelper = new IndexerHelper();
    private final GCSStore store;

    public IndexerResource(IndexerService indexerService, GCSStore store) {
        this.indexerService = indexerService;
        this.store = store;
    }

    @GET
    @Produces("application/json")
    @RolesAllowed("ADMIN")
    public Response getIndexedFiles() {
        try {
            indexerService.getIndexedFiles();
            return Response.ok().entity(indexerService.getIndexedFiles()).build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces("application/json")
    @RolesAllowed("ADMIN")
    public Response saveAndIndex(FormDataMultiPart formParams) {
        try {
            List<StreamRec> fileCompList = elasticSearchHelper.filesCompBuilder(formParams);
            return indexerService.saveAndIndex(fileCompList);
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

    @PUT
    @Produces("application/json")
    @RolesAllowed("ADMIN")
    public Response deleteIndexedFile(String fileURL) {
        if (fileURL == null || fileURL.isEmpty()) {
            return createExceptionResponse(new BadRequestException("Query Parameter 'fileURL' cannot be empty."));
        }
        try {
            String url = URLDecoder.decode(fileURL, "UTF-8");
            return indexerService.deleteOntologiesByType(url);
        } catch (Exception e) {
            return createExceptionResponse(e);
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
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

    @GET
    @Produces("application/json")
    @Path("types")
    @RolesAllowed("ADMIN")
    public Response getOntologyTypes() {
        return Response.ok().entity(OntologyTypes.values()).build();
    }

}