package org.broadinstitute.consent.http.resources;

import com.google.api.client.http.HttpStatusCodes;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.support.SupportRequester;
import org.broadinstitute.consent.http.models.support.TicketFields;
import org.broadinstitute.consent.http.service.SupportRequestService;
import org.broadinstitute.consent.http.service.UserService;

/**
 * The support form for a signed-in user: the same request as {@code POST /support/request}, but
 * under /api, where the sidecar verifies the sign-in, so the ticket can say who sent it. Any
 * sign-in will do, with or without a DUOS account, so someone can ask for help registering.
 * Attachments still go through the open {@code POST /support/upload}.
 */
@Path("api/support")
public class SignedInSupportResource extends Resource {

  private final SupportRequestService supportRequestService;
  private final UserService userService;

  @Inject
  public SignedInSupportResource(
      SupportRequestService supportRequestService, UserService userService) {
    this.supportRequestService = supportRequestService;
    this.userService = userService;
  }

  @POST
  @Path("request")
  @PermitAll
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response postRequest(@Auth AuthUser authUser, String body) {
    try {
      // Validated before the user lookup, so an invalid request costs no query.
      TicketFields ticketFields = TicketFields.fromJson(body);
      SupportRequester requester =
          SupportRequester.signedIn(
              authUser.getEmail(), duosUserId(authUser), ticketFields.email());
      JsonObject request = supportRequestService.submitTicket(ticketFields, requester);
      return Response.status(HttpStatusCodes.STATUS_CODE_CREATED).entity(request).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  private Integer duosUserId(AuthUser authUser) {
    try {
      return userService.findUserByEmail(authUser.getEmail()).getUserId();
    } catch (NotFoundException e) {
      return null;
    }
  }
}
