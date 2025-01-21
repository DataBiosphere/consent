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
import org.broadinstitute.consent.http.models.support.DuosTicket;
import org.broadinstitute.consent.http.models.support.TicketFactory;
import org.broadinstitute.consent.http.models.support.TicketFields;
import org.broadinstitute.consent.http.service.SupportRequestService;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.owasp.fileio.FileValidator;
import org.zendesk.client.v2.model.Request;

@Path("support")
public class SupportResource extends Resource {

  private final SupportRequestService supportRequestService;
  private final static Gson gson = GsonUtil.getInstance();
  static final long MAX_FILE_UPLOAD_SIZE = new FileValidator().getMaxFileUploadSize();

  @Inject
  public SupportResource(SupportRequestService supportRequestService) {
    this.supportRequestService = supportRequestService;
  }

  @POST
  @Path("request")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response postRequest(String body) {
    try {
      TicketFields ticketFields = gson.fromJson(body, TicketFields.class);
      DuosTicket ticket = TicketFactory.createTicket(ticketFields);
      logInfo("Support Request Ticket: " + ticket.toString());
      Request request = supportRequestService.postTicketToSupport(ticket);
      return Response.status(HttpStatusCodes.STATUS_CODE_CREATED).entity(request).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @POST
  @Path("upload")
  @Consumes("application/binary")
  @Produces(MediaType.APPLICATION_JSON)
  public Response postUpload(byte[] content) {
    try {
      if (content.length > MAX_FILE_UPLOAD_SIZE) {
        return Response.status(Response.Status.BAD_REQUEST).build();
      }
      JsonObject token = supportRequestService.postAttachmentToSupport(content);
      logInfo("Support Request Content Upload: " + content.length + " bytes");
      return Response.status(HttpStatusCodes.STATUS_CODE_CREATED).entity(token).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

}
