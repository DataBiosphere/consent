package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
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

  private final SupportResource supportResource;
  private final UserService userService;

  @Inject
  public SignedInSupportResource(
      SupportRequestService supportRequestService, UserService userService) {
    this.supportResource = new SupportResource(supportRequestService);
    this.userService = userService;
  }

  @POST
  @Path("request")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response postRequest(@Auth AuthUser authUser, String body) {
    try {
      TicketFields ticketFields = SupportResource.ticketFields(body);
      // Before the user lookup, so an invalid request costs no query.
      ticketFields.validate();
      SupportRequester requester =
          SupportRequester.signedIn(
              authUser.getEmail(), duosUserId(authUser), ticketFields.email());
      return supportResource.submit(ticketFields, requester);
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
