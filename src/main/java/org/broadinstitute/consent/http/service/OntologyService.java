package org.broadinstitute.consent.http.service;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.ontology.DataUseSummary;
import org.broadinstitute.consent.http.util.ConsentLogger;

public class OntologyService implements ConsentLogger {

  private final ServicesConfiguration servicesConfiguration;
  private final Client client;

  public OntologyService(Client client, ServicesConfiguration config) {
    this.client = client;
    this.servicesConfiguration = config;
  }


  public DataUseSummary translateDataUseSummary(DataUse dataUse) {
    WebTarget target = client.target(
        servicesConfiguration.getOntologyURL() + "translate/summary");
    try (Response response = target.request(MediaType.APPLICATION_JSON)
        .post(Entity.json(dataUse.toString()))) {
      if (response.getStatus() >= 200 || response.getStatus() <= 299) {
        return response.readEntity(DataUseSummary.class);
      }
      logWarn("Error response from Ontology service: " + response.readEntity(String.class));
    } catch (Exception e) {
      logWarn("Error parsing response from Ontology service: " + e);
    }
    return null;
  }
}
