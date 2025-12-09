package org.broadinstitute.consent.http.health;

import static org.broadinstitute.consent.http.service.ontology.OpenSearchSupport.jsonHeader;

import com.codahale.metrics.health.HealthCheck;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import io.dropwizard.lifecycle.Managed;
import java.io.IOException;
import java.nio.charset.Charset;
import org.apache.commons.io.IOUtils;
import org.broadinstitute.consent.http.configurations.OpenSearchConfiguration;
import org.broadinstitute.consent.http.service.ontology.OpenSearchSupport;
import org.opensearch.client.Request;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.Response;
import org.opensearch.client.RestClient;

public class OpenSearchHealthCheck extends HealthCheck implements Managed {

  private final RestClient client;

  @Override
  public void start() throws Exception {}

  @Override
  public void stop() throws Exception {
    if (client != null) {
      client.close();
    }
  }

  @Inject
  public OpenSearchHealthCheck(OpenSearchConfiguration config) {
    this.client = OpenSearchSupport.createRestClient(config);
  }

  @Override
  protected Result check() throws Exception {
    try {
      RequestOptions.Builder builder = RequestOptions.DEFAULT.toBuilder();
      builder.addHeader(jsonHeader.getName(), jsonHeader.getValue());
      Request request = new Request("GET", OpenSearchSupport.getClusterHealthPath());
      request.setOptions(builder.build());
      Response esResponse = client.performRequest(request);
      if (esResponse.getStatusLine().getStatusCode() != 200) {
        return Result.unhealthy(
            "Invalid health check request: " + esResponse.getStatusLine().getReasonPhrase());
      }
      String stringResponse =
          IOUtils.toString(esResponse.getEntity().getContent(), Charset.defaultCharset());
      JsonObject jsonResponse = JsonParser.parseString(stringResponse).getAsJsonObject();
      String status = jsonResponse.get("status").getAsString();
      if (status.equalsIgnoreCase("red")) {
        return Result.unhealthy("ClusterHealth is RED\n" + jsonResponse);
      }
      if (status.equalsIgnoreCase("yellow")) {
        return Result.healthy("ClusterHealth is YELLOW\n" + jsonResponse);
      }
    } catch (IOException e) {
      return Result.unhealthy("Unable to connect to OpenSearch");
    }
    return Result.healthy("ClusterHealth is GREEN");
  }
}
