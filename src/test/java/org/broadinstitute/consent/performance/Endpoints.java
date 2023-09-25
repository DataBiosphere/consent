package org.broadinstitute.consent.performance;


import static io.gatling.javaapi.http.HttpDsl.http;

import io.gatling.javaapi.http.HttpRequestActionBuilder;
import jakarta.ws.rs.core.MediaType;

/**
 * This interface defines all possible endpoints in the Consent system. Some endpoints require
 * different accept/content-type headers so define those here. Each endpoint defined should cover a
 * single use case. In the event of APIs having multiple inputs, there should be explicit methods
 * for each variation.
 */
public interface Endpoints {

  default HttpRequestActionBuilder liveness() {
    return
        http("Liveness")
            .get("/liveness")
            .header("Accept", MediaType.TEXT_PLAIN);
  }

  default HttpRequestActionBuilder systemStatus() {
    return
        http("Status")
            .get("/status")
            .header("Accept", MediaType.APPLICATION_JSON);
  }

  default HttpRequestActionBuilder systemVersion() {
    return
        http("Version")
            .get("/version")
            .header("Accept", MediaType.APPLICATION_JSON);
  }

}
