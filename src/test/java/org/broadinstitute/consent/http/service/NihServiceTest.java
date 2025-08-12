package org.broadinstitute.consent.http.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import com.google.api.client.http.HttpStatusCodes;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ServerErrorException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.broadinstitute.consent.http.MockServerTestHelper;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.db.UserPropertyDAO;
import org.broadinstitute.consent.http.enumeration.UserFields;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.NIHUserAccount;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserProperty;
import org.broadinstitute.consent.http.models.ecm.LinkInfo;
import org.broadinstitute.consent.http.service.dao.NihServiceDAO;
import org.broadinstitute.consent.http.util.HttpClientUtil;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NihServiceTest extends MockServerTestHelper {

  @Mock
  private UserDAO userDAO;

  @Mock
  private UserPropertyDAO userPropertyDAO;

  @Mock
  private NihServiceDAO nihServiceDAO;

  private NihService service;
  private NIHUserAccount nihUserAccount;
  private AuthUser authUser;

  @BeforeEach
  void setUp() {
    ServicesConfiguration servicesConfig = new ServicesConfiguration();
    servicesConfig.setTimeoutSeconds(1);
    servicesConfig.setEcmUrl(
        "http://" + CONTAINER.getHost() + ":" + CONTAINER.getServerPort() + "/");
    nihUserAccount = new NIHUserAccount("nih username", new Date().toString(), true);
    authUser = new AuthUser("test@test.com");
    service = new NihService(userDAO, userPropertyDAO, nihServiceDAO,
        new HttpClientUtil(servicesConfig), servicesConfig);
    mockServerClient.reset();
  }

  @Test
  void testSyncAccount() throws Exception {
    User user = new User();
    user.setUserId(1);
    when(userDAO.findUserByEmail(authUser.getEmail())).thenReturn(user);
    when(userDAO.findUserWithPropertiesById(user.getUserId(), UserFields.getValues())).thenReturn(user);
    LinkInfo ecmResponse = new LinkInfo("test", "test", true);
    NIHUserAccount nihAccount = new NIHUserAccount(
        ecmResponse.externalUserId(), ecmResponse.expirationTimestamp(), ecmResponse.authenticated());
    mockServerClient.when(request())
        .respond(response()
            .withStatusCode(200)
            .withBody(GsonUtil.getInstance().toJson(ecmResponse)));
    User syncedUser = service.syncAccount(authUser);
    assertEquals(user.getUserId(), syncedUser.getUserId());
    verify(nihServiceDAO).updateUserNihStatus(user, nihAccount);
  }

  @Test
  void testSyncAccountBadRequestError() {
    User user = new User();
    user.setUserId(1);
    when(userDAO.findUserByEmail(authUser.getEmail())).thenReturn(user);
    LinkInfo ecmResponse = new LinkInfo("test", "test", true);
    mockServerClient.when(request())
        .respond(response()
            .withStatusCode(HttpStatusCodes.STATUS_CODE_BAD_REQUEST)
            .withBody(GsonUtil.getInstance().toJson(ecmResponse)));
    assertThrows(BadRequestException.class, () -> service.syncAccount(authUser));
  }

  @Test
  void testSyncAccountServerError() {
    User user = new User();
    user.setUserId(1);
    when(userDAO.findUserByEmail(authUser.getEmail())).thenReturn(user);
    LinkInfo ecmResponse = new LinkInfo("test", "test", true);
    mockServerClient.when(request())
        .respond(response()
            .withStatusCode(HttpStatusCodes.STATUS_CODE_SERVER_ERROR)
            .withBody(GsonUtil.getInstance().toJson(ecmResponse)));
    assertThrows(ServerErrorException.class, () -> service.syncAccount(authUser));
  }

  @Test
  void testAuthenticateNih_InvalidUser() {
    AuthUser testUser = new AuthUser("test@test.com");
    assertThrows(NotFoundException.class,
        () -> service.authenticateNih(nihUserAccount, testUser, 1));
  }

  @Test
  void testAuthenticateNih() {
    List<UserProperty> props = Collections.singletonList(new UserProperty(1, 1, "test", "value"));
    when(userPropertyDAO.findUserPropertiesByUserIdAndPropertyKeys(any(), any())).thenReturn(props);
    User user = new User();
    user.setUserId(1);
    when(userDAO.findUserById(any())).thenReturn(user);
    try {
      List<UserProperty> properties = service.authenticateNih(nihUserAccount, authUser,
          user.getUserId());
      assertEquals(1, properties.size());
      assertEquals(Integer.valueOf(1), properties.get(0).getPropertyId());
      verify(nihServiceDAO, times(1)).updateUserNihStatus(user, nihUserAccount);
    } catch (BadRequestException bre) {
      assert false;
    }
  }

  @Test
  void testAuthenticateNih_BadRequest() {
    User user = new User();
    user.setUserId(1);
    when(userDAO.findUserById(any())).thenReturn(user);
    nihUserAccount.setNihUsername("");
    assertThrows(BadRequestException.class,
        () -> service.authenticateNih(nihUserAccount, authUser, 1));
  }

  @Test
  void testAuthenticateNih_BadRequestNullAccount() {
    assertThrows(BadRequestException.class, () -> service.authenticateNih(null, authUser, 1));
  }

  @Test
  void testAuthenticateNih_BadRequestNullAccountExpiration() {
    NIHUserAccount account = new NIHUserAccount();
    account.setStatus(true);
    assertThrows(BadRequestException.class, () -> service.authenticateNih(account, authUser, 1));
  }

  @Test
  void testDeleteNihAccountById() {
    User user = new User();
    user.setUserId(1);
    when(userDAO.findUserById(any())).thenReturn(user);
    service.deleteNihAccountById(1);
    verify(userPropertyDAO, times(1)).deletePropertiesByUserAndKey(any());
  }

  @Test
  void testDeleteNihAccountByIdNotFound() {
    assertThrows(NotFoundException.class, () -> service.deleteNihAccountById(1));
  }
}