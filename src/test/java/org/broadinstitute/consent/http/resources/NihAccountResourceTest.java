package org.broadinstitute.consent.http.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import com.google.api.client.http.HttpStatusCodes;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.NIHUserAccount;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.NihService;
import org.broadinstitute.consent.http.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NihAccountResourceTest {

  @Mock
  private NihService nihService;

  @Mock
  private UserService userService;

  @Mock
  private NIHUserAccount nihAccount;

  @Mock
  private User user;

  @Mock
  private AuthUser authUser;

  private NihAccountResource resource;

  @Test
  void testRegisterResearcherSuccess() {
    when(userService.findUserByEmail(any())).thenReturn(user);
    resource = new NihAccountResource(nihService, userService);
    Response response = resource.registerResearcher(nihAccount, authUser);
    assertEquals(200, response.getStatus());
  }

  @Test
  void testRegisterResearcherNoAuth() {
    when(userService.findUserByEmail(any())).thenReturn(null);
    resource = new NihAccountResource(nihService, userService);
    Response response = resource.registerResearcher(nihAccount, authUser);
    assertEquals(500, response.getStatus());
  }

  @Test
  void testRegisterResearcherError() {
    when(userService.findUserByEmail(any())).thenReturn(user);
    doThrow(new RuntimeException()).when(nihService).authenticateNih(any(), any(), any());
    resource = new NihAccountResource(nihService, userService);
    Response response = resource.registerResearcher(nihAccount, authUser);
    assertEquals(500, response.getStatus());
  }

  @Test
  void testRegisterResearcherNullAccountError() {
    doThrow(new BadRequestException()).when(nihService).validateNihUserAccount(any(), any());
    resource = new NihAccountResource(nihService, userService);
    Response response = resource.registerResearcher(null, authUser);
    assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
  }

  @Test
  void testDeleteNihAccountSuccess() {
    when(userService.findUserByEmail(any())).thenReturn(user);
    resource = new NihAccountResource(nihService, userService);
    Response response = resource.deleteNihAccount(authUser);
    assertEquals(200, response.getStatus());
  }

  @Test
  void testDeleteNihAccountNoAuth() {
    when(userService.findUserByEmail(any())).thenReturn(null);
    resource = new NihAccountResource(nihService, userService);
    Response response = resource.deleteNihAccount(authUser);
    assertEquals(500, response.getStatus());
  }

  @Test
  void testDeleteNihAccountError() {
    when(userService.findUserByEmail(any())).thenReturn(user);
    doThrow(new RuntimeException()).when(nihService).deleteNihAccountById(any());
    resource = new NihAccountResource(nihService, userService);
    Response response = resource.deleteNihAccount(authUser);
    assertEquals(500, response.getStatus());
  }
}
