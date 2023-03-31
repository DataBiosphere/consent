package org.broadinstitute.consent.pact.sam;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit.PactProviderRule;
import au.com.dius.pact.consumer.junit.PactVerification;
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
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

/**
 * Pact Consumer Contract and test for interactions between Consent and Sam
 */
public class SamPactTests {

  private static final String PROVIDER_NAME = "sam-provider";
  private static final String CONSUMER_NAME = "consent-consumer";

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

  private SamDAO samDAO;

  @Rule
  public PactProviderRule mockProvider = new PactProviderRule(PROVIDER_NAME, this);

  @BeforeClass
  public static void beforeClass() {
    System.setProperty("pact_do_not_track", "true");
    System.setProperty("pact.writer.overwrite", "true");
  }

  private void initSamDAO(MockServer mockServer) {
    ServicesConfiguration config = new ServicesConfiguration();
    config.setSamUrl("http://localhost:" + mockServer.getPort() + "/");
    samDAO = new SamDAO(config);
  }

  /**
   * This defines an expectation that Consent has of Sam. This expectation will be published to a
   * Pact Broker so that if our expectation differs from what is published, we'll be notified via
   * GitHub Action runs.
   *
   * @param builder PactDslWithProvider
   * @return RequestResponsePact
   */
  @Pact(provider = PROVIDER_NAME, consumer = CONSUMER_NAME)
  public RequestResponsePact createPact(PactDslWithProvider builder) {
    Map<String, String> jsonHeaders = Map.of(
        HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON,
        "Authentication", "Bearer: auth-token");
    Map<String, String> textPlainHeaders = Map.of(
        HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN,
        "Authentication", "Bearer: auth-token");
    return builder

        // Resource Types:
        .given(" GET Sam Resource Types")
        .uponReceiving(" GET Request: " + ServicesConfiguration.RESOURCE_TYPES_PATH)
        .path("/" + ServicesConfiguration.RESOURCE_TYPES_PATH)
        .method("GET")
        .willRespondWith()
        .status(HttpStatusCodes.STATUS_CODE_OK)
        .headers(jsonHeaders)
        .body(RESOURCE_TYPES.toString())

        // Self Info:
        .given(" GET Sam Self Info")
        .uponReceiving(" GET Request: " + ServicesConfiguration.REGISTER_SELF_INFO_PATH)
        .path("/" + ServicesConfiguration.REGISTER_SELF_INFO_PATH)
        .method("GET")
        .willRespondWith()
        .status(HttpStatusCodes.STATUS_CODE_OK)
        .headers(jsonHeaders)
        .body(USER_STATUS_INFO.toString())

        // Self Diagnostics:
        .given(" GET Sam Self Diagnostics")
        .uponReceiving(" GET Request: " + ServicesConfiguration.REGISTER_SELF_DIAGNOSTICS_PATH)
        .path("/" + ServicesConfiguration.REGISTER_SELF_DIAGNOSTICS_PATH)
        .method("GET")
        .willRespondWith()
        .status(HttpStatusCodes.STATUS_CODE_OK)
        .headers(jsonHeaders)
        .body(USER_STATUS_DIAGNOSTICS.toString())

        // User Registration V2
        .given(" POST Sam User Registration V2")
        .uponReceiving(" POST Request: " + ServicesConfiguration.REGISTER_SELF_PATH)
        .path("/" + ServicesConfiguration.REGISTER_SELF_PATH)
        .method("POST")
        .willRespondWith()
        .status(HttpStatusCodes.STATUS_CODE_CREATED)
        .headers(jsonHeaders)
        .body(USER_STATUS.toString())

        // Terms of Service:
        .given(" GET Sam Terms of Service")
        .uponReceiving(" GET Request: " + ServicesConfiguration.TOS_TEXT_PATH)
        .path("/" + ServicesConfiguration.TOS_TEXT_PATH)
        .method("GET")
        .willRespondWith()
        .status(HttpStatusCodes.STATUS_CODE_OK)
        .headers(textPlainHeaders)
        .body("Terms of Service")

        // Accept Terms of Service:
        .given(" POST Sam Terms of Service")
        .uponReceiving(" POST Request: " + ServicesConfiguration.REGISTER_TOS_PATH)
        .path("/" + ServicesConfiguration.REGISTER_TOS_PATH)
        .method("POST")
        .willRespondWith()
        .status(HttpStatusCodes.STATUS_CODE_OK)
        .headers(jsonHeaders)
        .body(USER_STATUS.toString())

        // Reject Terms of Service:
        .given(" DELETE Sam Terms of Service")
        .uponReceiving(" DELETE Request: " + ServicesConfiguration.REGISTER_TOS_PATH)
        .path("/" + ServicesConfiguration.REGISTER_TOS_PATH)
        .method("DELETE")
        .willRespondWith()
        .status(HttpStatusCodes.STATUS_CODE_OK)
        .headers(jsonHeaders)
        .body(USER_STATUS.toString())
        .toPact();
  }

  @Test
  @PactVerification(PROVIDER_NAME)
  public void testSamContracts() throws Exception {
    initSamDAO(mockProvider.getMockServer());
    AuthUser authUser = new AuthUser();
    authUser.setAuthToken("auth-token");

    List<ResourceType> types = samDAO.getResourceTypes(authUser);
    assertNotNull(types);
    assertFalse(types.isEmpty());

    UserStatusInfo info = samDAO.getRegistrationInfo(authUser);
    assertNotNull(info);

    UserStatusDiagnostics statusDiagnostics = samDAO.getSelfDiagnostics(authUser);
    assertNotNull(statusDiagnostics);

    UserStatus userStatus = samDAO.postRegistrationInfo(authUser);
    assertNotNull(userStatus);

    String tosText = samDAO.getToSText();
    assertNotNull(tosText);

    TosResponse tosPostResponse = samDAO.postTosAcceptedStatus(authUser);
    assertNotNull(tosPostResponse);

    TosResponse TosDeleteResponse = samDAO.removeTosAcceptedStatus(authUser);
    assertNotNull(TosDeleteResponse);
  }
}