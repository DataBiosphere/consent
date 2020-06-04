package org.broadinstitute.consent.http.resources;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.cloudstore.GCSStore;
import org.broadinstitute.consent.http.enumeration.Actions;
import org.broadinstitute.consent.http.enumeration.AuditTable;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.service.AbstractConsentAPI;
import org.broadinstitute.consent.http.service.AuditService;
import org.broadinstitute.consent.http.service.ConsentAPI;
import org.broadinstitute.consent.http.service.UnknownIdentifierException;
import org.broadinstitute.consent.http.service.UserService;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.UUID;

@Path("{auth: (basic/|api/)?}consent/{id}/dul")
public class DataUseLetterResource extends Resource {

    private final ConsentAPI api;
    private final GCSStore store;
    private final AuditService auditService;
    private final UserService userService;

    @Inject
    public DataUseLetterResource(AuditService auditService, GCSStore store, UserService userService) {
        this.auditService = auditService;
        this.api = AbstractConsentAPI.getInstance();
        this.store = store;
        this.userService = userService;
    }

    private String getFileExtension(String fileName) {
        return FilenameUtils.getExtension(fileName);
    }

    private void deletePreviousStorageFile(String consentId) throws IOException, GeneralSecurityException, UnknownIdentifierException {
        String prevFile = api.getConsentDulUrl(consentId);
        if (StringUtils.isNotBlank(prevFile)) {
            try {
                new GenericUrl(prevFile);
                store.deleteStorageDocument(prevFile);
            } catch (IllegalArgumentException e) {
                logger().warn("Document URL is not valid, could not delete '" + prevFile + "' from GCS for consent id: " + consentId);
            }
        }
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({RESEARCHER, DATAOWNER, ADMIN})
    public Consent createDUL(
            @FormDataParam("data") InputStream uploadedDUL,
            @FormDataParam("data") FormDataBodyPart part,
            @PathParam("id") String consentId,
            @QueryParam("fileName") String fileName,
            @Auth AuthUser user) {
        String msg = String.format("POSTing Data Use Letter to consent with id '%s'", consentId);
        logger().debug(msg);
        try {
            String name = StringUtils.isNotEmpty(fileName) ? fileName : part.getContentDisposition().getFileName();
            deletePreviousStorageFile(consentId);
            String toStoreFileName =  UUID.randomUUID() + "." + getFileExtension(part.getContentDisposition().getFileName());
            String dulUrl = store.postStorageDocument(uploadedDUL, part.getMediaType().toString(), toStoreFileName);
            Consent consent = api.updateConsentDul(consentId, dulUrl, name);
            DACUser dacUser = userService.findUserByEmail(user.getName());
            auditService.saveConsentAudit(consentId, AuditTable.CONSENT.getValue(), Actions.REPLACE.getValue(), dacUser.getEmail());
            return consent;
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
    @RolesAllowed(ADMIN)
    public Consent updateDUL(
            @FormDataParam("data") InputStream uploadedDUL,
            @FormDataParam("data") FormDataBodyPart part,
            @PathParam("id") String consentId,
            @QueryParam("fileName") String fileName,
            @Auth AuthUser user) {
        String msg = String.format("PUTing Data Use Letter to consent with id '%s'", consentId);
        logger().debug(msg);
        try {
            String name = StringUtils.isNotEmpty(fileName) ? fileName : part.getContentDisposition().getFileName();
            deletePreviousStorageFile(consentId);
            String toStoreFileName =  UUID.randomUUID() + "." + getFileExtension(part.getContentDisposition().getFileName());
            String dulUrl = store.putStorageDocument(uploadedDUL, part.getMediaType().toString(), toStoreFileName);
            Consent consent = api.updateConsentDul(consentId,dulUrl, name);
            DACUser dacUser = userService.findUserByEmail(user.getName());
            auditService.saveConsentAudit(consent.getConsentId(), AuditTable.CONSENT.getValue(), Actions.REPLACE.getValue(), dacUser.getEmail());
            return api.updateConsentDul(consentId,dulUrl, name);
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
    @RolesAllowed({ADMIN, CHAIRPERSON, MEMBER, DATAOWNER})
    public Response getDUL(@PathParam("id") String consentId, @QueryParam("electionId") Integer electionId) {
        String msg = String.format("GETing Data Use Letter for consent with id '%s' and Election Id '%s", consentId, electionId);
        logger().debug(msg);
        Election election = null;
        try {
            Consent consent = api.retrieve(consentId);
            if (StringUtils.isNotEmpty(consent.getLastElectionStatus())) {
                election = api.retrieveElection(electionId, consentId);
            }
            String fileUrl = election != null ? election.getDataUseLetter() : consent.getDataUseLetter();
            String fileName = election != null ? election.getDulName() : consent.getDulName();
            HttpResponse r = store.getStorageDocument(fileUrl);
            StreamingOutput stream = createStreamingOutput(r.getContent());
            return Response.ok(stream)
                    .type(r.getContentType())
                    .header("Content-Disposition", "attachment; filename=" + fileName)
                    .build();
        } catch (UnknownIdentifierException e) {
            throw new NotFoundException(e);
        } catch (IOException e) {
            logger().error("Error when trying to read/write the file " + e.getMessage());
        }
        return null;
    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(ADMIN)
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
        return LoggerFactory.getLogger(this.getClass());
    }
}