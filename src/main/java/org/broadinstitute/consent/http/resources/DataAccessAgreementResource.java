package org.broadinstitute.consent.http.resources;

import com.google.api.client.http.HttpResponse;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.cloudstore.GCSStore;
import org.broadinstitute.consent.http.enumeration.ResearcherFields;
import org.broadinstitute.consent.http.service.users.handler.ResearcherAPI;
import org.bson.Document;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataParam;


import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.InputStream;
import java.util.Map;
import java.util.UUID;

@Path("{api : (api/)?}dar")
public class DataAccessAgreementResource extends Resource {

    private final GCSStore store;
    private final ResearcherAPI researcherAPI;

    public DataAccessAgreementResource(GCSStore store, ResearcherAPI researcherAPI) {
        this.store = store;
        this.researcherAPI = researcherAPI;
    }

    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @RolesAllowed({"ADMIN", "RESEARCHER"})
    @Path("/downloadDAA/{researcherId}")
    public Response getDAA(@PathParam("researcherId") Integer researcherId) {
        Map<String, String> researcherProperties = researcherAPI.describeResearcherPropertiesForDAR(researcherId);
        if (researcherProperties.containsKey(ResearcherFields.URL_DAA.getValue())) {
            try {
                HttpResponse r = store.getStorageDocument(researcherProperties.get(ResearcherFields.URL_DAA.getValue()));
                File targetFile = new File(researcherProperties.get(ResearcherFields.NAME_DAA.getValue()));
                FileUtils.copyInputStreamToFile(r.getContent(), targetFile);
                return Response.ok(targetFile)
                        .type(r.getContentType())
                        .header("Content-Disposition", "attachment; filename=" + targetFile.getName())
                        .build();
            } catch (Exception e) {
                return createExceptionResponse(e);
            }
        } else {
            return createExceptionResponse(new NotFoundException());
        }
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/storeDAA")
    @RolesAllowed({"RESEARCHER", "ADMIN"})
    public Response storeDAA (
            @FormDataParam("data") InputStream uploadedDAA,
            @FormDataParam("data") FormDataBodyPart part,
            @QueryParam("fileName") String fileName,
            @QueryParam("existentFileUrl") String existentFileUrl) {
        try {
            if(StringUtils.isNotEmpty(existentFileUrl)) {
                store.deleteStorageDocument(existentFileUrl);
            }
            String toStoreFileName =  UUID.randomUUID() + "." + FilenameUtils.getExtension(fileName);
            Document dataAccessAgreement = new Document();
            dataAccessAgreement.put(ResearcherFields.URL_DAA.getValue(), store.postStorageDocument(uploadedDAA, part.getMediaType().toString(), toStoreFileName));
            dataAccessAgreement.put(ResearcherFields.NAME_DAA.getValue(), fileName);
            return Response.ok(dataAccessAgreement).build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }

    }


}
