package org.broadinstitute.consent.pact.sam;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Pact Consumer Contract and test for interactions with Sam
 *
 * TODO:
 * Figure out mock server issue
 * GitHub action for pushing to pact broker
 * Expand test suite
 */
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName="SamProvider")
public class SamPactTests {

  private static final String SELF_INFO_URL = "/register/user/v2/self/info";

  @BeforeEach
  public void setUp(MockServer mockServer) {
    Assertions.assertNotNull(mockServer);
  }

  /**
   * This defines an expectation that Consent has of Sam. This expectation will
   * be published to a Pact Broker so that if our expectation differs from what
   * is published, we'll be notified via GitHub Action runs.
   *
   * @param builder PactDslWithProvider
   * @return RequestResponsePact
   */
  @Pact(provider="SamProvider", consumer = "test_consumer")
  public RequestResponsePact selfInfoPact(PactDslWithProvider builder) {
    Map<String, String> headers = new HashMap<>();
    headers.put("Content-Type", "application/json");
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
                 "userEmail": "gregory.rushton@gmail.com",
                 "userSubjectId": "104394682457782759726"
               }""")
          .toPact();
  }

  @Test
  @PactTestFor(pactMethod = "selfInfoPact")
  void test(MockServer mockServer) throws IOException {
    ClassicHttpResponse httpResponse = (ClassicHttpResponse)  Request.get(mockServer.getUrl() + SELF_INFO_URL).execute().returnResponse();
    Assertions.assertEquals(200, httpResponse.getCode());
  }
}
