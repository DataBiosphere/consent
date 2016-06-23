package org.broadinstitute.consent.http.resources;

import com.google.api.client.http.HttpResponse;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.broadinstitute.consent.http.cloudstore.GCSStore;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.service.AbstractConsentAPI;
import org.broadinstitute.consent.http.service.ConsentAPI;
import org.broadinstitute.consent.http.service.UnknownIdentifierException;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataParam;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.UUID;

@Path("{auth: (basic/|api/)?}consent/{id}/dul")
public class DataUseLetterResource extends Resource {

    private final ConsentAPI api;
    private final GCSStore store;

    public DataUseLetterResource(GCSStore store) {
        this.api = AbstractConsentAPI.getInstance();
        this.store = store;
    }

    private String getFileExtension(String fileName) {
        return FilenameUtils.getExtension(fileName);
    }

    private void deletePreviousStorageFile(String consentId) throws IOException, GeneralSecurityException, UnknownIdentifierException {
        String prevFile = api.getConsentDulUrl(consentId);
        if (StringUtils.isNotBlank(prevFile)) {
            store.deleteStorageDocument(prevFile);
        }
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("ADMIN")
    public Consent createDUL(
            @FormDataParam("data") InputStream uploadedDUL,
            @FormDataParam("data") FormDataBodyPart part,
            @PathParam("id") String consentId,
            @QueryParam("fileName") String fileName) {
        String msg = String.format("POSTing Data Use Letter to consent with id '%s'", consentId);
        logger().debug(msg);

        try {
            deletePreviousStorageFile(consentId);
            String toStoreFileName =  UUID.randomUUID() + "." + getFileExtension(part.getContentDisposition().getFileName());
            String dulUrl = store.postStorageDocument(uploadedDUL, part.getMediaType().toString(), toStoreFileName);
            return api.updateConsentDul(consentId, dulUrl, fileName);
        } catch (UnknownIdentifierException e) {
            throw new NotFoundException(String.format("Could not find consent with id %s", consentId));
        } catch (IOException e) {
            logger().error("Error when trying to read/write the uploaded file " + e.getMessage());
        } catch (GeneralSecurityException e) {
            logger().error("Security error: " + e.getMessage());
        }
        return null;
    }

    @PUT
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("ADMIN")
    public Consent updateDUL(
            @FormDataParam("data") InputStream uploadedDUL,
            @FormDataParam("data") FormDataBodyPart part,
            @PathParam("id") String consentId) {
        String msg = String.format("PUTing Data Use Letter to consent with id '%s'", consentId);
        logger().debug(msg);
        try {
            deletePreviousStorageFile(consentId);
            String toStoreFileName =  UUID.randomUUID() + "." + getFileExtension(part.getContentDisposition().getFileName());
            String dulUrl = store.putStorageDocument(uploadedDUL, part.getMediaType().toString(), toStoreFileName);
            return api.updateConsentDul(consentId,dulUrl,part.getContentDisposition().getFileName());
        } catch (UnknownIdentifierException e) {
            throw new NotFoundException(String.format("Could not find consent with id %s", consentId));
        } catch (IOException e) {
            logger().error("Error when trying to read/write the uploaded file " + e.getMessage());
        } catch (GeneralSecurityException e) {
            logger().error("Security error: " + e.getMessage());
        }
        return null;
    }

    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @RolesAllowed({"ADMIN","CHAIRPERSON","MEMBER"})
    public Response getDUL(@PathParam("id") String consentId) {
        String msg = String.format("GETing Data Use Letter for consent with id '%s'", consentId);
        logger().debug(msg);
        try {
            Consent consent = api.retrieve(consentId);
            String fileUrl  = consent.getDataUseLetter();
            String fileName = consent.getDulName();
            HttpResponse r = store.getStorageDocument(fileUrl);
            File targetFile = new File(fileName);
            FileUtils.copyInputStreamToFile(r.getContent(), targetFile);
            return Response.ok(targetFile)
                    .type(r.getContentType())
                    .header("Content-Disposition", "attachment; filename=" + targetFile.getName())
                    .build();
        } catch (UnknownIdentifierException e) {
            throw new NotFoundException(String.format("Could not find consent with id %s", consentId));
        } catch (IOException e) {
            logger().error("Error when trying to read/write the file " + e.getMessage());
        } catch (GeneralSecurityException e) {
            logger().error("Security error: " + e.getMessage());
        }
        return null;
    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("ADMIN")
    public Consent deleteDUL(@PathParam("id") String consentId) {
        String msg = String.format("DELETEing Data Use Letter for consent with id '%s'", consentId);
        logger().debug(msg);
        try {
            deletePreviousStorageFile(consentId);
            return api.deleteConsentDul(consentId);
        } catch (UnknownIdentifierException e) {
            throw new NotFoundException(String.format("Could not find consent with id %s", consentId));
        } catch (IOException e) {
            logger().error("Error when trying to read/write the file " + e.getMessage());
        } catch (GeneralSecurityException e) {
            logger().error("Security error: " + e.getMessage());
        }
        return null;
    }

    @Override
    protected Logger logger() {
        return Logger.getLogger("DataUseLetterResource");
    }
}
