package org.genomebridge.consent.http.resources;

import com.google.api.client.http.HttpResponse;
import com.sun.jersey.api.NotFoundException;
import com.sun.jersey.multipart.FormDataBodyPart;
import com.sun.jersey.multipart.FormDataParam;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.genomebridge.consent.http.cloudstore.GCSStore;
import org.genomebridge.consent.http.models.Consent;
import org.genomebridge.consent.http.service.AbstractConsentAPI;
import org.genomebridge.consent.http.service.ConsentAPI;
import org.genomebridge.consent.http.service.UnknownIdentifierException;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;

@Path("/consent/{id}/dul")
public class DataUseLetterResource extends Resource {

    private ConsentAPI api;
    private GCSStore store;

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
    public Consent createDUL(@FormDataParam("data") InputStream uploadedDUL, @FormDataParam("data") FormDataBodyPart part, @PathParam("id") String consentId) {
        String msg = String.format("POSTing Data Use Letter to consent with id '%s'", consentId);
        logger().debug(msg);

        try {
            deletePreviousStorageFile(consentId);
            String dulUrl = store.postStorageDocument(consentId, uploadedDUL, part.getMediaType().toString(), getFileExtension(part.getContentDisposition().getFileName()));
            return api.updateConsentDul(consentId, dulUrl, part.getContentDisposition().getFileName());
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
    public Consent updateDUL(@FormDataParam("data") InputStream uploadedDUL, @FormDataParam("data") FormDataBodyPart part, @PathParam("id") String consentId) {
        String msg = String.format("PUTing Data Use Letter to consent with id '%s'", consentId);
        logger().debug(msg);
        try {
            deletePreviousStorageFile(consentId);
            String dulUrl = store.putStorageDocument(consentId, uploadedDUL, part.getMediaType().toString(), getFileExtension(part.getContentDisposition().getFileName()));
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
    public Response getDUL(@PathParam("id") String consentId) {
        String msg = String.format("GETing Data Use Letter for consent with id '%s'", consentId);
        logger().debug(msg);
        try {
            String prevFile = api.getConsentDulUrl(consentId);
            HttpResponse r = store.getStorageDocument(prevFile);
            File targetFile = new File("dataUseLetter-" + consentId + "." + getFileExtension(prevFile));
            FileUtils.copyInputStreamToFile(r.getContent(), targetFile);
            return Response.ok(targetFile)
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
