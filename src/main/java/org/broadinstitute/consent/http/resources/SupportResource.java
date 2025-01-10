package org.broadinstitute.consent.http.resources;

import com.google.gson.Gson;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.broadinstitute.consent.http.models.support.DuosTicket;
import org.broadinstitute.consent.http.models.support.TicketFields;
import org.broadinstitute.consent.http.models.support.TicketFactory;
import org.broadinstitute.consent.http.service.SupportRequestService;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.owasp.fileio.FileValidator;
import org.zendesk.client.v2.model.Request;

@Path("support")
public class SupportResource extends Resource {

  private final SupportRequestService supportRequestService;
  private final static TicketFactory ticketFactory = new TicketFactory();
  private final static Gson gson = GsonUtil.getInstance();
  private final static FileValidator validator = new FileValidator();

  public SupportResource(SupportRequestService supportRequestService) {
    this.supportRequestService = supportRequestService;
  }

  @POST
  @Path("request")
  public Response postRequest(String body) {
    try {
      TicketFields ticketFields = gson.fromJson(body, TicketFields.class);
      DuosTicket ticket = ticketFactory.createTicket(ticketFields);
      Request request = supportRequestService.postTicketToSupport(ticket);
      return Response.ok(request).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @POST
  @Path("upload")
  public Response postUpload(byte[] content) {
    try {
      if (content.length > validator.getMaxFileUploadSize()) {
        return Response.status(Response.Status.BAD_REQUEST).build();
      }
      String token = supportRequestService.postAttachmentToSupport(content);
      return Response.ok(token).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

}
