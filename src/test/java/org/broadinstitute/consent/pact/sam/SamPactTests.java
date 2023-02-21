package org.broadinstitute.consent.pact.sam;

import static org.junit.Assert.assertNotNull;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit.PactProviderRule;
import au.com.dius.pact.consumer.junit.PactVerification;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import java.util.Map;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.db.SamDAO;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.sam.UserStatusDiagnostics;
import org.broadinstitute.consent.http.models.sam.UserStatusInfo;
import org.broadinstitute.consent.pact.PactTests;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Modeled on <a href="https://github.com/pact-foundation/pact-jvm/blob/master/consumer/junit/src/test/java/au/com/dius/pact/consumer/junit/examples/ExampleJavaConsumerPactRuleTest.java">ExampleJavaConsumerPactRuleTest.java</a>
 *
 * Pact Consumer Contract and test for interactions with Sam
 *
 * TODO:
 *    Expand test suite
 *    Can we split tests up more granularly???
 */
@Category(PactTests.class)
public class SamPactTests {

  private static final String SELF_INFO_URL = "/register/user/v2/self/info";
  private static final String SELF_DIAGNOSTICS_URL = "/register/user/v2/self/diagnostics";
  private static final String PROVIDER_NAME = "sam-provider";
  private static final String CONSUMER_NAME = "consent-consumer";
  private static final String SAMPLE_INFO_RESPONSE = """
      {
         "adminEnabled": true,
         "enabled": true,
         "userEmail": "test.user@gmail.com",
         "userSubjectId": "1234567890"
       }""";

  private static final String SAMPLE_INFO_DIAGNOSTICS = """
      {
          "adminEnabled": true,
          "enabled": true,
          "inAllUsersGroup": true,
          "inGoogleProxyGroup": true,
          "tosAccepted": true
      }""";

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
  @Pact(provider=PROVIDER_NAME, consumer=CONSUMER_NAME)
  public RequestResponsePact createPact(PactDslWithProvider builder) {
    Map<String, String> headers = Map.of("Content-Type", "application/json");
    return builder
        // Self Info:
        .given(" GET Sam Self Info")
        .uponReceiving(" GET Request: " + SELF_INFO_URL)
          .path(SELF_INFO_URL)
          .method("GET")
        .willRespondWith()
          .status(200)
          .headers(headers)
          .body(SAMPLE_INFO_RESPONSE)
        // Self Diagnostics:
        .given(" GET Sam Self Diagnostics")
        .uponReceiving(" GET Request: " + SELF_DIAGNOSTICS_URL)
          .path(SELF_DIAGNOSTICS_URL)
          .method("GET")
        .willRespondWith()
          .status(200)
          .headers(headers)
          .body(SAMPLE_INFO_DIAGNOSTICS)
        .toPact();
  }

  @Test
  @PactVerification(PROVIDER_NAME)
  public void testSamContracts() throws Exception {
    initSamDAO(mockProvider.getMockServer());
    AuthUser authUser = new AuthUser();

    UserStatusInfo info = samDAO.getRegistrationInfo(authUser);
    assertNotNull(info);

    UserStatusDiagnostics statusDiagnostics = samDAO.getSelfDiagnostics(authUser);
    assertNotNull(statusDiagnostics);
  }
}
