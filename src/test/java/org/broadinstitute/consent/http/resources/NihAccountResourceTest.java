package org.broadinstitute.consent.http.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.google.api.client.http.HttpStatusCodes;
import jakarta.ws.rs.BadRequestException;
import java.util.Date;
import java.util.List;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.models.NIHUserAccount;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.service.NihService;
import org.broadinstitute.consent.http.service.UserService;
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


  private final AuthUser authUser = new AuthUser("test");
  private final List<UserRole> roles = List.of(UserRoles.Researcher());
  private final User user = new User(1, authUser.getEmail(), "Display Name", new Date(), roles);

  private final DuosUser duosUser = new DuosUser(authUser, user);

  private NihAccountResource resource;

  @Test
  void testSyncAccountSuccess() {
    resource = new NihAccountResource(nihService, userService);
    try (var response = resource.syncAccount(duosUser)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }
  }

  @Test
  void testSyncAccountNoAuth() throws Exception {
    when(nihService.syncAccount(duosUser)).thenThrow(new RuntimeException());
    resource = new NihAccountResource(nihService, userService);
    try (var response = resource.syncAccount(duosUser)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_SERVER_ERROR, response.getStatus());
    }
  }

  @Test
  void testRegisterResearcherSuccess() {
    resource = new NihAccountResource(nihService, userService);
    try (var response = resource.registerResearcher(duosUser, nihAccount)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }
  }

  @Test
  void testRegisterResearcherError() {
    doThrow(new RuntimeException()).when(nihService).authenticateNih(any(), any(), any());
    resource = new NihAccountResource(nihService, userService);
    try (var response = resource.registerResearcher(duosUser, nihAccount)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_SERVER_ERROR, response.getStatus());
    }
  }

  @Test
  void testRegisterResearcherNullAccountError() {
    doThrow(new BadRequestException()).when(nihService).validateNihUserAccount(any(), any());
    resource = new NihAccountResource(nihService, userService);
    try (var response = resource.registerResearcher(duosUser,null)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }
  }

  @Test
  void testDeleteNihAccountSuccess() {
    resource = new NihAccountResource(nihService, userService);
    try (var response = resource.deleteNihAccount(duosUser)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }
  }

  @Test
  void testDeleteNihAccountError() {
    doThrow(new RuntimeException()).when(nihService).deleteNihAccountById(duosUser);
    resource = new NihAccountResource(nihService, userService);
    try (var response = resource.deleteNihAccount(duosUser)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_SERVER_ERROR, response.getStatus());
    }
  }
}
