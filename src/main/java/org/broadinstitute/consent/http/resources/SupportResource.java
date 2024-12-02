package org.broadinstitute.consent.http.resources;


import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import org.broadinstitute.consent.http.enumeration.SupportRequestType;
import org.broadinstitute.consent.http.models.support.SupportTicket;
import org.broadinstitute.consent.http.service.SupportRequestService;
import org.broadinstitute.consent.http.util.gson.GsonUtil;


@Path("/support")
public class SupportResource extends Resource {

  private final SupportRequestService supportRequestService;

  @Inject
  public SupportResource(SupportRequestService supportRequestService) {
    this.supportRequestService = supportRequestService;
  }

  @Path("/request")
  @POST
  public Response createTicket(String json) {
    try {
      JsonObject obj = GsonUtil.getInstance().fromJson(json, JsonObject.class);
      List<String> uploads =
          obj.has("uploads") ?
              new ArrayList<>(
                  obj.get("uploads").getAsJsonArray().asList().stream()
                      .map(JsonElement::getAsString)
                      .toList()) :
              null;
      SupportTicket ticket = new SupportTicket(
          obj.get("name").getAsString(),
          SupportRequestType.valueOf(obj.get("type").getAsString().toUpperCase()),
          obj.get("email").getAsString(),
          obj.get("subject").getAsString(),
          obj.get("description").getAsString(),
          obj.get("url").getAsString(),
          uploads
      );
      String ticketResponse = supportRequestService.postTicketToSupport(ticket);
      if (ticketResponse != null) {
        return Response.ok(ticketResponse).build();
      }
      throw new InternalServerErrorException("Failed to create ticket");
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

}
