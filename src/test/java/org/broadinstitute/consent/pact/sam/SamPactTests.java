package org.broadinstitute.consent.pact.sam;

import static org.junit.Assert.assertEquals;

import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit.PactProviderRule;
import au.com.dius.pact.consumer.junit.PactVerification;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;

/**
 * Modeled on https://github.com/pact-foundation/pact-jvm/blob/master/consumer/junit/src/test/java/au/com/dius/pact/consumer/junit/examples/ExampleJavaConsumerPactRuleTest.java
 *
 * Pact Consumer Contract and test for interactions with Sam
 *
 * TODO:
 * Figure out mock server issue
 * GitHub action for pushing to pact broker
 * Expand test suite
 */
public class SamPactTests {

  private static final String SELF_INFO_URL = "/register/user/v2/self/info";
  private static final String providerName = "SamProvider";
  private static final String consumerName = "SamConsumer";

  @Rule
  public PactProviderRule mockProvider = new PactProviderRule(providerName, this);

  /**
   * This defines an expectation that Consent has of Sam. This expectation will be published to a
   * Pact Broker so that if our expectation differs from what is published, we'll be notified via
   * GitHub Action runs.
   *
   * @param builder PactDslWithProvider
   * @return RequestResponsePactmockProvider
   */
  @Pact(provider=providerName, consumer=consumerName)
  public RequestResponsePact createPact(PactDslWithProvider builder) {
    Map<String, String> headers = Map.of("Content-Type", "application/json");
    return builder
        .given("test GET")
        .uponReceiving("GET REQUEST")
        .path(SELF_INFO_URL)
        .method("GET")
        .willRespondWith()
        .status(200)
        .headers(headers)
        .body("""
            {
               "adminEnabled": true,
               "enabled": true,
               "userEmail": "test.user@gmail.com",
               "userSubjectId": "1234567890"
             }""")
        .toPact();
  }

  @Test
  @PactVerification(providerName)
  public void runTest() throws Exception {
        assertEquals(200, new ConsumerClient(mockProvider.getUrl()).options(SELF_INFO_URL));
  }
}
