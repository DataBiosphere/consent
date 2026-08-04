package org.broadinstitute.consent.http.health;

import static org.broadinstitute.consent.http.service.ontology.ElasticSearchSupport.jsonHeader;

import com.codahale.metrics.health.HealthCheck;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import java.io.IOException;
import java.nio.charset.Charset;
import org.apache.commons.io.IOUtils;
import org.broadinstitute.consent.http.service.ontology.ElasticSearchSupport;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;

public class ElasticSearchHealthCheck extends HealthCheck {

  private final RestClient client;

  /**
   * Takes the application's shared {@link RestClient} rather than building one: a health check that
   * opened its own connection pool would double the pools held against the same cluster. The client
   * is closed by the module that provides it, so nothing is closed here.
   */
  @Inject
  public ElasticSearchHealthCheck(RestClient client) {
    this.client = client;
  }

  @Override
  protected Result check() throws Exception {
    try {
      RequestOptions.Builder builder = RequestOptions.DEFAULT.toBuilder();
      builder.addHeader(jsonHeader.getName(), jsonHeader.getValue());
      Request request = new Request("GET", ElasticSearchSupport.getClusterHealthPath());
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
      return Result.unhealthy("Unable to connect to ElasticSearch");
    }
    return Result.healthy("ClusterHealth is GREEN");
  }
}
