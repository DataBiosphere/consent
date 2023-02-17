package org.broadinstitute.consent.pact.sam;

import static org.junit.Assert.assertFalse;

import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit.PactProviderRule;
import au.com.dius.pact.consumer.junit.PactVerification;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import java.util.Map;
import org.broadinstitute.consent.pact.ConsumerClient;
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
 * Expand test suite
 * Clean up consumer client
 */
@Category(PactTests.class)
public class SamPactTests {

  private static final String SELF_INFO_URL = "/register/user/v2/self/info";
  private static final String PROVIDER_NAME = "sam-provider";
  private static final String CONSUMER_NAME = "consent-consumer";
  private static final String SAMPLE_RESPONSE = """
              {
                 "adminEnabled": true,
                 "enabled": true,
                 "userEmail": "test.user@gmail.com",
                 "userSubjectId": "1234567890"
               }""";

  @Rule
  public PactProviderRule mockProvider = new PactProviderRule(PROVIDER_NAME, this);

  @BeforeClass
  public static void beforeClass() {
    System.setProperty("pact_do_not_track", "true");
  }

  /**
   * This defines an expectation that Consent has of Sam. This expectation will be published to a
   * Pact Broker so that if our expectation differs from what is published, we'll be notified via
   * GitHub Action runs.
   *
   * @param builder PactDslWithProvider
   * @return RequestResponsePact
   */
  @Pact(provider= PROVIDER_NAME, consumer= CONSUMER_NAME)
  public RequestResponsePact createPact(PactDslWithProvider builder) {
    Map<String, String> headers = Map.of("Content-Type", "application/json");
    return builder
        .given("test GET")
        .uponReceiving("GET Request")
          .path(SELF_INFO_URL)
          .method("GET")
        .willRespondWith()
          .status(200)
          .headers(headers)
          .body(SAMPLE_RESPONSE)
        .toPact();
  }

  @Test
  @PactVerification(PROVIDER_NAME)
  public void testGetSelfInfo() throws Exception {
    ConsumerClient client = new ConsumerClient(mockProvider.getUrl());
    String response = client.get(SELF_INFO_URL);
    assertFalse(response.isBlank());
  }
}
