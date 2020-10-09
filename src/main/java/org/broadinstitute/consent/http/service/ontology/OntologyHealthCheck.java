package org.broadinstitute.consent.http.service.ontology;

import com.codahale.metrics.health.HealthCheck;
import com.google.api.client.http.HttpStatusCodes;
import io.dropwizard.lifecycle.Managed;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;

public class OntologyHealthCheck extends HealthCheck implements Managed {

  private final Client client;
  private final ServicesConfiguration servicesConfiguration;

  public OntologyHealthCheck(Client client, ServicesConfiguration servicesConfiguration) {
    this.client = client;
    this.servicesConfiguration = servicesConfiguration;
  }

  @Override
  public Result check() {
    String statusUrl = servicesConfiguration.getOntologyURL() + "status";
    WebTarget target = client.target(statusUrl);
    Response response = target.request(MediaType.APPLICATION_JSON).get();
    try {
      if (response.getStatus() == HttpStatusCodes.STATUS_CODE_OK) {
        return Result.healthy();
      } else {
        return Result.unhealthy("Ontology status");
      }
    } catch (Exception e) {
      return Result.unhealthy(e.getMessage());
    }
  }

  @Override
  public void start() {}

  @Override
  public void stop() {}
}
