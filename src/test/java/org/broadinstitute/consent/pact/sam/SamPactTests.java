package org.broadinstitute.consent.pact.sam;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit.MockServerConfig;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import com.google.api.client.http.HttpStatusCodes;
import com.google.gson.Gson;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.db.SamDAO;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.sam.EmailResponse;
import org.broadinstitute.consent.http.models.sam.ResourceType;
import org.broadinstitute.consent.http.models.sam.TosResponse;
import org.broadinstitute.consent.http.models.sam.UserStatus;
import org.broadinstitute.consent.http.models.sam.UserStatus.Enabled;
import org.broadinstitute.consent.http.models.sam.UserStatus.UserInfo;
import org.broadinstitute.consent.http.models.sam.UserStatusDiagnostics;
import org.broadinstitute.consent.http.models.sam.UserStatusInfo;
import org.broadinstitute.consent.http.util.HttpClientUtil;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Pact Consumer Contract and test for interactions between Consent and Sam
 */
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = SamPactTests.PROVIDER_NAME, pactVersion = PactSpecVersion.V3)
@MockServerConfig(hostInterface = "localhost")
class SamPactTests {

  protected static final String PROVIDER_NAME = "sam";
  protected static final String CONSUMER_NAME = "consent";

  private static final List<ResourceType> RESOURCE_TYPES = List.of(
      new ResourceType()
          .setName("resource-name")
  );
  private static final UserStatusInfo USER_STATUS_INFO =
      new UserStatusInfo()
          .setAdminEnabled(true)
          .setEnabled(true)
          .setUserEmail("test.user@gmail.com")
          .setUserSubjectId("1234567890");

  private static final UserStatusDiagnostics USER_STATUS_DIAGNOSTICS =
      new UserStatusDiagnostics()
          .setAdminEnabled(true)
          .setEnabled(true)
          .setInAllUsersGroup(true)
          .setInGoogleProxyGroup(true)
          .setTosAccepted(true);

  private static final UserStatus USER_STATUS =
      new UserStatus()
          .setUserInfo(new UserInfo()
              .setUserEmail(USER_STATUS_INFO.getUserEmail())
              .setUserSubjectId(USER_STATUS_INFO.getUserSubjectId()))
          .setEnabled(new Enabled()
              .setAllUsersGroup(USER_STATUS_DIAGNOSTICS.getInAllUsersGroup())
              .setLdap(true)
              .setGoogle(USER_STATUS_DIAGNOSTICS.getInGoogleProxyGroup()));

  private static final TosResponse TOS_RESPONSE =
      new TosResponse("accepted", true, "version", true);

  private static final EmailResponse EMAIL_RESPONSE =
      new EmailResponse("googleId", "email", "subjectId");

  private final Map<String, String> JSON_HEADERS = Map.of(
      HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON,
      "Authentication", "Bearer: auth-token");

  private final Map<String, String> TEXT_PLAIN_HEADERS = Map.of(
      HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN,
      "Authentication", "Bearer: auth-token");

  private SamDAO samDAO;

  @BeforeAll
  public static void beforeClass() {
    System.setProperty("pact_do_not_track", "true");
    System.setProperty("pact.writer.overwrite", "true");
  }

  private void initSamDAO(MockServer mockServer) {
    ServicesConfiguration config = new ServicesConfiguration();
    config.setSamUrl(mockServer.getUrl() + "/");
    samDAO = new SamDAO(new HttpClientUtil(config), config);
  }

  /* Pacts
   * RequestResponsePact methods define an expectation that Consent has of Sam. This expectation
   * will be published to a Pact Broker so that if our expectation differs from what is published,
   * we'll be notified via GitHub Action runs.
   */

  @Pact(consumer = CONSUMER_NAME)
  public RequestResponsePact getResourceTypes(PactDslWithProvider builder) {
    return builder
        .given(" GET Sam Resource Types")
        .uponReceiving(" GET Request: " + ServicesConfiguration.RESOURCE_TYPES_PATH)
        .path("/" + ServicesConfiguration.RESOURCE_TYPES_PATH)
        .method("GET")
        .willRespondWith()
        .status(HttpStatusCodes.STATUS_CODE_OK)
        .headers(JSON_HEADERS)
        .body(RESOURCE_TYPES.toString())
        .toPact();
  }

