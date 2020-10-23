package org.broadinstitute.consent.http.util;

import java.io.IOException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.HttpClients;

public class HttpClientUtil {

  public CloseableHttpResponse getHttpResponse(HttpUriRequest request) throws IOException {
    return HttpClients.createMinimal().execute(request);
  }

}
