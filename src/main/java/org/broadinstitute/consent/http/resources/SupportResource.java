package org.broadinstitute.consent.http.resources;

import com.google.api.client.http.HttpStatusCodes;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.InputStream;
import org.broadinstitute.consent.http.cloudstore.UploadTooLargeException;
import org.broadinstitute.consent.http.configurations.StoreConfiguration;
import org.broadinstitute.consent.http.filters.UploadSizeLimited;
import org.broadinstitute.consent.http.models.support.SupportRequester;
import org.broadinstitute.consent.http.models.support.TicketFields;
import org.broadinstitute.consent.http.service.SupportRequestService;

@Path("support")
public class SupportResource extends Resource {

  private final SupportRequestService supportRequestService;

  /** The largest attachment, the same limit as stored uploads (googleStore.maxUploadBytes). */
  private final long maxUploadBytes;

  @Inject
  public SupportResource(
      SupportRequestService supportRequestService, StoreConfiguration storeConfiguration) {
    this.supportRequestService = supportRequestService;
    this.maxUploadBytes = storeConfiguration.getMaxUploadBytes();
  }

  @POST
  @Path("request")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response postRequest(String body) {
    try {
      JsonObject request =
          supportRequestService.submitTicket(
              TicketFields.fromJson(body), SupportRequester.unverified());
      return Response.status(HttpStatusCodes.STATUS_CODE_CREATED).entity(request).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @POST
  @Path("upload")
  @UploadSizeLimited
  @Consumes("application/binary")
  @Produces(MediaType.APPLICATION_JSON)
  public Response postUpload(InputStream body) {
    try {
      // Reads one byte past the limit at most, so an oversized body is never read whole. The
      // limit's bounds (StoreConfiguration) keep that within one array.
      byte[] content = body.readNBytes(Math.toIntExact(maxUploadBytes + 1));
      if (content.length > maxUploadBytes) {
        throw new UploadTooLargeException(maxUploadBytes);
      }
      JsonObject token = supportRequestService.postAttachmentToSupport(content);
      logInfo("Support Request Content Upload: " + content.length + " bytes");
      return Response.status(HttpStatusCodes.STATUS_CODE_CREATED).entity(token).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }
}
