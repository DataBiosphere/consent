package org.broadinstitute.consent.http.resources;

import com.google.api.client.http.HttpStatusCodes;
import com.google.gson.Gson;
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
import org.broadinstitute.consent.http.models.support.DuosTicket;
import org.broadinstitute.consent.http.models.support.SupportRequester;
import org.broadinstitute.consent.http.models.support.TicketFactory;
import org.broadinstitute.consent.http.models.support.TicketFields;
import org.broadinstitute.consent.http.service.SupportRequestService;
import org.broadinstitute.consent.http.util.gson.GsonUtil;

@Path("support")
public class SupportResource extends Resource {

  private final SupportRequestService supportRequestService;
  private static final Gson gson = GsonUtil.getInstance();

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
      return submit(ticketFields(body), SupportRequester.unverified());
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  static TicketFields ticketFields(String body) {
    return gson.fromJson(body, TicketFields.class);
  }

  /**
   * Sends the ticket to Zendesk; shared with the signed-in route ({@link SignedInSupportResource}).
   */
  Response submit(TicketFields ticketFields, SupportRequester requester) throws Exception {
    DuosTicket ticket = TicketFactory.createTicket(ticketFields, requester);
    logInfo("Support Request Ticket: " + ticket);
    JsonObject request = supportRequestService.postTicketToSupport(ticket);
    return Response.status(HttpStatusCodes.STATUS_CODE_CREATED).entity(request).build();
  }

  @POST
  @Path("upload")
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
