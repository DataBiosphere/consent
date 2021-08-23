package org.broadinstitute.consent.http.service.ontology;

import com.codahale.metrics.health.HealthCheck;
import com.google.api.client.http.HttpStatusCodes;
import io.dropwizard.lifecycle.Managed;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.util.HttpClientUtil;

public class OntologyHealthCheck extends HealthCheck implements Managed {

  private final HttpClientUtil clientUtil;
  private final ServicesConfiguration servicesConfiguration;

  public OntologyHealthCheck(HttpClientUtil clientUtil, ServicesConfiguration servicesConfiguration) {
    this.clientUtil = clientUtil;
    this.servicesConfiguration = servicesConfiguration;
  }

  @Override
  public Result check() {
    try {
      String statusUrl = servicesConfiguration.getOntologyURL() + "status";
      HttpGet httpGet = new HttpGet(statusUrl);
      try (CloseableHttpResponse response = clientUtil.getHttpResponse(httpGet)) {
        if (response.getStatusLine().getStatusCode() == HttpStatusCodes.STATUS_CODE_OK) {
          return Result.healthy();
        } else {
          return Result.unhealthy("Ontology status is unhealthy: " + response.getStatusLine());
        }
      } catch (Exception e) {
        return Result.unhealthy(e);
      }
    } catch (Exception e) {
      return Result.unhealthy(e);
    }
  }

  @Override
  public void start() {
    // no-op
  }

  @Override
  public void stop() {
    // no-op
  }
}
