package org.genomebridge.consent.http.resources;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;
import com.sun.jersey.api.NotFoundException;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.genomebridge.consent.http.cloudstore.FileType;
import org.genomebridge.consent.http.cloudstore.GCSStore;
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
public class DataUseLetterResource extends Resource{

    private ConsentAPI api;
    private GCSStore store;

    public DataUseLetterResource(GCSStore store) {
        this.api = AbstractConsentAPI.getInstance();
        this.store = store;
    }

    public FileType validFileType(String name){
        if(name.equalsIgnoreCase("pdf")){
            return FileType.PDF;
        } else {
            if(name.equalsIgnoreCase("doc")){
                return FileType.DOC;
            }
        }
        return null;
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public String  createDUL(@FormDataParam("data") InputStream uploadedDUL, @FormDataParam("data") FormDataContentDisposition contentDispositionHeader, @PathParam("id")String consentId){
        String msg = String.format("POSTing Data Use Letter to consent with id '%s'", consentId);
        logger().debug(msg);
        FileType type = validFileType(contentDispositionHeader.getFileName().split("\\.")[1]);
        try {
            String dulUrl = store.postStorageDocument(consentId, uploadedDUL, type);
            api.updateCreateDUL(consentId, dulUrl);
        } catch (UnknownIdentifierException e) {
            throw new NotFoundException(String.format("Could not find consent with id %s", consentId));
        } catch (IOException e){
            logger().error("Error when trying to read/write the uploaded file "+ e.getMessage());
        } catch (GeneralSecurityException e){
            logger().error("Security error: "+ e.getMessage());
        }
        return "Success";
    };

    @PUT
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public String updateDUL(@FormDataParam("data") InputStream uploadedDUL, @FormDataParam("data") FormDataContentDisposition contentDispositionHeader, @PathParam("id")String consentId) {
        String msg = String.format("PUTing Data Use Letter to consent with id '%s'", consentId);
        logger().debug(msg);
        FileType type = validFileType(contentDispositionHeader.getFileName().split("\\.")[1]);
        try {
            String dulUrl = store.putStorageDocument(consentId, uploadedDUL, type);
            api.updateCreateDUL(consentId, dulUrl);
        } catch (UnknownIdentifierException e) {
            throw new NotFoundException(String.format("Could not find consent with id %s", consentId));
        } catch (IOException e){
            logger().error("Error when trying to read/write the uploaded file "+ e.getMessage());
        } catch (GeneralSecurityException e){
            logger().error("Security error: "+ e.getMessage());
        }
        return "Success";
    }

    @GET
    @Produces({"application/pdf","application/msword"})
    public Response getDUL(@PathParam("id")String consentId){
        String msg = String.format("GETing Data Use Letter for consent with id '%s'", consentId);
        logger().debug(msg);
        try {
            HttpResponse  r = store.getStorageDocument(new GenericUrl(api.getFileURL(consentId)));
            File targetFile = null;
            if(r.getContentType().equals(FileType.PDF.toString())){
                targetFile = new File("dataUseLetter-"+consentId+".pdf");
            }if(r.getContentType().equals(FileType.DOC.toString())){
                targetFile = new File("dataUseLetter-"+consentId+".doc");
            }
            FileUtils.copyInputStreamToFile(r.getContent(), targetFile);
            Response.ResponseBuilder response = Response.ok((Object) targetFile);
            response.header("Content-Disposition",
                    "attachment; filename="+targetFile.getName());
            return response.build();
        } catch (UnknownIdentifierException e) {
            throw new NotFoundException(String.format("Could not find consent with id %s", consentId));
        } catch (IOException e){
            logger().error("Error when trying to read/write the file "+ e.getMessage());
        } catch (GeneralSecurityException e){
            logger().error("Security error: "+ e.getMessage());
        }
        return null;
    };

    @DELETE
    public String deleteDUL(@PathParam("id")String consentId){
        String msg = String.format("DELETEing Data Use Letter for consent with id '%s'", consentId);
        logger().debug(msg);
        try {
            store.deleteStorageDocument(consentId);
            api.deleteDUL(consentId);
        } catch (UnknownIdentifierException e) {
            throw new NotFoundException(String.format("Could not find consent with id %s", consentId));
        } catch (IOException e){
            logger().error("Error when trying to read/write the file "+ e.getMessage());
        } catch (GeneralSecurityException e){
            logger().error("Security error: "+ e.getMessage());
        }
        return "Success";
    };

    @Override
    protected Logger logger() {
        return Logger.getLogger("DataUseLetterResource");
    }
}
