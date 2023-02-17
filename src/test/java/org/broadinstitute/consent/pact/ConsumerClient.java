package org.broadinstitute.consent.pact;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.core5.net.URIBuilder;

/**
 * Modeled on <a href="https://github.com/pact-foundation/pact-jvm/blob/e297ef75761f5008aac4b63550d1871f33119aab/consumer/junit/src/test/java/au/com/dius/pact/consumer/junit/exampleclients/ConsumerClient.java">ConsumerClient.java</a>
 */
public class ConsumerClient {

  private final String url;

  public ConsumerClient(String url) {
    this.url = url;
  }

  public String get(String path) throws IOException {
    URIBuilder uriBuilder;
    try {
      uriBuilder = new URIBuilder(url).setPath(path);
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
    return Request
      .get(uriBuilder.toString())
      .execute()
      .returnContent()
      .asString(Charset.defaultCharset());
  }

}
