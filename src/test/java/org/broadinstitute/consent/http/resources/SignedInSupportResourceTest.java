package org.broadinstitute.consent.http.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.client.http.HttpStatusCodes;
import com.google.gson.JsonObject;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.support.SupportRequester;
import org.broadinstitute.consent.http.service.SupportRequestService;
import org.broadinstitute.consent.http.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SignedInSupportResourceTest {

  private static final String BODY =
      """
      {
        "name": "Ada",
        "email": "%s",
        "subject": "Help",
        "description": "Registering",
        "type": "QUESTION",
        "url": "https://example.com"
      }
      """;

  @Mock private SupportRequestService supportRequestService;
  @Mock private UserService userService;

  private SignedInSupportResource resource;
  private final AuthUser authUser = new AuthUser().setEmail("ada@example.org");

  @BeforeEach
  void setUp() {
    resource = new SignedInSupportResource(supportRequestService, userService);
  }

  @Test
  void testTheTicketNamesTheRequestersDuosUser() throws Exception {
    User user = new User();
    user.setUserId(12);
    when(userService.findUserByEmail("ada@example.org")).thenReturn(user);

    String body = postAndCaptureNote("ada@example.org");

    assertEquals(
        "Requester: signed in as DUOS user 12. The form's email is the sign-in email.", body);
  }

  // Someone who signed in but has not registered can still ask for help.
  @Test
  void testASignInWithoutADuosAccountIsAccepted() throws Exception {
    when(userService.findUserByEmail("ada@example.org")).thenThrow(new NotFoundException());

    String body = postAndCaptureNote("bob@example.org");

    assertEquals(
        "Requester: signed in, without a DUOS account. The form's email differs from the"
            + " sign-in email and is not verified.",
        body);
  }

  @Test
  void testAnInvalidRequestIsRejected() throws Exception {
    try (Response response = resource.postRequest(authUser, "{\"name\": \"Ada\"}")) {
      assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }
    verify(supportRequestService, never()).submitTicket(any(), any());
    verify(userService, never()).findUserByEmail(any());
  }

  @Test
  void testAnEmptyRequestIsABadRequest() throws Exception {
    try (Response response = resource.postRequest(authUser, null)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }
    verify(supportRequestService, never()).submitTicket(any(), any());
  }

  private String postAndCaptureNote(String formEmail) throws Exception {
    when(supportRequestService.submitTicket(any(), any())).thenReturn(new JsonObject());

    try (Response response = resource.postRequest(authUser, BODY.formatted(formEmail))) {
      assertEquals(HttpStatusCodes.STATUS_CODE_CREATED, response.getStatus());
    }
    ArgumentCaptor<SupportRequester> requester = ArgumentCaptor.forClass(SupportRequester.class);
    verify(supportRequestService).submitTicket(any(), requester.capture());
    return requester.getValue().note();
  }
}
