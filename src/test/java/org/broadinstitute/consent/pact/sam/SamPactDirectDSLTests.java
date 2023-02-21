package org.broadinstitute.consent.pact.sam;

import static au.com.dius.pact.consumer.ConsumerPactRunnerKt.runConsumerTest;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import au.com.dius.pact.consumer.ConsumerPactBuilder;
import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.PactVerificationResult;
import au.com.dius.pact.consumer.model.MockProviderConfig;
import au.com.dius.pact.core.model.RequestResponsePact;
import java.io.IOException;
import java.util.Map;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.db.SamDAO;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.sam.UserStatusDiagnostics;
import org.broadinstitute.consent.http.models.sam.UserStatusInfo;
import org.broadinstitute.consent.pact.PactTests;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Modeled on https://github.com/pact-foundation/pact-jvm/blob/ac6a0eae0b18183f6f453eafddb89b90741ace42/consumer/junit/src/test/java/au/com/dius/pact/consumer/junit/examples/DirectDSLConsumerPactTest.java
 *
 * TODO:
 *    This doesn't generate pact files like its companion test class does.
 */
@Category(PactTests.class)
public class SamPactDirectDSLTests {


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

  private RequestResponsePact pact;

  private SamDAO samDAO;

  @BeforeClass
  public static void beforeClass() {
    System.setProperty("pact_do_not_track", "true");
    System.setProperty("pact.writer.overwrite", "true");
  }

  @Before
  public void setUp() {
    Map<String, String> headers = Map.of("Content-Type", "application/json");
    pact = ConsumerPactBuilder
        .consumer(CONSUMER_NAME)
        .hasPactWith(PROVIDER_NAME)
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

  private void initSamDAO(MockServer mockServer) {
    ServicesConfiguration config = new ServicesConfiguration();
    config.setSamUrl("http://localhost:" + mockServer.getPort() + "/");
    samDAO = new SamDAO(config);
  }

  @Test
  public void testSelfInfo() {
    MockProviderConfig config = MockProviderConfig.createDefault();
    PactVerificationResult result = runConsumerTest(pact, config, (mockServer, context) -> {
      initSamDAO(mockServer);
      AuthUser authUser = new AuthUser();
      try {
        UserStatusInfo info = samDAO.getRegistrationInfo(authUser);
        assertNotNull(info);
        return new PactVerificationResult.Ok();
      } catch (IOException e) {
        fail(e.getMessage());
        throw new RuntimeException(e);
      }
    });
    if (result instanceof PactVerificationResult.Error) {
      fail(result.getDescription());
    }
  }

  @Test
  public void testSelfDiagnostics() {
    MockProviderConfig config = MockProviderConfig.createDefault();
    PactVerificationResult result = runConsumerTest(pact, config, (mockServer, context) -> {
      initSamDAO(mockServer);
      AuthUser authUser = new AuthUser();
      try {
        UserStatusDiagnostics statusDiagnostics = samDAO.getSelfDiagnostics(authUser);
        assertNotNull(statusDiagnostics);
        return new PactVerificationResult.Ok();
      } catch (IOException e) {
        fail(e.getMessage());
        throw new RuntimeException(e);
      }
    });
    if (result instanceof PactVerificationResult.Error) {
      fail(result.getDescription());
    }
  }

}
