package org.broadinstitute.consent.http.service;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.client.http.HttpStatusCodes;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ServerErrorException;
import java.time.Instant;
import org.broadinstitute.consent.http.WireMockTestHelper;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.enumeration.UserFields;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.models.NIHUserAccount;
import org.broadinstitute.consent.http.models.User;
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
class NihServiceTest extends WireMockTestHelper {

  @Mock private UserDAO userDAO;

  @Mock private NihServiceDAO nihServiceDAO;

  @Mock private DuosUser duosUser;

  private NihService service;

  @BeforeEach
  void setUp() {
    ServicesConfiguration servicesConfig = new ServicesConfiguration();
    servicesConfig.setTimeoutSeconds(1);
    servicesConfig.setEcmUrl(mockServerBaseUrl() + "/");
    service =
        new NihService(userDAO, nihServiceDAO, new HttpClientUtil(servicesConfig), servicesConfig);
    wireMockServer.resetAll();
  }

  @Test
  void testSyncAccount() throws Exception {
    User user = new User();
    user.setUserId(1);
    when(duosUser.getUser()).thenReturn(user);
    when(userDAO.findUserWithPropertiesById(user.getUserId(), UserFields.getValues()))
        .thenReturn(user);
    String timestamp = "2025-08-28T16:54:22.064+00:00";
    LinkInfo ecmResponse = new LinkInfo("test", timestamp, true);
    NIHUserAccount nihAccount =
        new NIHUserAccount(
            ecmResponse.externalUserId(),
            String.valueOf(Instant.parse(timestamp).toEpochMilli()),
            ecmResponse.authenticated());
    wireMockServer.stubFor(
        any(anyUrl())
            .willReturn(
                aResponse()
                    .withStatus(HttpStatusCodes.STATUS_CODE_OK)
                    .withBody(GsonUtil.getInstance().toJson(ecmResponse))));
    User syncedUser = service.syncAccount(duosUser);
    assertEquals(user.getUserId(), syncedUser.getUserId());
    verify(nihServiceDAO).updateUserNihStatus(user, nihAccount);
  }

  @Test
  void testSyncAccountBadRequestError() {
    User user = new User();
    user.setUserId(1);
    when(duosUser.getUser()).thenReturn(user);
    LinkInfo ecmResponse = new LinkInfo("test", "test", true);
    wireMockServer.stubFor(
        any(anyUrl())
            .willReturn(
                aResponse()
                    .withStatus(HttpStatusCodes.STATUS_CODE_BAD_REQUEST)
                    .withBody(GsonUtil.getInstance().toJson(ecmResponse))));
    assertThrows(BadRequestException.class, () -> service.syncAccount(duosUser));
  }

  @Test
  void testSyncAccountServerError() throws Exception {
    // A 500 from ECM is treated specially, which is caught gracefully:
    // the NIH account is deleted locally and the user is returned without throwing.
    User user = new User();
    user.setUserId(1);
    when(duosUser.getUser()).thenReturn(user);
    when(duosUser.getEmail()).thenReturn("test@test.org");
    when(userDAO.findUserWithPropertiesById(user.getUserId(), UserFields.getValues()))
        .thenReturn(user);
    wireMockServer.stubFor(
        any(anyUrl()).willReturn(aResponse().withStatus(HttpStatusCodes.STATUS_CODE_SERVER_ERROR)));

    User syncedUser = service.syncAccount(duosUser);

    assertEquals(user.getUserId(), syncedUser.getUserId());
    verify(nihServiceDAO, times(1)).deleteNihAccountById(user.getUserId());
    verify(nihServiceDAO, never()).updateUserNihStatus(any(), any());
  }

  @Test
  void testSyncAccountInvalidECMResponse() {
    User user = new User();
    user.setUserId(1);
    when(duosUser.getUser()).thenReturn(user);
    wireMockServer.stubFor(
        any(anyUrl())
            .willReturn(
                aResponse()
                    .withStatus(HttpStatusCodes.STATUS_CODE_OK)
                    .withBody(GsonUtil.getInstance().toJson("bad response"))));
    assertThrows(ServerErrorException.class, () -> service.syncAccount(duosUser));
  }

  @Test
  void testSyncAccountECMNotFound() throws Exception {
    User user = new User();
    user.setUserId(1);
    when(duosUser.getUser()).thenReturn(user);
    when(userDAO.findUserWithPropertiesById(user.getUserId(), UserFields.getValues()))
        .thenReturn(user);
    wireMockServer.stubFor(
        any(anyUrl()).willReturn(aResponse().withStatus(HttpStatusCodes.STATUS_CODE_NOT_FOUND)));
    User syncedUser = service.syncAccount(duosUser);
    assertEquals(user.getUserId(), syncedUser.getUserId());
    verify(nihServiceDAO).deleteNihAccountById(user.getUserId());
  }

  @Test
  void testSyncAccountECMNotAuthorized() throws Exception {
    User user = new User();
    user.setUserId(1);
    when(duosUser.getUser()).thenReturn(user);
    when(userDAO.findUserWithPropertiesById(user.getUserId(), UserFields.getValues()))
        .thenReturn(user);
    wireMockServer.stubFor(
        any(anyUrl()).willReturn(aResponse().withStatus(HttpStatusCodes.STATUS_CODE_UNAUTHORIZED)));
    User syncedUser = service.syncAccount(duosUser);
    assertEquals(user.getUserId(), syncedUser.getUserId());
    verify(nihServiceDAO, never()).deleteNihAccountById(user.getUserId());
  }
}
