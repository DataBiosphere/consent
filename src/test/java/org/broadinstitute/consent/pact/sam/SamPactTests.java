package org.broadinstitute.consent.pact.sam;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit.MockServerConfig;
import au.com.dius.pact.consumer.junit5.PactConsumerTest;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import com.google.api.client.http.HttpStatusCodes;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.db.SamDAO;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.sam.ResourceType;
import org.broadinstitute.consent.http.models.sam.TosResponse;
import org.broadinstitute.consent.http.models.sam.UserStatus;
import org.broadinstitute.consent.http.models.sam.UserStatus.Enabled;
import org.broadinstitute.consent.http.models.sam.UserStatus.UserInfo;
import org.broadinstitute.consent.http.models.sam.UserStatusDiagnostics;
import org.broadinstitute.consent.http.models.sam.UserStatusInfo;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


/**
 * Pact Consumer Contract and test for interactions between Consent and Sam
 */
@PactConsumerTest
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = SamPactTests.PROVIDER_NAME, pactVersion = PactSpecVersion.V3)
@MockServerConfig(hostInterface = "localhost", port = "1234")
public class SamPactTests {

  protected static final String PROVIDER_NAME = "sam-provider";
  protected static final String CONSUMER_NAME = "consent-consumer";

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

  private final Map<String, String> JSON_HEADERS = Map.of(
      HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON,
      HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON,
      "Authentication", "Bearer: auth-token",
      "X-App-ID", "DUOS");

  private final Map<String, String> TEXT_PLAIN_HEADERS = Map.of(
      HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN,
      "Authentication", "Bearer: auth-token");

  private SamDAO samDAO;

  @BeforeAll
  public static void beforeClass() {
    System.setProperty("pact_do_not_track", "true");
    System.setProperty("pact.writer.overwrite", "true");
  }

  @BeforeEach
  public void setUp(MockServer mockServer) {
    assertThat(mockServer, is(notNullValue()));
  }

  private void initSamDAO(MockServer mockServer) {
    ServicesConfiguration config = new ServicesConfiguration();
    config.setSamUrl("http://localhost:" + mockServer.getPort() + "/");
    samDAO = new SamDAO(config);
  }

  // Pacts
  // RequestResponsePact methods define an expectation that Consent has of Sam. This expectation
  // will be published to a Pact Broker so that if our expectation differs from what is published,
  // we'll be notified via GitHub Action runs.

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
  public RequestResponsePact getTermsOfService(PactDslWithProvider builder) {
    return builder
        .given(" GET Sam Terms of Service")
        .uponReceiving(" GET Request: " + ServicesConfiguration.TOS_TEXT_PATH)
        .path("/" + ServicesConfiguration.TOS_TEXT_PATH)
        .method("GET")
        .willRespondWith()
        .status(HttpStatusCodes.STATUS_CODE_OK)
        .headers(TEXT_PLAIN_HEADERS)
        .toPact();
  }

  @Pact(consumer = CONSUMER_NAME)
  public RequestResponsePact postTermsOfService(PactDslWithProvider builder) {
    return builder
        .given(" POST Sam Terms of Service")
        .uponReceiving(" POST Request: " + ServicesConfiguration.REGISTER_TOS_PATH)
        .path("/" + ServicesConfiguration.REGISTER_TOS_PATH)
        .method("POST")
        .willRespondWith()
        .status(HttpStatusCodes.STATUS_CODE_OK)
        .headers(JSON_HEADERS)
        .body(USER_STATUS.toString())
        .toPact();
  }

  @Disabled
  @Pact(consumer = CONSUMER_NAME)
  public RequestResponsePact deleteTermsOfService(PactDslWithProvider builder) {
    return builder
        .given(" DELETE Sam Terms of Service")
        .uponReceiving(" DELETE Request: " + ServicesConfiguration.REGISTER_TOS_PATH)
        .path("/" + ServicesConfiguration.REGISTER_TOS_PATH)
        .method("DELETE")
        .willRespondWith()
        .status(HttpStatusCodes.STATUS_CODE_OK)
        .headers(JSON_HEADERS)
        .body(USER_STATUS.toString())
        .toPact();
  }

  //******* Tests *******//

  @Test
  @PactTestFor(pactMethod = "getResourceTypes")
  public void testGetResourceTypes(MockServer mockServer) throws Exception {
    initSamDAO(mockServer);
    AuthUser authUser = new AuthUser();
    authUser.setAuthToken("auth-token");

    List<ResourceType> types = samDAO.getResourceTypes(authUser);
    assertNotNull(types);
    assertFalse(types.isEmpty());
  }

  @Test
  @PactTestFor(pactMethod = "getSelfInfo")
  public void testGetSelfInfo(MockServer mockServer) throws Exception {
    initSamDAO(mockServer);
    AuthUser authUser = new AuthUser();
    authUser.setAuthToken("auth-token");

    UserStatusInfo info = samDAO.getRegistrationInfo(authUser);
    assertNotNull(info);
  }

  @Test
  @PactTestFor(pactMethod = "getSelfDiagnostics")
  public void testGetSelfDiagnostics(MockServer mockServer) throws Exception {
    initSamDAO(mockServer);
    AuthUser authUser = new AuthUser();
    authUser.setAuthToken("auth-token");

    UserStatusDiagnostics statusDiagnostics = samDAO.getSelfDiagnostics(authUser);
    assertNotNull(statusDiagnostics);
  }

  @Test
  @PactTestFor(pactMethod = "postUserRegistration")
  public void testPostUserRegistration(MockServer mockServer) throws Exception {
    initSamDAO(mockServer);
    AuthUser authUser = new AuthUser();
    authUser.setAuthToken("auth-token");

    UserStatus userStatus = samDAO.postRegistrationInfo(authUser);
    assertNotNull(userStatus);
  }

  @Test
  @PactTestFor(pactMethod = "getTermsOfService")
  public void testGetTermsOfService(MockServer mockServer) throws Exception {
    initSamDAO(mockServer);
    AuthUser authUser = new AuthUser();
    authUser.setAuthToken("auth-token");

    String tosText = samDAO.getToSText();
    assertNotNull(tosText);
  }

  @Test
  @PactTestFor(pactMethod = "postTermsOfService")
  public void testPostTermsOfService(MockServer mockServer) throws Exception {
    initSamDAO(mockServer);
    AuthUser authUser = new AuthUser();
    authUser.setAuthToken("auth-token");

    TosResponse tosPostResponse = samDAO.postTosAcceptedStatus(authUser);
    assertNotNull(tosPostResponse);
  }

  // TODO: This test succeeds when run individually separate from the rest of the tests  in this
  // class. Once a second test is enabled alongside of it, it will consistently fail with
  // Connection Reset errors.
  @Test
  @Disabled
  @PactTestFor(pactMethod = "deleteTermsOfService")
  public void testDeleteTermsOfService(MockServer mockServer) throws Exception {
    initSamDAO(mockServer);
    AuthUser authUser = new AuthUser();
    authUser.setAuthToken("auth-token");

    TosResponse TosDeleteResponse = samDAO.removeTosAcceptedStatus(authUser);
    assertNotNull(TosDeleteResponse);
  }

}
