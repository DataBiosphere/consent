package org.broadinstitute.consent.http.resources;

import com.google.api.client.http.GenericUrl;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.UUID;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.cloudstore.GCSStore;
import org.broadinstitute.consent.http.enumeration.Actions;
import org.broadinstitute.consent.http.enumeration.AuditTable;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.AuditService;
import org.broadinstitute.consent.http.service.ConsentService;
import org.broadinstitute.consent.http.service.UnknownIdentifierException;
import org.broadinstitute.consent.http.service.UserService;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("api/consent/{id}/dul")
public class DataUseLetterResource extends Resource {

    private final ConsentService consentService;
    private final GCSStore store;
    private final AuditService auditService;
    private final UserService userService;

    @Inject
    public DataUseLetterResource(AuditService auditService, GCSStore store, UserService userService, ConsentService consentService) {
        this.auditService = auditService;
        this.consentService = consentService;
        this.store = store;
        this.userService = userService;
    }

    private String getFileExtension(String fileName) {
        return FilenameUtils.getExtension(fileName);
    }

    private void deletePreviousStorageFile(String consentId) throws IOException, GeneralSecurityException, UnknownIdentifierException {
        String prevFile = consentService.getConsentDulUrl(consentId);
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
            Consent consent = consentService.updateConsentDul(consentId, dulUrl, name);
            User dacUser = userService.findUserByEmail(user.getName());
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

    @Override
    protected Logger logger() {
        return LoggerFactory.getLogger(this.getClass());
    }
}