package org.broadinstitute.consent.http.service.ontology;

import com.codahale.metrics.health.HealthCheck;
import com.google.api.client.http.HttpStatusCodes;
import com.google.common.annotations.VisibleForTesting;
import io.dropwizard.lifecycle.Managed;
import java.util.Objects;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;

public class OntologyHealthCheck extends HealthCheck implements Managed {

  private final ServicesConfiguration servicesConfiguration;
  private CloseableHttpClient httpClient;

  public OntologyHealthCheck(ServicesConfiguration servicesConfiguration) {
    this.servicesConfiguration = servicesConfiguration;
  }

  @VisibleForTesting
  public void setHttpClient(CloseableHttpClient httpClient) {
    this.httpClient = httpClient;
  }

  @Override
  public Result check() {
    try {
      String statusUrl = servicesConfiguration.getOntologyURL() + "status";
      HttpGet httpGet = new HttpGet(statusUrl);
      if (Objects.isNull(httpClient)) {
        setHttpClient(HttpClients.createDefault());
      }
      try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
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