  @Pact(consumer = CONSUMER_NAME)
  public RequestResponsePact getSelfInfo(PactDslWithProvider builder) {
    return builder
        // Self Info:
        .given(" GET Sam Self Info")
        .uponReceiving(" GET Request: " + ServicesConfiguration.REGISTER_SELF_INFO_PATH)
        .path("/" + ServicesConfiguration.REGISTER_SELF_INFO_PATH)
        .method("GET")
        .willRespondWith()
        .status(HttpStatusCodes.STATUS_CODE_OK)
        .headers(JSON_HEADERS)
        .body(USER_STATUS_INFO.toString())
        .toPact();
  }

  @Pact(consumer = CONSUMER_NAME)
  public RequestResponsePact getSelfDiagnostics(PactDslWithProvider builder) {
    return builder
        .given(" GET Sam Self Diagnostics")
        .uponReceiving(" GET Request: " + ServicesConfiguration.REGISTER_SELF_DIAGNOSTICS_PATH)
        .path("/" + ServicesConfiguration.REGISTER_SELF_DIAGNOSTICS_PATH)
        .method("GET")
        .willRespondWith()
        .status(HttpStatusCodes.STATUS_CODE_OK)
        .headers(JSON_HEADERS)
        .body(USER_STATUS_DIAGNOSTICS.toString())
        .toPact();
  }

  @Pact(consumer = CONSUMER_NAME)
  public RequestResponsePact postUserRegistration(PactDslWithProvider builder) {
    return builder
        .given(" POST Sam User Registration V2")
        .uponReceiving(" POST Request: " + ServicesConfiguration.REGISTER_SELF_PATH)
        .path("/" + ServicesConfiguration.REGISTER_SELF_PATH)
        .method("POST")
        .willRespondWith()
        .status(HttpStatusCodes.STATUS_CODE_CREATED)
        .headers(JSON_HEADERS)
        .body(USER_STATUS.toString())
        .toPact();
  }

  @Pact(consumer = CONSUMER_NAME)
  public RequestResponsePact getTermsOfServiceText(PactDslWithProvider builder) {
    return builder
        .given(" GET Sam Terms of Service Text")
        .uponReceiving(" GET Request: " + ServicesConfiguration.TOS_TEXT_PATH)
        .path("/" + ServicesConfiguration.TOS_TEXT_PATH)
        .method("GET")
        .willRespondWith()
        .status(HttpStatusCodes.STATUS_CODE_OK)
        .headers(TEXT_PLAIN_HEADERS)
        .toPact();
  }

  @Pact(consumer = CONSUMER_NAME)
  public RequestResponsePact getTermsOfService(PactDslWithProvider builder) {
    return builder
        .given(" GET Sam Terms of Service")
        .uponReceiving(" GET Request: " + ServicesConfiguration.TOS_SELF_PATH)
        .path("/" + ServicesConfiguration.TOS_SELF_PATH)
        .method("GET")
        .willRespondWith()
        .status(HttpStatusCodes.STATUS_CODE_OK)
        .headers(JSON_HEADERS)
        .body(TOS_RESPONSE.toString())
        .toPact();
  }

  @Pact(consumer = CONSUMER_NAME)
  public RequestResponsePact acceptTermsOfService(PactDslWithProvider builder) {
    return builder
        .given(" PUT Accept Sam Terms of Service")
        .uponReceiving(" PUT Request: " + ServicesConfiguration.ACCEPT_TOS_PATH)
        .path("/" + ServicesConfiguration.ACCEPT_TOS_PATH)
        .method("PUT")
        .willRespondWith()
        .status(HttpStatusCodes.STATUS_CODE_NO_CONTENT)
        .headers(JSON_HEADERS)
        .toPact();
  }

  @Pact(consumer = CONSUMER_NAME)
  public RequestResponsePact rejectTermsOfService(PactDslWithProvider builder) {
    return builder
        .given(" PUT Reject Sam Terms of Service")
        .uponReceiving(" PUT Request: " + ServicesConfiguration.REJECT_TOS_PATH)
        .path("/" + ServicesConfiguration.REJECT_TOS_PATH)
        .method("PUT")
        .willRespondWith()
        .status(HttpStatusCodes.STATUS_CODE_NO_CONTENT)
        .headers(JSON_HEADERS)
        .toPact();
  }

  @Pact(consumer = CONSUMER_NAME)
  public RequestResponsePact getV1UserByEmail(PactDslWithProvider builder) {
    Gson gson = GsonUtil.buildGson();
    return builder
        .given(" GET Sam User By Email")
        .uponReceiving(" GET Request: " + ServicesConfiguration.SAM_V1_USER_EMAIL)
        .path("/" + ServicesConfiguration.SAM_V1_USER_EMAIL + "/test")
        .method("GET")
        .willRespondWith()
        .status(HttpStatusCodes.STATUS_CODE_OK)
        .headers(JSON_HEADERS)
        .body(gson.toJson(EMAIL_RESPONSE))
        .toPact();
  }

  //******* Tests *******//

  @Test
  @PactTestFor(pactMethod = "acceptTermsOfService")
  void testAcceptTermsOfService(MockServer mockServer) throws Exception {
    initSamDAO(mockServer);
    AuthUser authUser = new AuthUser();
    authUser.setAuthToken("auth-token");

    int tosResponse = samDAO.acceptTosStatus(authUser);
    assertEquals(HttpStatusCodes.STATUS_CODE_NO_CONTENT, tosResponse);
  }

  @Test
  @PactTestFor(pactMethod = "getSelfInfo")
  void testGetSelfInfo(MockServer mockServer) throws Exception {
    initSamDAO(mockServer);
    AuthUser authUser = new AuthUser();
    authUser.setAuthToken("auth-token");

    UserStatusInfo info = samDAO.getRegistrationInfo(authUser);
    assertNotNull(info);
  }

  @Test
  @PactTestFor(pactMethod = "getSelfDiagnostics")
  void testGetSelfDiagnostics(MockServer mockServer) throws Exception {
    initSamDAO(mockServer);
    AuthUser authUser = new AuthUser();
    authUser.setAuthToken("auth-token");

    UserStatusDiagnostics statusDiagnostics = samDAO.getSelfDiagnostics(authUser);
    assertNotNull(statusDiagnostics);
  }

  @Test
  @PactTestFor(pactMethod = "postUserRegistration")
  void testPostUserRegistration(MockServer mockServer) throws Exception {
    initSamDAO(mockServer);
    AuthUser authUser = new AuthUser();
    authUser.setAuthToken("auth-token");

    UserStatus userStatus = samDAO.postRegistrationInfo(authUser);
    assertNotNull(userStatus);
  }

  @Test
  @PactTestFor(pactMethod = "getTermsOfServiceText")
  void testGetTermsOfServiceText(MockServer mockServer) throws Exception {
    initSamDAO(mockServer);
    AuthUser authUser = new AuthUser();
    authUser.setAuthToken("auth-token");

    String tosText = samDAO.getToSText();
    assertNotNull(tosText);
  }

  @Test
  @PactTestFor(pactMethod = "getTermsOfService")
  void testGetTermsOfService(MockServer mockServer) throws Exception {
    initSamDAO(mockServer);
    AuthUser authUser = new AuthUser();
    authUser.setAuthToken("auth-token");

    TosResponse tosResponse = samDAO.getTosResponse(authUser);
    assertNotNull(tosResponse);
  }

  @Test
  @PactTestFor(pactMethod = "getResourceTypes")
  void testGetResourceTypes(MockServer mockServer) throws Exception {
    initSamDAO(mockServer);
    AuthUser authUser = new AuthUser();
    authUser.setAuthToken("auth-token");

    List<ResourceType> types = samDAO.getResourceTypes(authUser);
    assertNotNull(types);
    assertFalse(types.isEmpty());
  }

  @Test
  @PactTestFor(pactMethod = "getV1UserByEmail")
  void testGetV1UserByEmail(MockServer mockServer) throws Exception {
    initSamDAO(mockServer);
    AuthUser authUser = new AuthUser();
    authUser.setAuthToken("auth-token");

    EmailResponse response = samDAO.getV1UserByEmail(authUser, "test");
    assertNotNull(response);
  }

  @Test
  @PactTestFor(pactMethod = "rejectTermsOfService")
  void testRejectTermsOfService(MockServer mockServer) throws Exception {
    initSamDAO(mockServer);
    AuthUser authUser = new AuthUser();
    authUser.setAuthToken("auth-token");

    int tosResponse = samDAO.rejectTosStatus(authUser);
    assertEquals(HttpStatusCodes.STATUS_CODE_NO_CONTENT, tosResponse);
  }

}

